import Ding

func makeCapture(store: DingCaptureStore) -> DingAppleCapture {
    DingAppleCapture(store: store)
}

func openStore(path: String) {
    PersistentDingCaptureStore.companion.get(
        storagePath: path,
        maxSnapshots: 50
    ) { store, error in
        _ = store
        _ = error
    }
}

func captureAndRead(
    capture: DingAppleCapture,
    userInfo: [AnyHashable: Any]
) {
    capture.captureAppleUserInfo(
        userInfo: userInfo,
        transport: .apns,
        capturePoint: .foreground
    ) { snapshot, error in
        _ = snapshot
        _ = error

        capture.snapshots { snapshots, snapshotsError in
            _ = snapshots
            _ = snapshotsError
        }
    }
}
