import SwiftUI
import FirebaseFirestore

struct ChatView: View {
    @EnvironmentObject var authService: AuthService
    @StateObject private var messageService = MessageService()
    @StateObject private var userService = UserService()

    let channelId: String
    let otherUserName: String?
    let showCallButtons: Bool
    let showMentions: Bool
    let serverMembers: [String]?

    @State private var messages: [Message] = []
    @State private var messageText = ""
    @State private var showGifSearch = false
    @State private var replyToMessage: Message?
    @State private var showMentionPicker = false
    @State private var mentionQuery = ""
    @State private var isTyping = false
    @State private var typingUsers: [String] = []
    @State private var listeners: [ListenerRegistration] = []

    var body: some View {
        VStack(spacing: 0) {
            // Typing indicator
            if !typingUsers.isEmpty {
                HStack {
                    Text(typingUsers.joined(separator: ", "))
                        .font(.caption)
                        .foregroundColor(Color(hex: "#80848E"))
                    + Text(typingUsers.count == 1 ? " is typing..." : " are typing...")
                        .font(.caption)
                        .foregroundColor(Color(hex: "#80848E"))
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.vertical, 4)
                .background(Color(hex: "#1E1F22"))
            }

            // Messages
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(messages) { message in
                            let isSent = message.senderId == authService.currentUser?.uid
                            MessageBubbleView(
                                message: message,
                                isSent: isSent,
                                currentUserId: authService.currentUser?.uid ?? "",
                                senderUser: nil,
                                onReply: { replyToMessage = message },
                                onReact: { emoji in toggleReaction(message: message, emoji: emoji) }
                            )
                            .id(message.id)
                        }
                    }
                }
                .onChange(of: messages.count) { _ in
                    if let last = messages.last?.id {
                        withAnimation { proxy.scrollTo(last, anchor: .bottom) }
                    }
                }
            }

            // Reply preview
            if let reply = replyToMessage {
                HStack {
                    Image(systemName: "arrow.turn.up.right")
                        .foregroundColor(Color(hex: "#5865F2"))
                    Text("Replying to \(reply.senderName)")
                        .font(.caption)
                        .foregroundColor(Color(hex: "#80848E"))
                    Spacer()
                    Button(action: { replyToMessage = nil }) {
                        Image(systemName: "xmark")
                            .foregroundColor(Color(hex: "#80848E"))
                    }
                }
                .padding(8)
                .background(Color(hex: "#2B2D31"))
            }

            // Mention picker
            if showMentions && showMentionPicker {
                mentionPickerView
            }

            // Input bar
            HStack(spacing: 8) {
                Button(action: { showGifSearch = true }) {
                    Image(systemName: "gift")
                        .foregroundColor(Color(hex: "#80848E"))
                        .font(.title3)
                }

                TextField("Message \(otherUserName.map { "@\($0)" } ?? "")", text: $messageText)
                    .textFieldStyle(RealCordTextFieldStyle())
                    .onChange(of: messageText) { newValue in
                        if showMentions && newValue.contains("@") {
                            let parts = newValue.split(separator: "@")
                            if parts.count > 1 {
                                mentionQuery = String(parts.last ?? "").trimmingCharacters(in: .whitespaces)
                                showMentionPicker = !mentionQuery.isEmpty
                            }
                        } else {
                            showMentionPicker = false
                        }

                        // Typing indicator
                        if !newValue.isEmpty && !isTyping {
                            isTyping = true
                            FirebaseManager.shared.updateTyping(for: channelId, userId: authService.currentUser?.uid ?? "")
                        } else if newValue.isEmpty && isTyping {
                            isTyping = false
                            FirebaseManager.shared.clearTyping(for: channelId, userId: authService.currentUser?.uid ?? "")
                        }
                    }

                Button(action: sendMessage) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title2)
                        .foregroundColor(messageText.trimmingCharacters(in: .whitespaces).isEmpty
                            ? Color(hex: "#80848E")
                            : Color(hex: "#5865F2"))
                }
                .disabled(messageText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding()
        }
        .background(Color(hex: "#313338"))
        .sheet(isPresented: $showGifSearch) {
            GifSearchView { gifUrl in
                sendGIF(url: gifUrl)
                showGifSearch = false
            }
        }
        .onAppear {
            observeMessages()
            observeTyping()
            FirebaseManager.shared.resetUnreadCount(for: channelId, in: authService.currentUser?.uid ?? "")
        }
        .onDisappear {
            for listener in listeners { listener.remove() }
            if let uid = authService.currentUser?.uid {
                FirebaseManager.shared.clearTyping(for: channelId, userId: uid)
            }
        }
    }

    private var mentionPickerView: some View {
        ScrollView(.horizontal) {
            HStack {
                ForEach(filteredMembers, id: \.self) { memberUid in
                    if let user = mentionUsers[memberUid] {
                        Button(action: { insertMention(user: user) }) {
                            HStack {
                                AvatarCircle(user: user, size: 24)
                                Text(user.username)
                                    .foregroundColor(.white)
                                    .font(.caption)
                            }
                            .padding(6)
                            .background(Color(hex: "#2B2D31"))
                            .cornerRadius(8)
                        }
                    }
                }
            }
            .padding(.horizontal)
        }
        .frame(height: 50)
        .background(Color(hex: "#1E1F22"))
    }

    @State private var mentionUsers: [String: User] = [:]

    private var filteredMembers: [String] {
        guard let members = serverMembers else { return [] }
        if mentionQuery.isEmpty { return members }
        return members.filter { uid in
            (mentionUsers[uid]?.username.lowercased() ?? "").contains(mentionQuery.lowercased())
        }
    }

    private func insertMention(user: User) {
        let parts = messageText.split(separator: "@", omittingEmptySubstrings: false)
        var newText = ""
        for (i, part) in parts.enumerated() {
            if i == parts.count - 1 {
                newText += "@\(user.username) "
            } else {
                newText += String(part) + "@"
            }
        }
        messageText = newText
        showMentionPicker = false
    }

    private func sendMessage() {
        guard let uid = authService.currentUser?.uid,
              let username = authService.currentUser?.username else { return }
        let text = messageText.trimmingCharacters(in: .whitespaces)

        Task {
            do {
                try await messageService.sendMessage(
                    channelId: channelId,
                    senderId: uid,
                    senderName: username,
                    senderAvatar: authService.currentUser?.avatarUrl,
                    content: text,
                    replyToId: replyToMessage?.id,
                    mentions: text.contains("@") ? extractMentions(from: text) : nil
                )
                await MainActor.run {
                    messageText = ""
                    replyToMessage = nil
                }
            } catch {}
        }
    }

    private func sendGIF(url: String) {
        guard let uid = authService.currentUser?.uid,
              let username = authService.currentUser?.username else { return }
        Task {
            try? await messageService.sendGIF(
                channelId: channelId,
                senderId: uid,
                senderName: username,
                senderAvatar: authService.currentUser?.avatarUrl,
                gifUrl: url
            )
        }
    }

    private func toggleReaction(message: Message, emoji: String) {
        guard let uid = authService.currentUser?.uid,
              let msgId = message.id else { return }
        Task {
            if message.userReactions?[uid] == emoji {
                try? await messageService.removeReaction(messageId: msgId, emoji: emoji, userId: uid)
            } else {
                try? await messageService.addReaction(messageId: msgId, emoji: emoji, userId: uid)
            }
        }
    }

    private func extractMentions(from text: String) -> [String] {
        let words = text.split(separator: " ")
        let mentions = words.filter { $0.hasPrefix("@") }.map { String($0.dropFirst()) }
        // Resolve to UIDs from server members
        return mentions
    }

    private func observeMessages() {
        let listener = messageService.observeMessages(channelId: channelId) { msgList in
            self.messages = msgList.map { msg in
                var m = msg
                m.setLocalFlags(
                    isSent: msg.senderId == authService.currentUser?.uid,
                    isRead: true
                )
                return m
            }
        }
        listeners.append(listener)
    }

    private func observeTyping() {
        guard let uid = authService.currentUser?.uid else { return }
        let listener = FirebaseManager.shared.usersCollection
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                var typing: [String] = []
                for doc in docs {
                    let data = doc.data()
                    if let typingMap = data["typing"] as? [String: Timestamp] {
                        for (chId, timestamp) in typingMap {
                            if chId == channelId && doc.documentID != uid {
                                if Date().timeIntervalSince(timestamp.dateValue()) < 10 {
                                    typing.append(data["username"] as? String ?? "Someone")
                                }
                            }
                        }
                    }
                }
                self.typingUsers = typing
            }
        listeners.append(listener)
    }
}
