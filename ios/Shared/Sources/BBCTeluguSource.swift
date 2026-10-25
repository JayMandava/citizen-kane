import Foundation

/// BBC Telugu's RSS feed — clean, stable, and the one feed of the three with
/// a real per-item image (`media:thumbnail`) and a usable description.
struct BBCTeluguSource: NewsSource {
    let id = "bbc-telugu"
    let displayName = "BBC Telugu"

    private static let feedURL = URL(string: "https://feeds.bbci.co.uk/telugu/rss.xml")!
    private static let category = "Latest"

    func fetchArticles() async throws -> [Article] {
        let (data, _) = try await URLSession.shared.data(from: Self.feedURL)
        guard let xml = String(data: data, encoding: .utf8) else { return [] }
        return Self.parse(xml: xml)
    }

    static func parse(xml: String) -> [Article] {
        RSSParsing.items(fromXML: xml).compactMap(makeArticle)
    }

    private static func makeArticle(from item: RSSParsing.Item) -> Article? {
        guard let title = item.title, let link = item.link, let url = URL(string: link) else { return nil }

        let imageURL = item.mediaThumbnailURL.flatMap(URL.init(string:))
        let publishedAt = RSSParsing.parseDate(item.pubDate)

        return Article(
            source: "BBC Telugu",
            category: category,
            title: HTMLParsing.normalizeWhitespace(title),
            url: url,
            imageURL: imageURL,
            snippet: item.description.map(HTMLParsing.normalizeWhitespace),
            scrapedAt: publishedAt ?? .now
        )
    }
}
