import SwiftUI

struct NewsListView: View {
    var store: NewsStore
    var settings: AppSettings

    @State private var showSettings = false
    @State private var selectedArticle: Article?

    var body: some View {
        NavigationStack {
            List {
                ForEach(store.articles) { article in
                    Button {
                        selectedArticle = article
                    } label: {
                        ArticleRow(article: article)
                    }
                    .buttonStyle(.plain)
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(GradientBackgroundView())
            .navigationTitle("సిటిజన్ కేన్")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .refreshable {
                await store.refresh()
            }
            .overlay {
                if store.isLoading && store.articles.isEmpty {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .accessibilityLabel("Loading news")
                } else if let error = store.lastError, store.articles.isEmpty {
                    ContentUnavailableView(
                        "Couldn't load news",
                        systemImage: "wifi.slash",
                        description: Text(error)
                    )
                } else if store.articles.isEmpty {
                    ContentUnavailableView(
                        "No sources enabled",
                        systemImage: "newspaper",
                        description: Text("Enable a provider in Settings.")
                    )
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView(settings: settings)
            }
            .sheet(item: $selectedArticle) { article in
                ArticleDetailView(article: article)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
        }
        .task {
            await store.refresh()
        }
    }
}
