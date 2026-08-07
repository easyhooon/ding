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
