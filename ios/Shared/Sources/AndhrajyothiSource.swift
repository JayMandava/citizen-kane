import Foundation
import SwiftSoup

/// Scrapes Andhrajyothi's section pages for headlines.
///
/// Andhrajyothi wraps each card as `<figure><div class="image_wrap">…image
/// link…</div><figcaption><h3><a>title</a></h3></figcaption></figure>` — the
/// title's own anchor is the link, and the image lives one level up in the
/// same `<figure>`. `AndhrajyothiSourceTests` locks this against a recorded
/// fixture the same way `EenaduSourceTests` does.
struct AndhrajyothiSource: NewsSource {
    let id = "andhrajyothi"
    let displayName = "Andhrajyothi"

    private struct Section {
        let path: String
        let category: String
    }

    private static let baseURL = URL(string: "https://www.andhrajyothy.com")!
    private static let maxPerSection = 20
    private static let userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    private static let minimumTitleLength = 15

    private static let sections: [Section] = [
        Section(path: "/telangana", category: "Telangana"),
        Section(path: "/andhra-pradesh", category: "Andhra Pradesh"),
        Section(path: "/latest-news", category: "Latest"),
        Section(path: "/sports", category: "Sports"),
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
        guard let headings = try? document.select("h3") else { return [] }

        var articles: [Article] = []
        for heading in headings.array().prefix(maxPerSection) {
            guard let article = makeArticle(from: heading, category: category) else { continue }
            articles.append(article)
        }
        return articles
    }

    private static func makeArticle(from heading: Element, category: String) -> Article? {
        guard let anchor = HTMLParsing.firstElement(heading, matching: "a"),
              let rawTitle = try? anchor.text(),
              rawTitle.count >= minimumTitleLength,
              let href = try? anchor.attr("href"),
              let url = HTMLParsing.absoluteURL(from: href, relativeTo: baseURL)
        else { return nil }

        // The title's <h3> sits inside <figcaption>, a sibling of the image's
        // wrapping <div> — walk up to the enclosing <figure> to find it.
        let figure = heading.parent()?.parent()
        let imageURL = figure
            .flatMap { HTMLParsing.firstElement($0, matching: "img") }
            .flatMap { try? $0.attr("src") }
            .flatMap { HTMLParsing.absoluteURL(from: $0, relativeTo: baseURL) }

        return Article(
            source: "Andhrajyothi",
            category: category,
            title: HTMLParsing.normalizeWhitespace(rawTitle),
            url: url,
            imageURL: imageURL
        )
    }

    private static func deduplicate(_ articles: [Article]) -> [Article] {
        var seen = Set<String>()
        return articles.filter { seen.insert($0.url.absoluteString).inserted }
    }
}
