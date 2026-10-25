import Testing
import Foundation
@testable import CitizenKane

struct NamasteTelanganaSourceTests {
    @Test func parsesEveryItemInTheFixture() throws {
        let xml = try loadFixture("namasthe_telangana_feed", withExtension: "xml")
        let articles = NamasteTelanganaSource.parse(xml: xml)
        #expect(articles.count == 2)
    }

    @Test func extractsTitleLinkAndFeaturedImageFromASyntheticItem() throws {
        let xml = try loadFixture("namasthe_telangana_feed", withExtension: "xml")
        let articles = NamasteTelanganaSource.parse(xml: xml)

        let first = try #require(articles.first)
        #expect(first.title.hasPrefix("సింథటిక్"))
        #expect(first.url.absoluteString.contains("namasthe-first"))
        #expect(first.imageURL?.absoluteString.hasPrefix("https://example.invalid/") == true)
        #expect(first.source == "Namasthe Telangana")
    }
}
