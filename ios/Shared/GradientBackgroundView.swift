import SwiftUI

/// The scrolling-list backdrop: a subtle top-to-bottom monochrome gradient,
/// not a flat single color and not an illustration. Liquid Glass needs some
/// variation behind it to visibly read as glass while scrolling — a dead
/// flat color gives it nothing to show against — but the shift here is only
/// a few percent of luminance, so it still reads as "solid" at a glance.
struct GradientBackgroundView: View {
    var body: some View {
        LinearGradient(
            colors: [ClassicTheme.background, ClassicTheme.backgroundGradientEnd],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}
