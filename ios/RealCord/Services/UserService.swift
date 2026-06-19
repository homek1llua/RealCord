import Foundation
import FirebaseFirestore
import FirebaseStorage
import UIKit

class UserService: ObservableObject {
    private let fb = FirebaseManager.shared

    func updateProfile(userId: String, data: [String: Any]) async throws {
        try await fb.usersCollection.document(userId).updateData(data)
    }

    func updateFullProfile(userId: String, username: String, email: String,
                           avatarUrl: String?, avatarColor: String, avatarText: String,
                           status: String, bio: String,
                           usernameColor: String?, usernameFont: String?,
                           usernameGradientStart: String?, usernameGradientEnd: String?,
                           chatColor: String?, chatGradientStart: String?, chatGradientEnd: String?,
                           profileBackgroundColor: String?, profileBackgroundEmoji: String?,
                           profileBackgroundEmojiOpacity: Double?, profileBannerUrl: String?) async throws {
        var data: [String: Any] = [
            "username": username,
            "email": email,
            "avatarColor": avatarColor,
            "avatarText": avatarText,
            "status": status,
            "bio": bio
        ]
        if let avatarUrl = avatarUrl { data["avatarUrl"] = avatarUrl }
        if let usernameColor = usernameColor { data["usernameColor"] = usernameColor }
        if let usernameFont = usernameFont { data["usernameFont"] = usernameFont }
        if let usernameGradientStart = usernameGradientStart { data["usernameGradientStart"] = usernameGradientStart }
        if let usernameGradientEnd = usernameGradientEnd { data["usernameGradientEnd"] = usernameGradientEnd }
        if let chatColor = chatColor { data["chatColor"] = chatColor }
        if let chatGradientStart = chatGradientStart { data["chatGradientStart"] = chatGradientStart }
        if let chatGradientEnd = chatGradientEnd { data["chatGradientEnd"] = chatGradientEnd }
        if let profileBackgroundColor = profileBackgroundColor { data["profileBackgroundColor"] = profileBackgroundColor }
        if let profileBackgroundEmoji = profileBackgroundEmoji { data["profileBackgroundEmoji"] = profileBackgroundEmoji }
        if let profileBackgroundEmojiOpacity = profileBackgroundEmojiOpacity { data["profileBackgroundEmojiOpacity"] = profileBackgroundEmojiOpacity }
        if let profileBannerUrl = profileBannerUrl { data["profileBannerUrl"] = profileBannerUrl }

        try await fb.usersCollection.document(userId).updateData(data)
    }

    func searchUsers(query: String) async throws -> [User] {
        let snapshot = try await fb.usersCollection
            .whereField("username", isGreaterThanOrEqualTo: query)
            .whereField("username", isLessThanOrEqualTo: query + "\u{f8ff}")
            .getDocuments()
        return snapshot.documents.compactMap { try? $0.data(as: User.self) }
    }

    func fetchUser(uid: String) async throws -> User? {
        let doc = try await fb.usersCollection.document(uid).getDocument()
        return try? doc.data(as: User.self)
    }

    func uploadAvatar(imageData: Data, userId: String) async throws -> String {
        let url = try await fb.uploadAvatar(data: imageData, for: userId)
        try await fb.usersCollection.document(userId).updateData(["avatarUrl": url.absoluteString])
        return url.absoluteString
    }

    func observeUser(uid: String, onChange: @escaping (User?) -> Void) -> ListenerRegistration {
        return fb.usersCollection.document(uid).addSnapshotListener { snapshot, _ in
            let user = try? snapshot?.data(as: User.self)
            onChange(user)
        }
    }

    func saveFCMToken(uid: String, token: String) {
        fb.usersCollection.document(uid).updateData(["fcmToken": token])
    }
}
