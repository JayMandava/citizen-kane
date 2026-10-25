import Foundation
import SwiftSoup

/// Scrapes Eenadu's section pages for headlines.
///
/// `parse(html:category:)` is a pure function with no network dependency —
/// `EenaduSourceTests` exercises it against a recorded fixture, so a markup
/// change on Eenadu's side shows up as a failing test instead of a silently
/// empty feed the next morning.
struct EenaduSource: NewsSource {
    let id = "eenadu"
    let displayName = "Eenadu"

    private struct Section {
        let path: String
        let category: String
    }

    private static let baseURL = URL(string: "https://www.eenadu.net")!
    private static let maxPerSection = 20
    private static let userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    private static let sections: [Section] = [
        Section(path: "/latest-news", category: "Latest"),
        Section(path: "/telangana", category: "Telangana"),
        Section(path: "/andhra-pradesh", category: "Andhra Pradesh"),
        Section(path: "/sports", category: "Sports"),
        Section(path: "/movies", category: "Cinema"),
        Section(path: "/business", category: "Business"),
    ]

    func fetchArticles() async throws -> [Article] {
        try await withThrowingTaskGroup(of: [Article].self) { group in
            for section in Self.sections {
                group.addTask {
                    let url = Self.baseURL.appendingPathComponent(section.path)
                    var request = URLRequest(url: url)
                    request.setValue(Self.userAgent, forHTTPHeaderField: "User-Agent")
                    let (data, _) = try await URLSession.shared.data(for: request)
                    guard let html = String(data: data, encoding: .utf8) else { return [] }
                    return Self.parse(html: html, category: section.category)
                }
            }
            var all: [Article] = []
            for try await articles in group {
                all.append(contentsOf: articles)
            }
            return Self.deduplicate(all)
        }
    }

    static func parse(html: String, category: String) -> [Article] {
        guard let document = try? SwiftSoup.parse(html) else { return [] }
        guard let cards = try? document.select(".bt-thumb-description") else { return [] }

        var articles: [Article] = []
        for card in cards.array().prefix(maxPerSection) {
            guard let article = makeArticle(from: card, category: category) else { continue }
            articles.append(article)
        }
        return articles
    }

    private static func makeArticle(from card: Element, category: String) -> Article? {
        let titleElement = HTMLParsing.firstElement(card, matching: "h3.article-title-rgt")
            ?? HTMLParsing.firstElement(card, matching: "h2, h3, h4")
        guard let titleElement,
              let rawTitle = try? titleElement.text(),
              !rawTitle.isEmpty
        else { return nil }

        guard let anchor = HTMLParsing.nearestAnchor(from: card),
              let href = try? anchor.attr("href"),
              let url = HTMLParsing.absoluteURL(from: href, relativeTo: baseURL)
        else { return nil }

        let imageURL = HTMLParsing.firstElement(anchor, matching: "img")
            .flatMap { try? $0.attr("src") }
            .flatMap { HTMLParsing.absoluteURL(from: $0, relativeTo: baseURL) }

        // The snippet, when present, is a bare text node directly inside the
        // card (after the title and time span) — `ownText()` picks up only
        // text owned directly by this element, not its children's text.
        let snippet = (try? card.ownText())
            .map(HTMLParsing.normalizeWhitespace)
            .flatMap { $0.isEmpty ? nil : $0 }

        return Article(
            source: "Eenadu",
            category: category,
            title: HTMLParsing.normalizeWhitespace(rawTitle),
            url: url,
            imageURL: imageURL,
            snippet: snippet
        )
    }

    private static func deduplicate(_ articles: [Article]) -> [Article] {
        var seen = Set<String>()
        return articles.filter { seen.insert($0.url.absoluteString).inserted }
    }
}
