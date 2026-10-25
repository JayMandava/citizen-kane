import SwiftUI

struct RootView: View {
    private let settings: AppSettings
    private let newsStore: NewsStore

    init() {
        let settings = AppSettings()
        self.settings = settings
        self.newsStore = NewsStore(settings: settings)
    }

    var body: some View {
        TabView {
            NewsListView(store: newsStore, settings: settings)
                .tabItem { Label("News", systemImage: "newspaper") }
            RashiView(settings: settings)
                .tabItem { Label("Rashi", systemImage: "sparkles") }
        }
        .tint(ClassicTheme.ink)
        .preferredColorScheme(settings.appearance.colorScheme)
    }
}
