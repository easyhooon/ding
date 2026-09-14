# Ding iOS Simulator push verification

This fixture verifies the host-owned Apple capture path through a real iOS
Simulator notification callback. It builds a disposable test host against the
locally packaged `Ding.xcframework`, sends `Payloads/foreground.apns` with
`simctl push`, and checks the JSON snapshot written by Ding.

Run the verification on an Apple Silicon Mac with Xcode and an iOS Simulator
runtime installed:

```bash
./gradlew :ding-core:verifyDingIosSimulatorPushCapture
```

The runner performs these steps:

1. Packages the local Swift package and static XCFramework.
2. Creates and boots a disposable iPhone Simulator.
3. Builds, installs, and launches `DingPushTestHost` without code signing.
4. Waits for `Documents/ding-e2e-ready.json` in the app data container.
5. Sends the foreground fixture to
   `io.github.easyhooon.ding.push-test-host` with `simctl push`.
6. Waits for `Documents/ding-e2e-result.json` and validates the actual Ding
   snapshot with Foundation's JSON parser, including JSON `null` values that
   property-list tooling cannot represent.
7. Shuts down and deletes the Simulator, including after failures.

`DING_E2E_SIMULATOR_RUNTIME` and `DING_E2E_SIMULATOR_DEVICE_TYPE` can override
the dynamically selected latest available iOS runtime and first compatible
iPhone device type. `DING_E2E_WAIT_ATTEMPTS` controls the one-second polling
limit and defaults to 30. Set `DING_E2E_DUMP_LOGS=1` to include the host's
recent Simulator logs after a failure.

This is a simulated application remote notification. It verifies Simulator
delivery, the host delegate forwarding the original `userInfo`, SwiftPM binary
integration, and Ding persistence. The host requests provisional authorization
so CI does not block on a system prompt; this check therefore does not assert a
visible banner or sound. Presentation is a separate UI smoke check.

This fixture does not contact APNs or validate device tokens, signing,
provisioning, Notification Service Extensions, or real-device background
delivery.
