import Foundation

struct RashiPrediction: Equatable, Sendable {
    let sign: RashiSign
    let dateLabel: String
    let today: String
    let weekly: String
}
