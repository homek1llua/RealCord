import Foundation
import FirebaseFirestore

struct Channel: Codable, Identifiable, Equatable {
    @DocumentID var id: String?
    var serverId: String
    var name: String
    var type: String
    var position: Int
    var createdAt: Timestamp?

    enum CodingKeys: String, CodingKey {
        case serverId, name, type, position, createdAt
    }

    var isVoice: Bool { type == "voice" }
    var isText: Bool { type == "text" }
}
