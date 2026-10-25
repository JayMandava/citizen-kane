import SwiftUI

/// The "larger card" shown when a headline is tapped: the same information
/// at reading size, plus the snippet when the source provided one. "Read
/// Full Article" is the drill-down to the actual website, via an in-app
/// Safari sheet rather than backgrounding the app.
struct ArticleDetailView: View {
    let article: Article

    @State private var showSafari = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    AsyncImage(url: article.imageURL) { phase in
                        if let image = phase.image {
                            image.resizable().aspectRatio(contentMode: .fill)
                        } else {
                            ClassicTheme.rule
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 220, maxHeight: 220)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                    Text(article.category.uppercased())
                        .font(ClassicTheme.kicker())
                        .foregroundStyle(ClassicTheme.ink)

                    Text(article.title)
                        .font(ClassicTheme.headline(24))
                        .foregroundStyle(ClassicTheme.ink)

                    Text(article.source)
                        .font(.caption)
                        .foregroundStyle(ClassicTheme.inkSecondary)

                    if let snippet = article.snippet, !snippet.isEmpty {
                        Divider().background(ClassicTheme.rule)
                        Text(snippet)
                            .font(ClassicTheme.body())
                            .foregroundStyle(ClassicTheme.ink)
                            .lineSpacing(4)
                    }

                    Button {
                        showSafari = true
                    } label: {
                        Label("Read Full Article", systemImage: "safari")
                            .foregroundStyle(ClassicTheme.background)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(ClassicTheme.ink)
                    .padding(.top, 8)
                }
                // Defensive: guarantees the card can never grow wider than
                // the screen regardless of any child's ideal size (the
                // AsyncImage above being the one that actually did).
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .glassEffect(.regular, in: RoundedRectangle(cornerRadius: 26, style: .continuous))
                .padding()
            }
            .background(GradientBackgroundView())
        }
        .sheet(isPresented: $showSafari) {
            SafariView(url: article.url)
                .ignoresSafeArea()
        }
    }
}
