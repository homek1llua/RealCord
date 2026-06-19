import SwiftUI

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r, g, b, a: UInt64
        switch hex.count {
        case 6:
            (r, g, b, a) = ((int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF, 255)
        case 8:
            (r, g, b, a) = ((int >> 24) & 0xFF, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            (r, g, b, a) = (88, 101, 242, 255)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }

    static let discordBackground = Color(hex: "#313338")
    static let discordSurface = Color(hex: "#2B2D31")
    static let discordDark = Color(hex: "#1E1F22")
    static let discordPrimary = Color(hex: "#5865F2")
    static let discordOnline = Color(hex: "#23A559")
    static let discordIdle = Color(hex: "#F0B232")
    static let discordDND = Color(hex: "#ED4245")
    static let discordOffline = Color(hex: "#80848E")
    static let discordMention = Color(hex: "#7983F5")
}
