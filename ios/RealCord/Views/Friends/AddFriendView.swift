import SwiftUI

struct AddFriendView: View {
    @EnvironmentObject var authService: AuthService
    @Environment(\.dismiss) var dismiss
    @StateObject private var userService = UserService()
    @StateObject private var friendService = FriendService()
    @State private var searchQuery = ""
    @State private var searchResults: [User] = []
    @State private var isSearching = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "#313338").ignoresSafeArea()

                VStack(spacing: 0) {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(Color(hex: "#80848E"))
                        TextField("Search users by username", text: $searchQuery)
                            .foregroundColor(.white)
                            .onSubmit(search)
                    }
                    .padding(12)
                    .background(Color(hex: "#1E1F22"))
                    .cornerRadius(8)
                    .padding()

                    if let error = errorMessage {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(Color(hex: "#ED4245"))
                            .padding(.bottom)
                    }

                    if isSearching {
                        Spacer()
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        Spacer()
                    } else {
                        List(searchResults) { user in
                            HStack {
                                AvatarCircle(user: user, size: 40)
                                VStack(alignment: .leading) {
                                    Text(user.username)
                                        .foregroundColor(.white)
                                        .fontWeight(.semibold)
                                    Text(user.status.capitalized)
                                        .font(.caption)
                                        .foregroundColor(Color(hex: user.statusColor()))
                                }

                                Spacer()

                                Button(action: { sendRequest(to: user.uid) }) {
                                    Text("Add Friend")
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(Color(hex: "#5865F2"))
                                        .foregroundColor(.white)
                                        .cornerRadius(4)
                                }
                            }
                            .listRowBackground(Color(hex: "#2B2D31"))
                        }
                        .listStyle(.plain)
                    }
                }
            }
            .navigationTitle("Add Friend")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundColor(Color(hex: "#80848E"))
                }
            }
        }
    }

    private func search() {
        guard !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        isSearching = true
        errorMessage = nil
        Task {
            do {
                let results = try await userService.searchUsers(query: searchQuery)
                let filtered = results.filter { $0.uid != authService.currentUser?.uid }
                await MainActor.run {
                    self.searchResults = filtered
                    self.isSearching = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.isSearching = false
                }
            }
        }
    }

    private func sendRequest(to uid: String) {
        guard let fromId = authService.currentUser?.uid else { return }
        Task {
            do {
                try await friendService.sendFriendRequest(fromId: fromId, toId: uid)
                await MainActor.run {
                    self.errorMessage = "Friend request sent!"
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
}
