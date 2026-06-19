import Foundation
import FirebaseFirestore

struct User: Codable, Identifiable, Equatable {
    @DocumentID var id: String?
    var uid: String
    var username: String
    var email: String
    var avatarUrl: String?
    var avatarColor: String
    var avatarText: String
    var status: String
    var bio: String
    var createdAt: Timestamp?
    var lastSeen: Double?
    var fcmToken: String?
    var nitroExpiresAt: Timestamp?
    var nitroTier: String?
    var usernameColor: String?
    var usernameFont: String?
    var usernameGradientStart: String?
    var usernameGradientEnd: String?
    var chatColor: String?
    var chatGradientStart: String?
    var chatGradientEnd: String?
    var profileBackgroundColor: String?
    var profileBackgroundEmoji: String?
    var profileBackgroundEmojiOpacity: Double?
    var profileBannerUrl: String?
    var unreadCounts: [String: Int]?
    var typing: [String: Timestamp]?

    enum CodingKeys: String, CodingKey {
        case uid, username, email, avatarUrl, avatarColor, avatarText, status, bio
        case createdAt, lastSeen, fcmToken, nitroExpiresAt, nitroTier
        case usernameColor, usernameFont, usernameGradientStart, usernameGradientEnd
        case chatColor, chatGradientStart, chatGradientEnd
        case profileBackgroundColor, profileBackgroundEmoji, profileBackgroundEmojiOpacity, profileBannerUrl
        case unreadCounts, typing
    }

    init(uid: String, username: String, email: String, avatarUrl: String? = nil,
         avatarColor: String = "#5865F2", avatarText: String = "",
         status: String = "offline", bio: String = "",
         createdAt: Timestamp? = nil, lastSeen: Double? = nil,
         fcmToken: String? = nil, nitroExpiresAt: Timestamp? = nil, nitroTier: String? = nil,
         usernameColor: String? = nil, usernameFont: String? = nil,
         usernameGradientStart: String? = nil, usernameGradientEnd: String? = nil,
         chatColor: String? = nil, chatGradientStart: String? = nil, chatGradientEnd: String? = nil,
         profileBackgroundColor: String? = nil, profileBackgroundEmoji: String? = nil,
         profileBackgroundEmojiOpacity: Double? = nil, profileBannerUrl: String? = nil,
         unreadCounts: [String: Int]? = nil, typing: [String: Timestamp]? = nil) {
        self.uid = uid
        self.username = username
        self.email = email
        self.avatarUrl = avatarUrl
        self.avatarColor = avatarColor
        self.avatarText = avatarText
        self.status = status
        self.bio = bio
        self.createdAt = createdAt
        self.lastSeen = lastSeen
        self.fcmToken = fcmToken
        self.nitroExpiresAt = nitroExpiresAt
        self.nitroTier = nitroTier
        self.usernameColor = usernameColor
        self.usernameFont = usernameFont
        self.usernameGradientStart = usernameGradientStart
        self.usernameGradientEnd = usernameGradientEnd
        self.chatColor = chatColor
        self.chatGradientStart = chatGradientStart
        self.chatGradientEnd = chatGradientEnd
        self.profileBackgroundColor = profileBackgroundColor
        self.profileBackgroundEmoji = profileBackgroundEmoji
        self.profileBackgroundEmojiOpacity = profileBackgroundEmojiOpacity
        self.profileBannerUrl = profileBannerUrl
        self.unreadCounts = unreadCounts
        self.typing = typing
    }

    func hasNitro() -> Bool {
        guard let expiresAt = nitroExpiresAt else { return false }
        return expiresAt.dateValue() > Date()
    }

    func hasPremium() -> Bool {
        return hasNitro() && nitroTier == "premium"
    }

    func statusColor() -> String {
        switch status {
        case "online": return "#23A559"
        case "idle": return "#F0B232"
        case "dnd": return "#ED4245"
        default: return "#80848E"
        }
    }
}
