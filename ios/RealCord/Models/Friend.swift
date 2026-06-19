import Foundation
import FirebaseFirestore

struct Friend: Codable, Identifiable, Equatable {
    @DocumentID var id: String?
    var userId: String
    var friendId: String
    var addedAt: Timestamp?

    enum CodingKeys: String, CodingKey {
        case userId, friendId, addedAt
    }
}
