import Testing
import Foundation
@testable import CitizenKane

struct EenaduSourceTests {
    @Test func parsesEveryCardInTheFixture() throws {
        let html = try loadFixture("eenadu_latest_news")
        let articles = EenaduSource.parse(html: html, category: "Latest")
        #expect(articles.count == 3)
    }

    @Test func extractsTitleLinkAndImageFromASyntheticCard() throws {
        let html = try loadFixture("eenadu_latest_news")
        let articles = EenaduSource.parse(html: html, category: "Latest")

        let first = try #require(articles.first)
        #expect(first.title == "సింథటిక్ వార్త ఒకటి: పరీక్ష కోసం రూపొందించిన శీర్షిక")
        #expect(first.url.absoluteString.contains("synthetic-eenadu-one"))
        #expect(first.imageURL?.absoluteString.hasPrefix("https://example.invalid/") == true)
        #expect(first.category == "Latest")
        #expect(first.source == "Eenadu")
    }

    @Test func extractsTheTrailingSnippetTextWhenPresent() throws {
        let html = try loadFixture("eenadu_latest_news")
        let articles = EenaduSource.parse(html: html, category: "Latest")

        let first = try #require(articles.first)
        let snippet = try #require(first.snippet)
        #expect(snippet.contains("నమూనా"))
        #expect(!snippet.contains(first.title))
    }

    @Test func assignsAStableIdentityDerivedFromTheURL() throws {
        let html = try loadFixture("eenadu_latest_news")
        let articles = EenaduSource.parse(html: html, category: "Latest")

        let first = try #require(articles.first)
        #expect(first.id == first.url.absoluteString)
    }

    @Test func returnsNoArticlesForMarkupWithoutAnyCards() {
        let articles = EenaduSource.parse(html: "<html><body><p>nothing here</p></body></html>", category: "Latest")
        #expect(articles.isEmpty)
    }
}
