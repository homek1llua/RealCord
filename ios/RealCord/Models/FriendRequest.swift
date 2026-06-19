import Foundation
import FirebaseFirestore

struct FriendRequest: Codable, Identifiable, Equatable {
    @DocumentID var id: String?
    var fromId: String
    var toId: String
    var status: String
    var createdAt: Timestamp?

    enum CodingKeys: String, CodingKey {
        case fromId, toId, status, createdAt
    }
}
