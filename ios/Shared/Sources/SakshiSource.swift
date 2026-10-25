import Foundation

/// Sakshi's RSS feed. It has neither an image field nor a usable
/// description — the `<description>` is just author/date boilerplate
/// markup, not article text — so an article's image is fetched in a second
/// pass, from its own page's Open Graph tag, rather than left blank.
struct SakshiSource: NewsSource {
    let id = "sakshi"
    let displayName = "Sakshi"

    private static let feedURL = URL(string: "https://www.sakshi.com/rss.xml")!
    private static let category = "Latest"
    private static let userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    func fetchArticles() async throws -> [Article] {
        let (data, _) = try await URLSession.shared.data(from: Self.feedURL)
        guard let xml = String(data: data, encoding: .utf8) else { return [] }
        let articles = Self.parse(xml: xml)
        return await Self.attachingImages(to: articles)
    }

    static func parse(xml: String) -> [Article] {
        RSSParsing.items(fromXML: xml).compactMap(makeArticle)
    }

    private static func makeArticle(from item: RSSParsing.Item) -> Article? {
        guard let title = item.title, let link = item.link, let url = URL(string: link) else { return nil }

        let publishedAt = RSSParsing.parseDate(item.pubDate)

        return Article(
            source: "Sakshi",
            category: category,
            title: HTMLParsing.normalizeWhitespace(title),
            url: url,
            imageURL: nil,
            scrapedAt: publishedAt ?? .now
        )
    }

    /// Fetches each article's own page in parallel for its `og:image` — the
    /// feed has 10 items per refresh, so this stays a handful of concurrent
    /// requests, the same shape as the section fan-out the scraped sources
    /// already do.
    private static func attachingImages(to articles: [Article]) async -> [Article] {
        await withTaskGroup(of: Article.self) { group in
            for article in articles {
                group.addTask {
                    guard let imageURL = await fetchOpenGraphImageURL(for: article.url) else { return article }
                    return article.withImageURL(imageURL)
                }
            }
            var results: [Article] = []
            for await article in group {
                results.append(article)
            }
            return results
        }
    }

    private static func fetchOpenGraphImageURL(for url: URL) async -> URL? {
        var request = URLRequest(url: url)
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        guard let (data, _) = try? await URLSession.shared.data(for: request),
              let html = String(data: data, encoding: .utf8)
        else { return nil }
        return HTMLParsing.openGraphImageURL(fromHTML: html)
    }
}
