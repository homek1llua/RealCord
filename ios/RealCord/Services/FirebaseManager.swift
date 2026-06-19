import Foundation
import FirebaseFirestore
import FirebaseAuth
import FirebaseStorage

class FirebaseManager: ObservableObject {
    static let shared = FirebaseManager()

    let db = Firestore.firestore()
    let storage = Storage.storage().reference()

    var usersCollection: CollectionReference { db.collection("users") }
    var messagesCollection: CollectionReference { db.collection("messages") }
    var friendsCollection: CollectionReference { db.collection("friends") }
    var friendRequestsCollection: CollectionReference { db.collection("friendRequests") }
    var serversCollection: CollectionReference { db.collection("servers") }
    var channelsCollection: CollectionReference { db.collection("channels") }
    var callsCollection: CollectionReference { db.collection("calls") }
    var updatesCollection: CollectionReference { db.collection("updates") }

    func dmChannelId(for uid1: String, uid2: String) -> String {
        let sorted = [uid1, uid2].sorted()
        return "dm_\(sorted[0])_\(sorted[1])"
    }

    func generateInviteCode() -> String {
        let letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<8).map { _ in letters.randomElement()! })
    }

    func incrementUnreadCount(for channelId: String, in userId: String) {
        let userRef = usersCollection.document(userId)
        userRef.updateData([
            "unreadCounts.\(channelId)": FieldValue.increment(Int64(1))
        ])
    }

    func resetUnreadCount(for channelId: String, in userId: String) {
        let userRef = usersCollection.document(userId)
        userRef.updateData([
            "unreadCounts.\(channelId)": 0
        ])
    }

    func updateStatus(_ status: String, for userId: String) {
        usersCollection.document(userId).updateData([
            "status": status,
            "lastSeen": Date().timeIntervalSince1970 * 1000
        ])
    }

    func updateTyping(for channelId: String, userId: String) {
        usersCollection.document(userId).updateData([
            "typing.\(channelId)": Timestamp(date: Date())
        ])
    }

    func clearTyping(for channelId: String, userId: String) {
        usersCollection.document(userId).updateData([
            "typing.\(channelId)": FieldValue.delete()
        ])
    }

    func avatarURL(for uid: String) async -> URL? {
        let ref = storage.child("avatars/\(uid).jpg")
        return try? await ref.downloadURL()
    }

    func uploadAvatar(data: Data, for uid: String) async throws -> URL {
        let ref = storage.child("avatars/\(uid).jpg")
        _ = try await ref.putDataAsync(data)
        return try await ref.downloadURL()
    }

    func uploadAttachment(data: Data, mimeType: String) async throws -> URL {
        let ext: String
        if mimeType.contains("gif") { ext = "gif" }
        else if mimeType.contains("png") { ext = "png" }
        else { ext = "jpg" }
        let filename = "img_\(Int(Date().timeIntervalSince1970)).\(ext)"
        let ref = storage.child("attachments/\(filename)")
        _ = try await ref.putDataAsync(data)
        return try await ref.downloadURL()
    }

    // Color utils matching Android
    static func hexColor(_ hex: String?) -> UInt? {
        guard let hex = hex, hex.hasPrefix("#") else { return nil }
        let hexStr = String(hex.dropFirst())
        guard hexStr.count == 6, let value = UInt(hexStr, radix: 16) else { return nil }
        return value
    }
}
