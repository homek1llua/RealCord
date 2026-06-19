import Foundation
import FirebaseFirestore

class ServerService: ObservableObject {
    private let fb = FirebaseManager.shared

    func createServer(name: String, ownerId: String) async throws -> String? {
        let inviteCode = fb.generateInviteCode()
        let server: [String: Any] = [
            "name": name,
            "ownerId": ownerId,
            "inviteCode": inviteCode,
            "memberIds": [ownerId],
            "createdAt": Timestamp(date: Date())
        ]
        let ref = try await fb.serversCollection.addDocument(data: server)

        let generalChannel: [String: Any] = [
            "serverId": ref.documentID,
            "name": "general",
            "type": "text",
            "position": 0,
            "createdAt": Timestamp(date: Date())
        ]
        try await fb.channelsCollection.addDocument(data: generalChannel)

        return ref.documentID
    }

    func joinServer(inviteCode: String, userId: String) async throws -> String? {
        let snapshot = try await fb.serversCollection
            .whereField("inviteCode", isEqualTo: inviteCode.uppercased())
            .getDocuments()

        guard let doc = snapshot.documents.first else { return nil }
        let serverId = doc.documentID
        try await doc.reference.updateData([
            "memberIds": FieldValue.arrayUnion([userId])
        ])
        return serverId
    }

    func deleteServer(serverId: String) async throws {
        let channels = try await fb.channelsCollection
            .whereField("serverId", isEqualTo: serverId)
            .getDocuments()
        for channel in channels.documents {
            try await channel.reference.delete()
        }
        try await fb.serversCollection.document(serverId).delete()
    }

    func createChannel(serverId: String, name: String, type: String, position: Int) async throws {
        let channel: [String: Any] = [
            "serverId": serverId,
            "name": name,
            "type": type,
            "position": position,
            "createdAt": Timestamp(date: Date())
        ]
        try await fb.channelsCollection.addDocument(data: channel)
    }

    func deleteChannel(channelId: String) async throws {
        try await fb.channelsCollection.document(channelId).delete()
    }

    func observeServers(memberId: String, onChange: @escaping ([Server]) -> Void) -> ListenerRegistration {
        return fb.serversCollection
            .whereField("memberIds", arrayContains: memberId)
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                let servers = docs.compactMap { try? $0.data(as: Server.self) }
                onChange(servers)
            }
    }

    func observeChannels(serverId: String, onChange: @escaping ([Channel]) -> Void) -> ListenerRegistration {
        return fb.channelsCollection
            .whereField("serverId", isEqualTo: serverId)
            .order(by: "position")
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                let channels = docs.compactMap { try? $0.data(as: Channel.self) }
                onChange(channels)
            }
    }

    func fetchServer(serverId: String) async throws -> Server? {
        let doc = try await fb.serversCollection.document(serverId).getDocument()
        return try? doc.data(as: Server.self)
    }
}
