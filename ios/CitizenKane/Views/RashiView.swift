import SwiftUI

struct RashiView: View {
    var settings: AppSettings

    @State private var store = RashiStore()

    var body: some View {
        NavigationStack {
            ScrollView {
                if let prediction = store.prediction {
                    VStack(alignment: .leading, spacing: 20) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(settings.rashiSign.teluguName)
                                .font(ClassicTheme.headline(24))
                                .foregroundStyle(ClassicTheme.ink)
                            Text(settings.rashiSign.englishName)
                                .font(.subheadline)
                                .foregroundStyle(ClassicTheme.inkSecondary)
                            if !prediction.dateLabel.isEmpty {
                                Text(prediction.dateLabel)
                                    .font(.caption)
                                    .foregroundStyle(ClassicTheme.inkSecondary)
                            }
                        }

                        section(title: "Today", body: prediction.today)

                        if !prediction.weekly.isEmpty {
                            Divider().background(ClassicTheme.rule)
                            section(title: "This Week", body: prediction.weekly)
                        }
                    }
                    .padding()
                } else if store.isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .accessibilityLabel("Loading rashi phalalu")
                        .frame(maxWidth: .infinity)
                        .padding(.top, 80)
                } else if let error = store.lastError {
                    ContentUnavailableView(
                        "Couldn't load rashi phalalu",
                        systemImage: "sparkles",
                        description: Text(error)
                    )
                    .padding(.top, 40)
                }
            }
            // Keep the backdrop full-width even when the only content is
            // a spinner or a short error message.
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(GradientBackgroundView())
            .navigationTitle("Rashi Phalalu")
            .refreshable {
                await store.refresh(sign: settings.rashiSign)
            }
        }
        .task(id: settings.rashiSign) {
            await store.refresh(sign: settings.rashiSign)
        }
    }

    private func section(title: String, body: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(ClassicTheme.kicker())
                .foregroundStyle(ClassicTheme.ink)
            Text(body)
                .font(ClassicTheme.body())
                .foregroundStyle(ClassicTheme.ink)
                .lineSpacing(4)
        }
    }
}
