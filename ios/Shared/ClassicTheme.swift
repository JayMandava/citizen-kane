import SwiftUI
import UIKit

/// A plain black-and-white theme: white text on a dark background in dark
/// mode, black text on white in light mode, no accent color anywhere.
/// Headlines and body text use NTR (bundled in `CitizenKane/Fonts`), a
/// Telugu-rendering typeface — unlike a Latin serif design, it actually
/// changes how Telugu text looks, not just the English chrome around it.
enum ClassicTheme {
    private static func adaptive(light: UIColor, dark: UIColor) -> Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark ? dark : light
        })
    }

    static var background: Color {
        adaptive(light: .white, dark: .black)
    }

    /// The far end of the backdrop gradient — a few percent off `background`,
    /// not a second hue. A perfectly flat color gives Liquid Glass nothing to
    /// visibly differentiate against as it scrolls past; this stays close
    /// enough to read as "solid" while still giving the material something
    /// to show.
    static var backgroundGradientEnd: Color {
        adaptive(
            light: UIColor(white: 0.91, alpha: 1),
            dark: UIColor(white: 0.10, alpha: 1)
        )
    }

    static var ink: Color {
        adaptive(light: .black, dark: .white)
    }

    static var inkSecondary: Color {
        adaptive(
            light: UIColor.black.withAlphaComponent(0.55),
            dark: UIColor.white.withAlphaComponent(0.55)
        )
    }

    static var rule: Color {
        adaptive(
            light: UIColor.black.withAlphaComponent(0.15),
            dark: UIColor.white.withAlphaComponent(0.15)
        )
    }

    private static let fontName = "NTR"

    static func headline(_ size: CGFloat) -> Font {
        .custom(fontName, size: size)
    }

    static func body(_ size: CGFloat = 17) -> Font {
        .custom(fontName, size: size)
    }

    static func kicker(_ size: CGFloat = 12) -> Font {
        .custom(fontName, size: size)
    }
}
