import Testing
import Foundation
@testable import CitizenKane

struct HTMLParsingTests {
    @Test func extractsTheOpenGraphImageFromASyntheticArticlePage() throws {
        let html = try loadFixture("sakshi_article_head")
        let imageURL = HTMLParsing.openGraphImageURL(fromHTML: html)
        #expect(imageURL?.absoluteString == "https://example.invalid/sakshi-image.jpg")
    }

    @Test func returnsNilWhenThereIsNoOpenGraphImageTag() {
        let imageURL = HTMLParsing.openGraphImageURL(fromHTML: "<html><head></head><body></body></html>")
        #expect(imageURL == nil)
    }
}
