import Testing
import Foundation
@testable import CitizenKane

struct SakshiSourceTests {
    @Test func parsesEveryItemInTheFixture() throws {
        let xml = try loadFixture("sakshi_feed", withExtension: "xml")
        let articles = SakshiSource.parse(xml: xml)
        #expect(articles.count == 2)
    }

    @Test func extractsTitleAndLinkFromASyntheticItem() throws {
        let xml = try loadFixture("sakshi_feed", withExtension: "xml")
        let articles = SakshiSource.parse(xml: xml)

        let first = try #require(articles.first)
        #expect(first.title.contains("సింథటిక్"))
        #expect(first.url.absoluteString.contains("sakshi-first"))
        #expect(first.source == "Sakshi")
    }

    @Test func parsingTheFeedAloneHasNoImageOrSnippet() throws {
        // `parse(xml:)` is the pure, network-free half of this source — the
        // feed itself has neither field. `fetchArticles()` fills the image
        // in separately, from each article's own page (see HTMLParsingTests).
        let xml = try loadFixture("sakshi_feed", withExtension: "xml")
        let articles = SakshiSource.parse(xml: xml)

        let first = try #require(articles.first)
        #expect(first.imageURL == nil)
    }
}
