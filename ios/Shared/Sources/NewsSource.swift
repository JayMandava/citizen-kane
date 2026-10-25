import Foundation

/// A single publisher this app aggregates from.
///
/// Every source is scraped, and every publisher's markup changes without
/// warning — see `EenaduSource` for the fixture-tested pattern that catches
/// that drift before it silently empties the feed.
protocol NewsSource: Sendable {
    /// Stable identifier used by settings to enable/disable this source
    /// (e.g. "eenadu") — never shown in the UI, unlike `displayName`.
    var id: String { get }
    var displayName: String { get }
    func fetchArticles() async throws -> [Article]
}

enum NewsSourceError: Error {
    case fetchFailed(String)
    case noArticlesFound
}
