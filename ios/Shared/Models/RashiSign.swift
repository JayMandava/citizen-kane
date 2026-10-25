import Foundation

/// One of the 12 rashis (moon signs), with its Eenadu URL slug.
enum RashiSign: String, CaseIterable, Identifiable, Codable {
    case mesh, vrushabha, mithun, kark, singh, kanya
    case tula, vrushchik, dhanu, makar, kumbh, meen

    var id: String { rawValue }

    /// Eenadu's slug for this sign's page, e.g. "makar-rashi-today".
    var slug: String { "\(rawValue)-rashi-today" }

    var teluguName: String {
        switch self {
        case .mesh: "మేషం"
        case .vrushabha: "వృషభం"
        case .mithun: "మిథునం"
        case .kark: "కర్కాటకం"
        case .singh: "సింహం"
        case .kanya: "కన్య"
        case .tula: "తుల"
        case .vrushchik: "వృశ్చికం"
        case .dhanu: "ధనుస్సు"
        case .makar: "మకరం"
        case .kumbh: "కుంభం"
        case .meen: "మీనం"
        }
    }

    var englishName: String {
        switch self {
        case .mesh: "Aries"
        case .vrushabha: "Taurus"
        case .mithun: "Gemini"
        case .kark: "Cancer"
        case .singh: "Leo"
        case .kanya: "Virgo"
        case .tula: "Libra"
        case .vrushchik: "Scorpio"
        case .dhanu: "Sagittarius"
        case .makar: "Capricorn"
        case .kumbh: "Aquarius"
        case .meen: "Pisces"
        }
    }

    var displayName: String { "\(teluguName) (\(englishName))" }
}
