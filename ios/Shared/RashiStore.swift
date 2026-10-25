import Foundation
import Observation

@MainActor
@Observable
final class RashiStore {
    private(set) var prediction: RashiPrediction?
    private(set) var isLoading = false
    private(set) var lastError: String?

    func refresh(sign: RashiSign) async {
        isLoading = true
        defer { isLoading = false }
        do {
            prediction = try await RashiService.fetch(sign: sign)
            lastError = nil
        } catch {
            lastError = error.localizedDescription
        }
    }
}
