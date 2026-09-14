#!/bin/sh

set -eu

SCRIPT_DIRECTORY=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
APPLE_E2E_DIRECTORY=$(CDPATH= cd -- "$SCRIPT_DIRECTORY/.." && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "$APPLE_E2E_DIRECTORY/.." && pwd)

PROJECT_PATH="$APPLE_E2E_DIRECTORY/DingPushTestHost.xcodeproj"
PAYLOAD_PATH="$APPLE_E2E_DIRECTORY/Payloads/foreground.apns"
RESULT_VERIFIER_PATH="$APPLE_E2E_DIRECTORY/Scripts/verify-result.swift"
LOCAL_SWIFT_PACKAGE="$REPOSITORY_ROOT/ding-core/build/swiftpm/local"
SCHEME="DingPushTestHost"
BUNDLE_IDENTIFIER="io.github.easyhooon.ding.push-test-host"
READY_FILE_NAME="ding-e2e-ready.json"
RESULT_FILE_NAME="ding-e2e-result.json"
STATE_FILE_NAME="ding-e2e-state.json"
WAIT_ATTEMPTS=${DING_E2E_WAIT_ATTEMPTS:-30}
TEMPORARY_ROOT=${TMPDIR:-/tmp}
TEMPORARY_ROOT=${TEMPORARY_ROOT%/}

SIMULATOR_UDID=""
STATE_PATH=""
RESULT_PATH=""
SIMULATOR_NAME="Ding Push E2E $$"
DERIVED_DATA_DIRECTORY=$(mktemp -d "$TEMPORARY_ROOT/ding-push-e2e.XXXXXX")

cleanup() {
    exit_status=$?
    trap - EXIT HUP INT TERM

    if [ -n "$SIMULATOR_UDID" ]; then
        xcrun simctl shutdown "$SIMULATOR_UDID" >/dev/null 2>&1 || true
        xcrun simctl delete "$SIMULATOR_UDID" >/dev/null 2>&1 || true
    fi

    case "$DERIVED_DATA_DIRECTORY" in
        "$TEMPORARY_ROOT"/ding-push-e2e.*)
            rm -rf "$DERIVED_DATA_DIRECTORY"
            ;;
        *)
            printf 'Refusing to remove unexpected temporary path: %s\n' \
                "$DERIVED_DATA_DIRECTORY" >&2
            ;;
    esac

    exit "$exit_status"
}

trap cleanup EXIT HUP INT TERM

fail() {
    printf 'Ding iOS simulator push verification failed: %s\n' "$1" >&2
    if [ -n "$STATE_PATH" ] && [ -s "$STATE_PATH" ]; then
        state=$(/usr/bin/plutil -extract stage raw "$STATE_PATH" 2>/dev/null || true)
        if [ -n "$state" ]; then
            printf 'Last observed host stage: %s\n' "$state" >&2
        fi
    fi
    if [ -n "$RESULT_PATH" ] && [ -e "$RESULT_PATH" ]; then
        result_size=$(wc -c < "$RESULT_PATH" | tr -d ' ')
        printf 'Observed result file size: %s bytes\n' "$result_size" >&2
    fi
    if [ -n "$SIMULATOR_UDID" ] && [ "${DING_E2E_DUMP_LOGS:-0}" = "1" ]; then
        xcrun simctl spawn "$SIMULATOR_UDID" log show \
            --last 2m \
            --style compact \
            --predicate 'process == "DingPushTestHost"' >&2 || true
    fi
    exit 1
}

require_file() {
    if [ ! -f "$1" ]; then
        fail "Missing required file: $1"
    fi
}

wait_for_json_file() {
    file_path=$1
    description=$2
    attempt=0

    while [ "$attempt" -lt "$WAIT_ATTEMPTS" ]; do
        if [ -s "$file_path" ] && is_valid_json "$file_path"; then
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 1
    done

    fail "Timed out waiting for $description at $file_path"
}

wait_for_nonempty_file() {
    file_path=$1
    description=$2
    attempt=0

    while [ "$attempt" -lt "$WAIT_ATTEMPTS" ]; do
        if [ -s "$file_path" ]; then
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 1
    done

    fail "Timed out waiting for $description at $file_path"
}

is_valid_json() {
    /usr/bin/plutil -convert xml1 -o /dev/null "$1" >/dev/null 2>&1
}

json_value() {
    file_path=$1
    key_path=$2
    /usr/bin/plutil -extract "$key_path" raw "$file_path" 2>/dev/null ||
        fail "Missing JSON key '$key_path' in $file_path"
}

