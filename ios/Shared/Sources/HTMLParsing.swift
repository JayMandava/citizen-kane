import Foundation
import SwiftSoup

/// Small selector/URL helpers shared by every scraped source, so each
/// `NewsSource` only has to describe its own markup shape.
enum HTMLParsing {
    static func firstElement(_ element: Element, matching selector: String) -> Element? {
        (try? element.select(selector))?.first()
    }

    /// Walks up from `element` looking for the nearest enclosing `<a>`.
    static func nearestAnchor(from element: Element, maxDepth: Int = 4) -> Element? {
        var current: Element? = element
        for _ in 0..<maxDepth {
            guard let node = current else { return nil }
            if node.tagName().lowercased() == "a" { return node }
            current = node.parent()
        }
        return nil
    }

    static func absoluteURL(from href: String, relativeTo baseURL: URL) -> URL? {
        if let url = URL(string: href), url.scheme != nil {
            return url
        }
        return URL(string: href, relativeTo: baseURL)?.absoluteURL
    }

    static func normalizeWhitespace(_ text: String) -> String {
        text
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
    }

    /// A page's `<meta property="og:image">` — used for sources whose RSS
    /// feed carries no image of its own, but whose article pages do (nearly
    /// every modern site sets Open Graph tags for link-preview/social
    /// sharing, so this is a far more stable target than a CSS selector).
    static func openGraphImageURL(fromHTML html: String) -> URL? {
        guard let document = try? SwiftSoup.parse(html),
              let meta = try? document.select("meta[property=og:image]").first(),
              let content = try? meta.attr("content"),
              !content.isEmpty
        else { return nil }
        return URL(string: content)
    }
}
