import SwiftUI

@main
struct CitizenKaneApp: App {
    init() {
        Self.configureNavigationBarFont()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }

    /// `.navigationTitle` has no `.font()` modifier of its own — the large
    /// and inline nav-bar titles are styled through the UIKit appearance
    /// proxy. Falls back to the system font if NTR somehow fails to load.
    private static func configureNavigationBarFont() {
        let largeTitleFont = UIFont(name: "NTR", size: 34) ?? .systemFont(ofSize: 34, weight: .bold)
        let titleFont = UIFont(name: "NTR", size: 17) ?? .systemFont(ofSize: 17, weight: .semibold)

        UINavigationBar.appearance().largeTitleTextAttributes = [.font: largeTitleFont]
        UINavigationBar.appearance().titleTextAttributes = [.font: titleFont]
    }
}
