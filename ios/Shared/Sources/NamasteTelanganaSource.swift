import Foundation

/// Namasthe Telangana's RSS feed — standard WordPress output. It has no
/// dedicated image field; the featured image is the first `<img>` inside
/// `content:encoded`'s embedded HTML.
struct NamasteTelanganaSource: NewsSource {
    let id = "namasthe-telangana"
    let displayName = "Namasthe Telangana"

    private static let feedURL = URL(string: "https://www.ntnews.com/feed")!
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

        let imageURL = item.contentEncoded
            .flatMap { RSSParsing.firstImageSrc(inHTML: $0) }
            .flatMap(URL.init(string:))
        let publishedAt = RSSParsing.parseDate(item.pubDate)

        return Article(
            source: "Namasthe Telangana",
            category: category,
            title: HTMLParsing.normalizeWhitespace(title),
            url: url,
            imageURL: imageURL,
            snippet: item.description.map(HTMLParsing.normalizeWhitespace),
            scrapedAt: publishedAt ?? .now
        )
    }
}
