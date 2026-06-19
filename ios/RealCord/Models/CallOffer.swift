import Foundation
import FirebaseFirestore

struct CallOffer: Codable, Identifiable, Equatable {
    @DocumentID var id: String?
    var callerId: String
    var callerName: String
    var calleeId: String
    var channelId: String?
    var type: String
    var status: String
    var sdp: String?
    var answerSdp: String?
    var iceCandidates: [String: String]?
    var createdAt: Timestamp?

    enum CodingKeys: String, CodingKey {
        case callerId, callerName, calleeId, channelId, type, status
        case sdp, answerSdp, iceCandidates, createdAt
    }
}
