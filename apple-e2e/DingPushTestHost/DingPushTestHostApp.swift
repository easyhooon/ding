import SwiftUI

@main
struct DingPushTestHostApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            Text("Ding Push Test Host")
        }
    }
}
