import Foundation

/// Persists the last successful feed to disk, so the app has something to
/// show the instant it launches instead of a blank screen while the network
/// fetch is in flight — this is the whole "cache" for now: no expiry policy,
/// no partial invalidation, just the last known-good snapshot.
enum ArticleCache {
    private static let fileName = "cached-articles.json"

    private static var fileURL: URL? {
        guard let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        else { return nil }
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory.appendingPathComponent(fileName)
    }

    static func load() -> [Article]? {
        guard let url = fileURL, let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode([Article].self, from: data)
    }

    static func save(_ articles: [Article]) {
        guard let url = fileURL, let data = try? JSONEncoder().encode(articles) else { return }
        try? data.write(to: url, options: .atomic)
    }
}
