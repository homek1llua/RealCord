import Foundation
import FirebaseFirestore

struct Message: Codable, Identifiable, Equatable {
    @DocumentID var id: String?
    var channelId: String
    var senderId: String
    var senderName: String
    var senderAvatar: String?
    var content: String
    var imageUrl: String?
    var fileUrl: String?
    var fileName: String?
    var createdAt: Timestamp?
    var editedAt: Timestamp?
    var replyToId: String?
    var mentions: [String]?
    var reactions: [String: Int]?
    var userReactions: [String: String]?
    var readAt: Timestamp?

    enum CodingKeys: String, CodingKey {
        case channelId, senderId, senderName, senderAvatar, content
        case imageUrl, fileUrl, fileName, createdAt, editedAt
        case replyToId, mentions, reactions, userReactions, readAt
    }

    var isSent: Bool = false
    var isRead: Bool = false

    mutating func setLocalFlags(isSent: Bool, isRead: Bool) {
        self.isSent = isSent
        self.isRead = isRead
    }
}
