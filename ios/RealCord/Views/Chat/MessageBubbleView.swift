import SwiftUI
import Kingfisher

struct MessageBubbleView: View {
    let message: Message
    let isSent: Bool
    let currentUserId: String
    let senderUser: User?
    let onReply: () -> Void
    let onReact: (String) -> Void

    @State private var showActions = false
    @State private var showProfile = false

    private let reactionEmojis = ["😊", "😂", "❤️", "👍", "🎉", "😢"]

    var body: some View {
        VStack(alignment: isSent ? .trailing : .leading, spacing: 2) {
            // Reply preview
            if let replyToId = message.replyToId {
                // Inline reply indicator (simplified)
                HStack {
                    Image(systemName: "arrow.turn.up.right")
                        .font(.caption2)
                        .foregroundColor(Color(hex: "#80848E"))
                    Text("Replying to a message")
                        .font(.caption2)
                        .foregroundColor(Color(hex: "#80848E"))
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(Color(hex: "#1E1F22"))
                .cornerRadius(4)
                .padding(.bottom, 2)
            }

            HStack(alignment: .bottom, spacing: 8) {
                if !isSent {
                    // Sender avatar
                    Button(action: { showProfile = true }) {
                        AvatarCircle(user: senderUser ?? User(uid: message.senderId, username: message.senderName, email: "", avatarColor: "#5865F2", avatarText: String(message.senderName.prefix(2)).uppercased()), size: 36)
                    }
                }

                VStack(alignment: isSent ? .trailing : .leading, spacing: 4) {
                    if !isSent {
                        Text(message.senderName)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(Color(hex: "#5865F2"))
                    }

                    // Message bubble
                    VStack(alignment: .leading, spacing: 4) {
                        if !message.content.isEmpty {
                            Text(parseMentions(message.content))
                                .foregroundColor(.white)
                                .font(.body)
                        }

                        if let imageUrl = message.imageUrl, let url = URL(string: imageUrl) {
                            KFImage(url)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(maxWidth: 200, maxHeight: 200)
                                .cornerRadius(8)
                        }

                        // Reactions
                        if let reactions = message.reactions, !reactions.isEmpty {
                            HStack(spacing: 4) {
                                ForEach(Array(reactions.keys.sorted()), id: \.self) { emoji in
                                    if let count = reactions[emoji], count > 0 {
                                        Button(action: { onReact(emoji) }) {
                                            Text("\(emoji) \(count)")
                                                .font(.caption)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(Color(hex: "#1E1F22"))
                                                .cornerRadius(8)
                                                .foregroundColor(.white)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(10)
                    .background(isSent ? Color(hex: "#5865F2") : Color(hex: "#2B2D31"))
                    .cornerRadius(12, corners: isSent ? [.topLeft, .topRight, .bottomLeft] : [.topLeft, .topRight, .bottomRight])

                    // Status & time
                    HStack(spacing: 4) {
                        if let createdAt = message.createdAt {
                            Text(createdAt.dateValue(), style: .time)
                                .font(.caption2)
                                .foregroundColor(Color(hex: "#80848E"))
                        }
                        if isSent {
                            Image(systemName: message.isRead ? "checkmark.message.fill" : "checkmark.message")
                                .font(.caption2)
                                .foregroundColor(message.isRead ? Color(hex: "#5865F2") : Color(hex: "#80848E"))
                        }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 2)
            .contextMenu {
                Button(action: onReply) {
                    Label("Reply", systemImage: "arrow.turn.up.left")
                }
                ForEach(reactionEmojis, id: \.self) { emoji in
                    Button(action: { onReact(emoji) }) {
                        Text(emoji)
                    }
                }
            }
        }
        .sheet(isPresented: $showProfile) {
            ProfileDialogView(
                user: senderUser ?? User(uid: message.senderId, username: message.senderName, email: "", avatarColor: "#5865F2", avatarText: String(message.senderName.prefix(2)).uppercased()),
                currentUserId: currentUserId,
                isFriend: false,
                onUnfriend: nil,
                onClose: { showProfile = false }
            )
        }
    }

    private func parseMentions(_ text: String) -> AttributedString {
        var attr = AttributedString(text)
        for run in attr.runs {
            // Simple @mention highlighting
        }
        return attr
    }
}

extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect,
                                byRoundingCorners: corners,
                                cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
