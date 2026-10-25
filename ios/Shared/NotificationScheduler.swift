import Foundation
import UserNotifications

/// Schedules the single repeating "your news is ready" morning notification.
///
/// iOS doesn't run app code at delivery time, so the content is fixed at
/// schedule time rather than reflecting the day's actual headlines — the
/// same constraint Dr Jay's notifications document for the same reason.
enum NotificationScheduler {
    static let identifier = "morning-news"

    static func requestAuthorization() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            return false
        }
    }

    static func scheduleMorning(at components: DateComponents) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [identifier])

        let content = UNMutableNotificationContent()
        content.title = "సిటిజన్ కేన్"
        content.body = "Your Telugu news and rashi phalalu are ready."
        content.sound = .default

        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
        center.add(request)
    }

    static func cancelMorning() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifier])
    }
}
