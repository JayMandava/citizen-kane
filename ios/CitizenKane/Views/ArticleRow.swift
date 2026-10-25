import SwiftUI

/// A single headline, rendered as a floating glass card (kicker, headline,
/// byline, thumbnail) — the monochrome palette drops color, not the glass
/// material itself. The card's own material provides the depth; it doesn't
/// need a busy background behind it to read as glass.
struct ArticleRow: View {
    let article: Article

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(article.category.uppercased())
                    .font(ClassicTheme.kicker())
                    .foregroundStyle(ClassicTheme.ink)
                Text(article.title)
                    .font(ClassicTheme.headline(18))
                    .foregroundStyle(ClassicTheme.ink)
                    .lineLimit(4)
                    .fixedSize(horizontal: false, vertical: true)
                Text(article.source)
                    .font(.caption)
                    .foregroundStyle(ClassicTheme.inkSecondary)
            }

            Spacer(minLength: 0)

            AsyncImage(url: article.imageURL) { phase in
                if let image = phase.image {
                    image.resizable().aspectRatio(contentMode: .fill)
                } else {
                    ClassicTheme.rule
                }
            }
            .frame(width: 72, height: 72)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .padding(14)
        .glassEffect(.regular, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}
