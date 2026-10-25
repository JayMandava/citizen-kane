import Testing
import Foundation
@testable import CitizenKane

struct BBCTeluguSourceTests {
    @Test func parsesEveryItemInTheFixture() throws {
        let xml = try loadFixture("bbc_telugu_feed", withExtension: "xml")
        let articles = BBCTeluguSource.parse(xml: xml)
        #expect(articles.count == 2)
    }

    @Test func extractsTitleLinkImageAndSnippetFromASyntheticItem() throws {
        let xml = try loadFixture("bbc_telugu_feed", withExtension: "xml")
        let articles = BBCTeluguSource.parse(xml: xml)

        let first = try #require(articles.first)
        #expect(first.title.contains("సింథటిక్"))
        #expect(first.url.absoluteString.contains("bbc-first"))
        #expect(first.imageURL?.absoluteString.hasPrefix("https://example.invalid/") == true)
        #expect(first.snippet?.contains("నమూనా") == true)
        #expect(first.source == "BBC Telugu")
    }
}
