import Foundation
import Observation

/// User-configurable preferences, persisted to `UserDefaults`.
///
/// A plain `@Observable` wrapper rather than raw `@AppStorage` properties,
/// since providers and the notification time need richer encoding than
/// `@AppStorage`'s primitive types support.
@MainActor
@Observable
final class AppSettings {
    private enum Keys {
        static let enabledProviderIDs = "enabledProviderIDs"
        static let rashiSign = "rashiSign"
        static let appearance = "appearance"
        static let morningNotificationEnabled = "morningNotificationEnabled"
        static let morningNotificationHour = "morningNotificationHour"
        static let morningNotificationMinute = "morningNotificationMinute"
    }

    private let defaults: UserDefaults

    var enabledProviderIDs: Set<String> {
        didSet { defaults.set(Array(enabledProviderIDs), forKey: Keys.enabledProviderIDs) }
    }

    var rashiSign: RashiSign {
        didSet { defaults.set(rashiSign.rawValue, forKey: Keys.rashiSign) }
    }

    var appearance: AppAppearance {
        didSet { defaults.set(appearance.rawValue, forKey: Keys.appearance) }
    }

    var morningNotificationEnabled: Bool {
        didSet { defaults.set(morningNotificationEnabled, forKey: Keys.morningNotificationEnabled) }
    }

    /// Hour/minute only — local notifications repeat by time of day, not a
    /// specific date, so this is a wall-clock time, not a `Date`.
    var morningNotificationHour: Int {
        didSet { defaults.set(morningNotificationHour, forKey: Keys.morningNotificationHour) }
    }

    var morningNotificationMinute: Int {
        didSet { defaults.set(morningNotificationMinute, forKey: Keys.morningNotificationMinute) }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults

        if let storedIDs = defaults.array(forKey: Keys.enabledProviderIDs) as? [String] {
            enabledProviderIDs = Set(storedIDs)
        } else {
            enabledProviderIDs = ["eenadu", "andhrajyothi", "sakshi", "namasthe-telangana", "bbc-telugu"]
        }

        if let storedSign = defaults.string(forKey: Keys.rashiSign),
           let sign = RashiSign(rawValue: storedSign) {
            rashiSign = sign
        } else {
            rashiSign = .makar
        }

        if let storedAppearance = defaults.string(forKey: Keys.appearance),
           let appearance = AppAppearance(rawValue: storedAppearance) {
            self.appearance = appearance
        } else {
            appearance = .system
        }

        morningNotificationEnabled = defaults.bool(forKey: Keys.morningNotificationEnabled)
        let storedHour = defaults.integer(forKey: Keys.morningNotificationHour)
        morningNotificationHour = defaults.object(forKey: Keys.morningNotificationHour) != nil ? storedHour : 7
        let storedMinute = defaults.integer(forKey: Keys.morningNotificationMinute)
        morningNotificationMinute = defaults.object(forKey: Keys.morningNotificationMinute) != nil ? storedMinute : 0
    }

    func isProviderEnabled(_ id: String) -> Bool {
        enabledProviderIDs.contains(id)
    }

    func setProvider(_ id: String, enabled: Bool) {
        if enabled {
            enabledProviderIDs.insert(id)
        } else {
            enabledProviderIDs.remove(id)
        }
    }

    var notificationTimeComponents: DateComponents {
        var components = DateComponents()
        components.hour = morningNotificationHour
        components.minute = morningNotificationMinute
        return components
    }
}
