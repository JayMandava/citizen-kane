import SwiftUI

struct SettingsView: View {
    @Bindable var settings: AppSettings

    @Environment(\.dismiss) private var dismiss
    @State private var notificationDeniedAlert = false

    private let providers: [(id: String, name: String)] = [
        ("eenadu", "Eenadu"),
        ("andhrajyothi", "Andhrajyothi"),
        ("sakshi", "Sakshi"),
        ("namasthe-telangana", "Namasthe Telangana"),
        ("bbc-telugu", "BBC Telugu"),
    ]

    var body: some View {
        NavigationStack {
            Form {
                Section("Providers") {
                    ForEach(providers, id: \.id) { provider in
                        Toggle(provider.name, isOn: Binding(
                            get: { settings.isProviderEnabled(provider.id) },
                            set: { settings.setProvider(provider.id, enabled: $0) }
                        ))
                    }
                }

                Section("Appearance") {
                    Picker("Appearance", selection: $settings.appearance) {
                        ForEach(AppAppearance.allCases) { mode in
                            Text(mode.label).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section("Rashi Phalalu") {
                    NavigationLink {
                        RashiSignPickerView(selection: $settings.rashiSign)
                    } label: {
                        HStack {
                            Text("Your Sign")
                            Spacer()
                            Text(settings.rashiSign.teluguName)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                Section("Morning Notification") {
                    Toggle("Remind me every morning", isOn: Binding(
                        get: { settings.morningNotificationEnabled },
                        set: { handleNotificationToggle($0) }
                    ))

                    if settings.morningNotificationEnabled {
                        DatePicker(
                            "Time",
                            selection: Binding(
                                get: { timeAsDate() },
                                set: { updateTime(from: $0) }
                            ),
                            displayedComponents: .hourAndMinute
                        )
                    }
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .alert("Notifications Disabled", isPresented: $notificationDeniedAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Enable notifications for Citizen Kane in Settings to get a morning reminder.")
            }
        }
        // A sheet is a separate presentation context: it fixes its
        // color-scheme override at presentation time and won't observe a
        // change to an ancestor's `.preferredColorScheme` while it's open.
        // Applying the override here too means this sheet updates itself
        // instead of requiring a dismiss-and-reopen to pick up the change.
        .preferredColorScheme(settings.appearance.colorScheme)
    }

    private func handleNotificationToggle(_ isOn: Bool) {
        settings.morningNotificationEnabled = isOn
        if isOn {
            Task {
                let granted = await NotificationScheduler.requestAuthorization()
                if granted {
                    NotificationScheduler.scheduleMorning(at: settings.notificationTimeComponents)
                } else {
                    settings.morningNotificationEnabled = false
                    notificationDeniedAlert = true
                }
            }
        } else {
            NotificationScheduler.cancelMorning()
        }
    }

    private func timeAsDate() -> Date {
        Calendar.current.date(
            bySettingHour: settings.morningNotificationHour,
            minute: settings.morningNotificationMinute,
            second: 0,
            of: .now
        ) ?? .now
    }

    private func updateTime(from date: Date) {
        let components = Calendar.current.dateComponents([.hour, .minute], from: date)
        settings.morningNotificationHour = components.hour ?? 7
        settings.morningNotificationMinute = components.minute ?? 0
        if settings.morningNotificationEnabled {
            NotificationScheduler.scheduleMorning(at: settings.notificationTimeComponents)
        }
    }
}
