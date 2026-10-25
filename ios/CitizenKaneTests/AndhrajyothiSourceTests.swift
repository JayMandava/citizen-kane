import Testing
import Foundation
@testable import CitizenKane

struct AndhrajyothiSourceTests {
    @Test func parsesEveryCardInTheFixture() throws {
        let html = try loadFixture("andhrajyothi_telangana")
        let articles = AndhrajyothiSource.parse(html: html, category: "Telangana")
        #expect(articles.count == 4)
    }

    @Test func extractsTitleLinkAndImageFromASyntheticCard() throws {
        let html = try loadFixture("andhrajyothi_telangana")
        let articles = AndhrajyothiSource.parse(html: html, category: "Telangana")

        let first = try #require(articles.first)
        #expect(first.title.contains("సింథటిక్"))
        #expect(first.url.absoluteString.contains("synthetic-telangana-one"))
        #expect(first.url.absoluteString.hasPrefix("https://www.andhrajyothy.com/"))
        #expect(first.imageURL != nil)
        #expect(first.category == "Telangana")
        #expect(first.source == "Andhrajyothi")
    }

    @Test func skipsHeadingsShorterThanTheMinimumTitleLength() {
        let html = "<html><body><figure><figcaption><h3><a href=\"/x\">too short</a></h3></figcaption></figure></body></html>"
        let articles = AndhrajyothiSource.parse(html: html, category: "Telangana")
        #expect(articles.isEmpty)
    }
}