assert_json_value() {
    file_path=$1
    key_path=$2
    expected=$3
    actual=$(json_value "$file_path" "$key_path")

    if [ "$actual" != "$expected" ]; then
        fail "Expected '$key_path' to be '$expected', but was '$actual'"
    fi
}

select_simulator_runtime() {
    xcrun simctl list runtimes |
        awk '/^iOS / && $0 !~ /unavailable/ { runtime = $NF } END { print runtime }'
}

create_simulator() {
    if [ -n "${DING_E2E_SIMULATOR_DEVICE_TYPE:-}" ]; then
        xcrun simctl create \
            "$SIMULATOR_NAME" \
            "$DING_E2E_SIMULATOR_DEVICE_TYPE" \
            "$SIMULATOR_RUNTIME"
        return
    fi

    xcrun simctl list devicetypes |
        awk -F '[()]' '/^iPhone / { print $2 }' |
        while IFS= read -r device_type; do
            if simulator_udid=$(xcrun simctl create \
                "$SIMULATOR_NAME" \
                "$device_type" \
                "$SIMULATOR_RUNTIME" 2>/dev/null); then
                printf '%s\n' "$simulator_udid"
                break
            fi
        done
}

require_file "$PROJECT_PATH/project.pbxproj"
require_file "$PAYLOAD_PATH"
require_file "$RESULT_VERIFIER_PATH"

if ! is_valid_json "$PAYLOAD_PATH"; then
    fail "Push payload is not valid JSON: $PAYLOAD_PATH"
fi

if [ "${DING_E2E_SWIFT_PACKAGE_PREPARED:-0}" != "1" ]; then
    "$REPOSITORY_ROOT/gradlew" :ding-core:prepareDingLocalSwiftPackage
fi

require_file "$LOCAL_SWIFT_PACKAGE/Package.swift"
require_file "$LOCAL_SWIFT_PACKAGE/Artifacts/Ding.xcframework/Info.plist"

SIMULATOR_RUNTIME=${DING_E2E_SIMULATOR_RUNTIME:-$(select_simulator_runtime)}

if [ -z "$SIMULATOR_RUNTIME" ]; then
    fail "No available iOS Simulator runtime was found"
fi

SIMULATOR_UDID=$(create_simulator)

if [ -z "$SIMULATOR_UDID" ]; then
    fail "No compatible iPhone Simulator device type was found for $SIMULATOR_RUNTIME"
fi

xcrun simctl boot "$SIMULATOR_UDID"
xcrun simctl bootstatus "$SIMULATOR_UDID" -b

xcodebuild \
    -quiet \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination "platform=iOS Simulator,id=$SIMULATOR_UDID" \
    -derivedDataPath "$DERIVED_DATA_DIRECTORY" \
    CODE_SIGNING_ALLOWED=NO \
    build

APP_PATH="$DERIVED_DATA_DIRECTORY/Build/Products/Debug-iphonesimulator/$SCHEME.app"
require_file "$APP_PATH/Info.plist"

xcrun simctl install "$SIMULATOR_UDID" "$APP_PATH"
DATA_CONTAINER=$(xcrun simctl get_app_container \
    "$SIMULATOR_UDID" \
    "$BUNDLE_IDENTIFIER" \
    data)

if [ -z "$DATA_CONTAINER" ]; then
    fail "Unable to resolve the test host data container"
fi

READY_PATH="$DATA_CONTAINER/Documents/$READY_FILE_NAME"
RESULT_PATH="$DATA_CONTAINER/Documents/$RESULT_FILE_NAME"
STATE_PATH="$DATA_CONTAINER/Documents/$STATE_FILE_NAME"

xcrun simctl launch --terminate-running-process \
    "$SIMULATOR_UDID" \
    "$BUNDLE_IDENTIFIER"

wait_for_json_file "$READY_PATH" "the test host readiness signal"
assert_json_value "$READY_PATH" "ready" "true"
assert_json_value "$READY_PATH" "delegateInstalled" "true"

xcrun simctl push \
    "$SIMULATOR_UDID" \
    "$BUNDLE_IDENTIFIER" \
    "$PAYLOAD_PATH"

wait_for_json_file "$STATE_PATH" "the foreground notification callback"
wait_for_nonempty_file "$RESULT_PATH" "the captured Ding snapshot"

/usr/bin/xcrun swift "$RESULT_VERIFIER_PATH" "$PAYLOAD_PATH" "$RESULT_PATH" ||
    fail "The captured Ding snapshot did not match the APNs fixture"

printf 'Ding captured the simulated foreground push on %s.\n' "$SIMULATOR_UDID"
