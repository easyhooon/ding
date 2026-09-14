import Ding
import UIKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private static let databaseFileName = "ding-e2e.db"
    private static let readyMarkerFileName = "ding-e2e-ready.json"
    private static let resultMarkerFileName = "ding-e2e-result.json"
    private static let stateMarkerFileName = "ding-e2e-state.json"

    private var capture: DingAppleCapture?
    private weak var application: UIApplication?
    private var isNotificationAuthorizationReady = false
    private var didWriteReadyMarker = false

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        self.application = application
        let notificationCenter = UNUserNotificationCenter.current()
        notificationCenter.delegate = self

        do {
            let databaseDirectory = try applicationSupportDirectory()
            let databasePath = databaseDirectory
                .appendingPathComponent(Self.databaseFileName)
                .path

            PersistentDingCaptureStore.companion.get(
                storagePath: databasePath,
                maxSnapshots: 50
            ) { [weak self] store, error in
                guard let self else { return }
                guard let store else {
                    self.writeErrorMarker(error, fileName: Self.readyMarkerFileName)
                    return
                }

                self.capture = DingAppleCapture(store: store)
                notificationCenter.requestAuthorization(
                    options: [.alert, .sound, .provisional]
                ) { granted, authorizationError in
                    guard authorizationError == nil else {
                        self.writeErrorMarker(
                            authorizationError,
                            fileName: Self.readyMarkerFileName
                        )
                        return
                    }
                    guard granted else {
                        self.writeErrorMarker(
                            nil,
                            message: "Provisional notification authorization was not granted",
                            fileName: Self.readyMarkerFileName
                        )
                        return
                    }

                    DispatchQueue.main.async {
                        application.registerForRemoteNotifications()
                        self.isNotificationAuthorizationReady = true
                        self.waitForActiveApplication()
                    }
                }
            }
        } catch {
            writeErrorMarker(error, fileName: Self.readyMarkerFileName)
        }

        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        writeReadyMarkerIfPossible()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        writeStateMarker("will-present")
        completionHandler([.banner, .list, .sound])

        guard let capture else {
            writeErrorMarker(nil, message: "Ding capture is not ready")
            return
        }

        capture.captureAppleUserInfo(
            userInfo: notification.request.content.userInfo,
            transport: .apns,
            capturePoint: .foreground
        ) { [weak self] snapshot, error in
            guard let self else { return }
            self.writeStateMarker("capture-completion")
            guard snapshot != nil else {
                self.writeErrorMarker(error)
                return
            }

            capture.snapshots { [weak self] snapshots, snapshotsError in
                guard let self else { return }
                self.writeStateMarker("snapshots-completion")
                guard let persistedSnapshot = snapshots?.last else {
                    self.writeErrorMarker(
                        snapshotsError,
                        message: "Ding did not return the persisted snapshot"
                    )
                    return
                }

                self.writeStateMarker("snapshot-selected")
                self.writeSnapshotMarker(persistedSnapshot)
            }
        }
    }

    private func applicationSupportDirectory() throws -> URL {
        let directory = FileManager.default.urls(
            for: .applicationSupportDirectory,
            in: .userDomainMask
        )[0].appendingPathComponent("DingPushTestHost", isDirectory: true)
        try FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )
        return directory
    }

    private func markerURL(fileName: String) -> URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(fileName)
    }

    private func writeReadyMarkerIfPossible() {
        guard application?.applicationState == .active,
              isNotificationAuthorizationReady,
              !didWriteReadyMarker else {
            return
        }

        didWriteReadyMarker = true
        writeJSONObject(
            [
                "ready": true,
                "delegateInstalled": UNUserNotificationCenter.current().delegate === self,
            ],
            to: markerURL(fileName: Self.readyMarkerFileName)
        )
    }

    private func waitForActiveApplication() {
        guard !didWriteReadyMarker else { return }

        if application?.applicationState == .active {
            writeReadyMarkerIfPossible()
            return
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            self?.waitForActiveApplication()
        }
    }

    private func writeSnapshotMarker(_ snapshot: String) {
        guard let data = snapshot.data(using: .utf8) else {
            writeStateMarker("snapshot-encoding-failed")
            writeErrorMarker(nil, message: "Ding returned a non-UTF-8 snapshot")
            return
        }

        do {
            try data.write(
                to: markerURL(fileName: Self.resultMarkerFileName),
                options: .atomic
            )
            writeStateMarker("result-written")
        } catch {
            writeStateMarker("result-write-failed")
            writeErrorMarker(error)
        }
    }

    private func writeStateMarker(_ stage: String) {
        writeJSONObject(
            ["stage": stage],
            to: markerURL(fileName: Self.stateMarkerFileName)
        )
    }

    private func writeErrorMarker(
        _ error: Error?,
        message: String? = nil,
        fileName: String = resultMarkerFileName
    ) {
        writeJSONObject(
            ["e2eError": message ?? error?.localizedDescription ?? "Unknown error"],
            to: markerURL(fileName: fileName)
        )
    }

    private func writeJSONObject(_ object: [String: Any], to url: URL) {
        do {
            let data = try JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])
            try data.write(to: url, options: .atomic)
        } catch {
            NSLog("DingPushTestHost marker write failed: \(error.localizedDescription)")
        }
    }
}
