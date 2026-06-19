import SwiftUI
import FirebaseFirestore
import Kingfisher

struct FriendsListView: View {
    @EnvironmentObject var authService: AuthService
    @StateObject private var friendService = FriendService()
    @StateObject private var userService = UserService()
    @State private var friends: [User] = []
    @State private var pendingRequests: [FriendRequest] = []
    @State private var requestUsers: [String: User] = [:]
    @State private var unreadCounts: [String: Int] = [:]
    @State private var showAddFriend = false

    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "#313338").ignoresSafeArea()

                VStack(alignment: .leading, spacing: 0) {
                    // Online count header
                    HStack {
                        Text("Friends")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)

                        Spacer()

                        Button(action: { showAddFriend = true }) {
                            Image(systemName: "person.badge.plus")
                                .font(.title3)
                                .foregroundColor(Color(hex: "#5865F2"))
                        }
                    }
                    .padding()

                    // Pending requests
                    if !pendingRequests.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("PENDING (\(pendingRequests.count))")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                                .padding(.horizontal)

                            ForEach(pendingRequests, id: \.id) { request in
                                HStack {
                                    if let user = requestUsers[request.fromId] {
                                        AvatarCircle(user: user, size: 32)
                                        Text(user.username)
                                            .foregroundColor(.white)
                                    }
                                    Spacer()
                                    Button(action: {
                                        Task {
                                            try? await friendService.acceptFriendRequest(
                                                requestId: request.id ?? "",
                                                userId: authService.currentUser?.uid ?? "",
                                                friendId: request.fromId
                                            )
                                        }
                                    }) {
                                        Text("Accept")
                                            .font(.caption)
                                            .fontWeight(.semibold)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(Color(hex: "#5865F2"))
                                            .foregroundColor(.white)
                                            .cornerRadius(4)
                                    }
                                    Button(action: {
                                        Task {
                                            try? await friendService.rejectFriendRequest(requestId: request.id ?? "")
                                        }
                                    }) {
                                        Text("Ignore")
                                            .font(.caption)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(Color(hex: "#ED4245"))
                                            .foregroundColor(.white)
                                            .cornerRadius(4)
                                    }
                                }
                                .padding(.horizontal)
                                .padding(.vertical, 4)
                            }
                            Divider()
                                .background(Color(hex: "#1E1F22"))
                                .padding(.vertical, 4)
                        }
                    }

                    // Friend list
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(friends) { friend in
                                NavigationLink(destination: DMChatView(otherUser: friend)
                                    .environmentObject(authService)) {
                                    FriendRowView(
                                        user: friend,
                                        unreadCount: unreadCounts[FirebaseManager.shared.dmChannelId(
                                            for: authService.currentUser?.uid ?? "",
                                            uid2: friend.uid
                                        )] ?? 0
                                    )
                                }
                                Divider()
                                    .background(Color(hex: "#1E1F22"))
                                    .padding(.leading, 60)
                            }
                        }
                    }
                }
            }
            .sheet(isPresented: $showAddFriend) {
                AddFriendView()
                    .environmentObject(authService)
            }
            .onAppear {
                guard let uid = authService.currentUser?.uid else { return }
                observeData(uid: uid)
            }
        }
        .navigationViewStyle(.stack)
    }

    private func observeData(uid: String) {
        friendService.observeFriends(userId: uid) { [weak self] friendList in
            guard let self = self else { return }
            let uids = friendList.map { $0.friendId }
            Task {
                var users: [User] = []
                for friendUid in uids {
                    if let user = try? await self.userService.fetchUser(uid: friendUid) {
                        users.append(user)
                    }
                }
                await MainActor.run {
                    self.friends = users.sorted { $0.statusSortOrder < $1.statusSortOrder }
                }
            }
        }

        friendService.observeFriendRequests(userId: uid) { [weak self] requests in
            self?.pendingRequests = requests
            Task {
                for request in requests {
                    if let user = try? await self?.userService.fetchUser(uid: request.fromId) {
                        await MainActor.run {
                            self?.requestUsers[request.fromId] = user
                        }
                    }
                }
            }
        }

        userService.observeUser(uid: uid) { [weak self] user in
            self?.unreadCounts = user?.unreadCounts ?? [:]
        }
    }
}

private extension User {
    var statusSortOrder: Int {
        switch status {
        case "online": return 0
        case "idle": return 1
        case "dnd": return 2
        default: return 3
        }
    }
}

struct FriendRowView: View {
    let user: User
    let unreadCount: Int

    var body: some View {
        HStack(spacing: 12) {
            AvatarCircle(user: user, size: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(user.username)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                Text(user.status.capitalized)
                    .font(.caption)
                    .foregroundColor(Color(hex: user.statusColor()))
            }

            Spacer()

            if unreadCount > 0 {
                Text("\(unreadCount)")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(6)
                    .background(Color(hex: "#ED4245"))
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}

struct AvatarCircle: View {
    let user: User
    let size: CGFloat

    var body: some View {
        KFImage(URL(string: user.avatarUrl ?? ""))
            .placeholder {
                Circle()
                    .fill(Color(hex: user.avatarColor))
                    .overlay(
                        Text(user.avatarText)
                            .font(.system(size: size * 0.4))
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    )
            }
            .resizable()
            .frame(width: size, height: size)
            .clipShape(Circle())
            .overlay(
                Circle()
                    .stroke(Color(hex: user.statusColor()), lineWidth: 2)
                    .frame(width: size + 4, height: size + 4)
            )
    }
}
