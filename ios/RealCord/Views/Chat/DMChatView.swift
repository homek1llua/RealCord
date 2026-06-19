import SwiftUI

struct DMChatView: View {
    @EnvironmentObject var authService: AuthService
    let otherUser: User

    var channelId: String {
        FirebaseManager.shared.dmChannelId(
            for: authService.currentUser?.uid ?? "",
            uid2: otherUser.uid
        )
    }

    var body: some View {
        ChatView(
            channelId: channelId,
            otherUserName: otherUser.username,
            showCallButtons: true,
            showMentions: false,
            serverMembers: nil
        )
        .environmentObject(authService)
        .navigationTitle(otherUser.username)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                HStack(spacing: 12) {
                    Circle()
                        .fill(Color(hex: otherUser.statusColor()))
                        .frame(width: 10, height: 10)
                    Text(otherUser.status.capitalized)
                        .font(.caption)
                        .foregroundColor(Color(hex: otherUser.statusColor()))
                }
            }
        }
    }
}
