#!/bin/sh

set -eu

version="${1:?Usage: verify-remote-release.sh <version>}"
if ! printf '%s\n' "$version" \
    | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?$'; then
    echo "Invalid release version: $version" >&2
    exit 1
fi

repository_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
smoke_directory=$(mktemp -d)
trap 'rm -rf "$smoke_directory"' EXIT
export SWIFTPM_MODULECACHE_OVERRIDE="$smoke_directory/module-cache"
mkdir -p "$SWIFTPM_MODULECACHE_OVERRIDE"

mkdir -p "$smoke_directory/Sources/DingRemoteSmoke"
sed "s/__DING_VERSION__/$version/g" \
    "$repository_root/swiftpm/RemoteSmoke.Package.swift.template" \
    > "$smoke_directory/Package.swift"
cp "$repository_root/swiftpm/Smoke.swift" \
    "$smoke_directory/Sources/DingRemoteSmoke/Smoke.swift"

swift package resolve --package-path "$smoke_directory"

framework_path=$(find "$smoke_directory/.build/artifacts" \
    -type d -name Ding.xcframework -print -quit)
if [ -z "$framework_path" ]; then
    echo "Resolved package does not contain Ding.xcframework." >&2
    exit 1
fi

simulator_sdk_path=$(xcrun --sdk iphonesimulator --show-sdk-path)
xcrun swiftc \
    -typecheck \
    -target arm64-apple-ios13.0-simulator \
    -sdk "$simulator_sdk_path" \
    -F "$framework_path/ios-arm64-simulator" \
    "$smoke_directory/Sources/DingRemoteSmoke/Smoke.swift"
