import Foundation
import SwiftSoup

/// Scrapes Eenadu's per-sign rashi phalalu pages.
///
/// Each page marks its "today" and "this week" sections with `<h2 class="ttl">`
/// (occasionally `<h3.ttl>`), immediately followed by the prediction `<p>`.
/// `parse(html:sign:)` is pure and network-free, tested against a recorded
/// fixture in `RashiServiceTests`.
enum RashiService {
    private static let baseURL = URL(string: "https://www.eenadu.net/rashi-phalalu")!
    private static let userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    static func fetch(sign: RashiSign) async throws -> RashiPrediction {
        let url = baseURL.appendingPathComponent(sign.slug)
        var request = URLRequest(url: url)
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        let (data, _) = try await URLSession.shared.data(for: request)
        guard let html = String(data: data, encoding: .utf8),
              let prediction = parse(html: html, sign: sign)
        else {
            throw NewsSourceError.noArticlesFound
        }
        return prediction
    }

    static func parse(html: String, sign: RashiSign) -> RashiPrediction? {
        guard let document = try? SwiftSoup.parse(html) else { return nil }
        guard let headings = try? document.select("h2.ttl, h3.ttl") else { return nil }

        var today = ""
        var weekly = ""
        var dateLabel = ""

        for heading in headings.array() {
            guard let labelText = try? heading.text() else { continue }
            let prediction = predictionText(followingSiblingOf: heading)

            if labelText.contains("ఈరోజు") || labelText.localizedCaseInsensitiveContains("today") {
                today = prediction
                if let range = labelText.range(of: #"\(([^)]+)\)"#, options: .regularExpression) {
                    dateLabel = String(labelText[range]).trimmingCharacters(in: CharacterSet(charactersIn: "()"))
                }
            } else if labelText.contains("ఈవారం") || labelText.localizedCaseInsensitiveContains("week") {
                weekly = prediction
            }
        }

        guard !today.isEmpty else { return nil }
        return RashiPrediction(sign: sign, dateLabel: dateLabel, today: today, weekly: weekly)
    }

    /// Collects text from `<p>` elements after `heading`, stopping at the next heading.
    private static func predictionText(followingSiblingOf heading: Element) -> String {
        var parts: [String] = []
        var sibling = heading.nextSibling()
        while let node = sibling {
            if let element = node as? Element {
                let tag = element.tagName().lowercased()
                if tag == "h2" || tag == "h3" { break }
                if tag == "p", let text = try? element.text(), !text.isEmpty {
                    parts.append(text)
                }
            }
            sibling = node.nextSibling()
        }
        return HTMLParsing.normalizeWhitespace(parts.joined(separator: " "))
    }
}
