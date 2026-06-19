import Foundation
import FirebaseFirestore

class FriendService: ObservableObject {
    private let fb = FirebaseManager.shared

    func sendFriendRequest(fromId: String, toId: String) async throws {
        let request: [String: Any] = [
            "fromId": fromId,
            "toId": toId,
            "status": "pending",
            "createdAt": Timestamp(date: Date())
        ]
        try await fb.friendRequestsCollection.addDocument(data: request)
    }

    func acceptFriendRequest(requestId: String, userId: String, friendId: String) async throws {
        try await fb.friendRequestsCollection.document(requestId).updateData(["status": "accepted"])

        let friend1: [String: Any] = [
            "userId": userId,
            "friendId": friendId,
            "addedAt": Timestamp(date: Date())
        ]
        let friend2: [String: Any] = [
            "userId": friendId,
            "friendId": userId,
            "addedAt": Timestamp(date: Date())
        ]
        try await fb.friendsCollection.addDocument(data: friend1)
        try await fb.friendsCollection.addDocument(data: friend2)
    }

    func rejectFriendRequest(requestId: String) async throws {
        try await fb.friendRequestsCollection.document(requestId).updateData(["status": "rejected"])
    }

    func removeFriend(userId: String, friendId: String) async throws {
        let snapshot1 = try await fb.friendsCollection
            .whereField("userId", isEqualTo: userId)
            .whereField("friendId", isEqualTo: friendId)
            .getDocuments()

        let snapshot2 = try await fb.friendsCollection
            .whereField("userId", isEqualTo: friendId)
            .whereField("friendId", isEqualTo: userId)
            .getDocuments()

        for doc in snapshot1.documents { try await doc.reference.delete() }
        for doc in snapshot2.documents { try await doc.reference.delete() }
    }

    func observeFriends(userId: String, onChange: @escaping ([Friend]) -> Void) -> ListenerRegistration {
        return fb.friendsCollection
            .whereField("userId", isEqualTo: userId)
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                let friends = docs.compactMap { try? $0.data(as: Friend.self) }
                onChange(friends)
            }
    }

    func observeFriendRequests(userId: String, onChange: @escaping ([FriendRequest]) -> Void) -> ListenerRegistration {
        return fb.friendRequestsCollection
            .whereField("toId", isEqualTo: userId)
            .whereField("status", isEqualTo: "pending")
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                let requests = docs.compactMap { try? $0.data(as: FriendRequest.self) }
                onChange(requests)
            }
    }

    func fetchFriendIds(userId: String) async throws -> [String] {
        let snapshot = try await fb.friendsCollection
            .whereField("userId", isEqualTo: userId)
            .getDocuments()
        return snapshot.documents.compactMap {
            try? $0.data(as: Friend.self)
        }.map { $0.friendId }
    }

    func isFriend(userId: String, otherId: String) async throws -> Bool {
        let snapshot = try await fb.friendsCollection
            .whereField("userId", isEqualTo: userId)
            .whereField("friendId", isEqualTo: otherId)
            .getDocuments()
        return !snapshot.documents.isEmpty
    }
}
