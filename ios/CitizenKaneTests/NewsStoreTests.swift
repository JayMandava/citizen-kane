import Testing
import Foundation
@testable import CitizenKane

private struct FakeSource: NewsSource {
    let id: String
    let displayName: String
    let articles: [Article]

    func fetchArticles() async throws -> [Article] { articles }
}

private func makeArticle(_ id: String, source: String) -> Article {
    Article(source: source, category: "Latest", title: id, url: URL(string: "https://example.com/\(id)")!, imageURL: nil)
}

@MainActor
struct NewsStoreTests {
    @Test func interleavesArticlesAcrossSourcesInsteadOfLettingOneDominate() async throws {
        // Source A has far more articles than B and C — a naive concatenation
        // (or a sort keyed on a "now" scrape timestamp shared by every one of
        // A's articles) would let A's articles bury B and C entirely.
        let sourceA = FakeSource(
            id: "a", displayName: "A",
            articles: (1...5).map { makeArticle("a\($0)", source: "A") }
        )
        let sourceB = FakeSource(id: "b", displayName: "B", articles: [makeArticle("b1", source: "B")])
        let sourceC = FakeSource(id: "c", displayName: "C", articles: [makeArticle("c1", source: "C")])

        let settings = AppSettings(defaults: UserDefaults(suiteName: #function)!)
        settings.enabledProviderIDs = ["a", "b", "c"]
        let store = NewsStore(settings: settings, sources: [sourceA, sourceB, sourceC])

        await store.refresh()

        let sources = store.articles.map(\.source)
        #expect(sources.first == "A")
        #expect(sources[1] == "B")
        #expect(sources[2] == "C")
        #expect(sources.count == 7)
    }
}
