import Testing
import Foundation
@testable import CitizenKane

struct RashiServiceTests {
    @Test func parsesTodayAndWeeklyPredictionsFromTheFixture() throws {
        let html = try loadFixture("eenadu_makara_rashi")
        let prediction = try #require(RashiService.parse(html: html, sign: .makar))

        #expect(prediction.sign == .makar)
        #expect(prediction.dateLabel == "26-09-2026")
        #expect(prediction.today.hasPrefix("సింథటిక్ ఈరోజు"))
        #expect(prediction.weekly.hasPrefix("సింథటిక్ ఈవారం"))
    }

    @Test func returnsNilWhenNoTodaySectionIsFound() {
        let prediction = RashiService.parse(html: "<html><body><p>nothing</p></body></html>", sign: .makar)
        #expect(prediction == nil)
    }
}
