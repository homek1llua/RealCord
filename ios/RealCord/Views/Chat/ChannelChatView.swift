import SwiftUI

struct ChannelChatView: View {
    @EnvironmentObject var authService: AuthService
    let channel: Channel

    @State private var server: Server?
    @State private var memberUsers: [String] = []

    var body: some View {
        ChatView(
            channelId: channel.id ?? "",
            otherUserName: channel.name,
            showCallButtons: false,
            showMentions: true,
            serverMembers: memberUsers.isEmpty ? nil : memberUsers
        )
        .environmentObject(authService)
        .navigationTitle("#\(channel.name)")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            loadServerMembers()
        }
    }

    private func loadServerMembers() {
        Task {
            let serverService = ServerService()
            let userService = UserService()
            if let s = try? await serverService.fetchServer(serverId: channel.serverId) {
                await MainActor.run { server = s }
                // Fetch member usernames for mention picker
                self.memberUsers = s.memberIds
            }
        }
    }
}
