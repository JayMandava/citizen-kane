import Foundation

/// A single headline pulled from a news source.
///
/// `id` is derived from the URL rather than the scrape timestamp, so the same
/// article keeps the same identity across refreshes — read state, bookmarks,
/// and SwiftUI list diffing all depend on that staying stable.
struct Article: Identifiable, Hashable, Codable {
    let id: String
    let source: String
    let category: String
    let title: String
    let url: URL
    let imageURL: URL?
    /// A short blurb scraped alongside the headline, when the source's
    /// markup includes one. Not every source has this — Andhrajyothi's
    /// section pages only expose a title and link.
    let snippet: String?
    let scrapedAt: Date

    init(
        source: String,
        category: String,
        title: String,
        url: URL,
        imageURL: URL?,
        snippet: String? = nil,
        scrapedAt: Date = .now
    ) {
        self.id = url.absoluteString
        self.source = source
        self.category = category
        self.title = title
        self.url = url
        self.imageURL = imageURL
        self.snippet = snippet
        self.scrapedAt = scrapedAt
    }

    /// Returns a copy with a different image — for a source that discovers
    /// its image in a second pass (e.g. fetching the article page for its
    /// Open Graph tag) after the article itself was already built.
    func withImageURL(_ imageURL: URL?) -> Article {
        Article(
            source: source,
            category: category,
            title: title,
            url: url,
            imageURL: imageURL,
            snippet: snippet,
            scrapedAt: scrapedAt
        )
    }
}
