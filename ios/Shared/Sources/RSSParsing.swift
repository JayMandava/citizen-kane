import Foundation
import SwiftSoup

/// Shared helpers for the RSS-based sources (Sakshi, Namasthe Telangana, BBC
/// Telugu).
///
/// This uses Foundation's `XMLParser`, not SwiftSoup, for the outer feed
/// document — SwiftSoup's XML mode does not treat CDATA as fully opaque, so
/// a `<content:encoded>` block containing real HTML (`<img>`, `<h2>`, `<p>`,
/// as WordPress feeds do) corrupts the parsed tree and other fields silently
/// come out wrong. `XMLParser` hands CDATA to its own `foundCDATA` callback,
/// verbatim, exactly as the XML spec requires. SwiftSoup is still used
/// below to parse an already-extracted HTML fragment (e.g. pulling the
/// first `<img>` out of a `content:encoded` string) — that's parsing real
/// HTML, which is what it's built for.
enum RSSParsing {
    struct Item {
        var title: String?
        var link: String?
        var description: String?
        var pubDate: String?
        var contentEncoded: String?
        var mediaThumbnailURL: String?
    }

    static func items(fromXML xml: String) -> [Item] {
        guard let data = xml.data(using: .utf8) else { return [] }
        let delegate = FeedDelegate()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        parser.parse()
        return delegate.items
    }

    /// The featured image inside a WordPress `content:encoded` HTML blob —
    /// its first `<img>`, which is conventionally the post's own image.
    static func firstImageSrc(inHTML html: String) -> String? {
        guard let fragment = try? SwiftSoup.parseBodyFragment(html),
              let img = try? fragment.select("img").first(),
              let src = try? img.attr("src"),
              !src.isEmpty
        else { return nil }
        return src
    }

    /// RSS `pubDate` is RFC 822, but in the wild that means either a numeric
    /// offset ("+0530") or a literal zone abbreviation ("GMT") — try both.
    static func parseDate(_ value: String?) -> Date? {
        guard let value else { return nil }
        for formatter in [rfc822Numeric, rfc822Named] {
            if let date = formatter.date(from: value) {
                return date
            }
        }
        return nil
    }

    private static let rfc822Numeric: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "EEE, d MMM yyyy HH:mm:ss Z"
        return formatter
    }()

    private static let rfc822Named: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "EEE, d MMM yyyy HH:mm:ss zzz"
        return formatter
    }()

    private final class FeedDelegate: NSObject, XMLParserDelegate {
        private(set) var items: [Item] = []
        private var current: Item?
        private var currentElement = ""
        private var buffer = ""

        func parser(
            _ parser: XMLParser,
            didStartElement elementName: String,
            namespaceURI: String?,
            qualifiedName qName: String?,
            attributes attributeDict: [String: String] = [:]
        ) {
            currentElement = elementName
            buffer = ""
            if elementName == "item" {
                current = Item()
            } else if elementName == "media:thumbnail" {
                current?.mediaThumbnailURL = attributeDict["url"]
            }
        }

        func parser(_ parser: XMLParser, foundCharacters string: String) {
            buffer += string
        }

        func parser(_ parser: XMLParser, foundCDATA CDATABlock: Data) {
            if let string = String(data: CDATABlock, encoding: .utf8) {
                buffer += string
            }
        }

        func parser(
            _ parser: XMLParser,
            didEndElement elementName: String,
            namespaceURI: String?,
            qualifiedName qName: String?
        ) {
            let trimmed = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
            switch elementName {
            case "title": if !trimmed.isEmpty { current?.title = trimmed }
            case "link": if !trimmed.isEmpty { current?.link = trimmed }
            case "description": current?.description = trimmed
            case "pubDate": current?.pubDate = trimmed
            case "content:encoded": current?.contentEncoded = trimmed
            case "item":
                if let item = current { items.append(item) }
                current = nil
            default: break
            }
            buffer = ""
        }
    }
}
