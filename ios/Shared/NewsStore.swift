import Foundation
import Observation

/// Aggregates every enabled `NewsSource` into one fairly-mixed feed.
///
/// Reads `AppSettings.enabledProviderIDs` fresh on every refresh, so toggling
/// a provider in Settings takes effect on the next pull without restarting
/// the app. Falls back to a disk cache (`ArticleCache`) at launch so there's
/// something on screen before the first network round trip completes.
@MainActor
@Observable
final class NewsStore {
    private(set) var articles: [Article] = []
    private(set) var isLoading = false
    private(set) var lastError: String?

    private let allSources: [NewsSource]
    private let settings: AppSettings

    init(
        settings: AppSettings,
        sources: [NewsSource] = [
            EenaduSource(),
            AndhrajyothiSource(),
            SakshiSource(),
            NamasteTelanganaSource(),
            BBCTeluguSource(),
        ]
    ) {
        self.settings = settings
        self.allSources = sources
        self.articles = ArticleCache.load() ?? []
    }

    func refresh() async {
        isLoading = true
        defer { isLoading = false }

        let activeSources = allSources.filter { settings.isProviderEnabled($0.id) }
        var bySourceID: [String: [Article]] = [:]
        var failureMessage: String?

        await withTaskGroup(of: (String, Result<[Article], Error>).self) { group in
            for source in activeSources {
                group.addTask {
                    do {
                        return (source.id, .success(try await source.fetchArticles()))
                    } catch {
                        return (source.id, .failure(error))
                    }
                }
            }
            for await (sourceID, result) in group {
                switch result {
                case .success(let articles):
                    // Sorted within its own source, where a recency signal
                    // (real pubDate, or scrape time) is actually comparable.
                    bySourceID[sourceID] = articles.sorted { $0.scrapedAt > $1.scrapedAt }
                case .failure(let error):
                    failureMessage = error.localizedDescription
                }
            }
        }

        // A scraped source's "scrapedAt" is just the moment it was fetched —
        // it isn't comparable to an RSS source's real `pubDate`. Sorting the
        // combined list by that timestamp would let every scraped article
        // (all stamped "now") permanently outrank every RSS article (stamped
        // with its real, older publish time), burying RSS sources entirely.
        // Interleaving round-robin, in the fixed provider order set at init,
        // keeps every enabled source fairly represented near the top instead.
        let grouped = activeSources.compactMap { bySourceID[$0.id] }
        let merged = Self.interleave(grouped)

        if !merged.isEmpty {
            articles = merged
            lastError = nil
            ArticleCache.save(merged)
        } else if !activeSources.isEmpty {
            lastError = failureMessage
        }
    }

    private static func interleave(_ groups: [[Article]]) -> [Article] {
        var result: [Article] = []
        result.reserveCapacity(groups.reduce(0) { $0 + $1.count })

        var indices = Array(repeating: 0, count: groups.count)
        var remaining = groups.count
        while remaining > 0 {
            remaining = 0
            for groupIndex in groups.indices {
                let position = indices[groupIndex]
                guard position < groups[groupIndex].count else { continue }
                result.append(groups[groupIndex][position])
                indices[groupIndex] += 1
                if indices[groupIndex] < groups[groupIndex].count {
                    remaining += 1
                }
            }
        }
        return result
    }
}
