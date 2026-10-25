import Foundation
import Testing

/// Anchor class for locating fixture files bundled with the test target.
private final class BundleToken {}

func loadFixture(_ name: String, withExtension ext: String = "html") throws -> String {
    let url = Bundle(for: BundleToken.self).url(forResource: name, withExtension: ext)
    let resolved = try #require(url, "fixture '\(name).\(ext)' not found in test bundle")
    return try String(contentsOf: resolved, encoding: .utf8)
}
