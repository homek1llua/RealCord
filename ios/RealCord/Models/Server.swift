import Foundation
import FirebaseFirestore

struct Server: Codable, Identifiable, Equatable {
    @DocumentID var id: String?
    var name: String
    var ownerId: String
    var iconUrl: String?
    var inviteCode: String
    var memberIds: [String]
    var createdAt: Timestamp?

    enum CodingKeys: String, CodingKey {
        case name, ownerId, iconUrl, inviteCode, memberIds, createdAt
    }
}
