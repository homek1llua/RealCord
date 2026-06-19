import Foundation
import FirebaseFirestore

class MessageService: ObservableObject {
    private let fb = FirebaseManager.shared

    func sendMessage(channelId: String, senderId: String, senderName: String,
                     senderAvatar: String?, content: String, imageUrl: String? = nil,
                     replyToId: String? = nil, mentions: [String]? = nil) async throws {
        let message: [String: Any] = [
            "channelId": channelId,
            "senderId": senderId,
            "senderName": senderName,
            "senderAvatar": senderAvatar as Any,
            "content": content,
            "imageUrl": imageUrl as Any,
            "createdAt": Timestamp(date: Date())
        ]
        var data = message
        if let replyToId = replyToId { data["replyToId"] = replyToId }
        if let mentions = mentions { data["mentions"] = mentions }

        try await fb.messagesCollection.addDocument(data: data)
    }

    func observeMessages(channelId: String, onChange: @escaping ([Message]) -> Void) -> ListenerRegistration {
        return fb.messagesCollection
            .whereField("channelId", isEqualTo: channelId)
            .order(by: "createdAt", descending: false)
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                let messages = docs.compactMap { try? $0.data(as: Message.self) }
                onChange(messages)
            }
    }

    func addReaction(messageId: String, emoji: String, userId: String) async throws {
        let msgRef = fb.messagesCollection.document(messageId)
        try await msgRef.updateData([
            "reactions.\(emoji)": FieldValue.increment(Int64(1)),
            "userReactions.\(userId)": emoji
        ])
    }

    func removeReaction(messageId: String, emoji: String, userId: String) async throws {
        let msgRef = fb.messagesCollection.document(messageId)
        try await msgRef.updateData([
            "reactions.\(emoji)": FieldValue.increment(Int64(-1)),
            "userReactions.\(userId)": FieldValue.delete()
        ])
    }

    func editMessage(messageId: String, newContent: String) async throws {
        try await fb.messagesCollection.document(messageId).updateData([
            "content": newContent,
            "editedAt": Timestamp(date: Date())
        ])
    }

    func deleteMessage(messageId: String) async throws {
        try await fb.messagesCollection.document(messageId).delete()
    }

    func sendGIF(channelId: String, senderId: String, senderName: String,
                 senderAvatar: String?, gifUrl: String) async throws {
        let message: [String: Any] = [
            "channelId": channelId,
            "senderId": senderId,
            "senderName": senderName,
            "senderAvatar": senderAvatar as Any,
            "content": "",
            "imageUrl": gifUrl,
            "createdAt": Timestamp(date: Date())
        ]
        try await fb.messagesCollection.addDocument(data: message)
    }
}
