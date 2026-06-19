import SwiftUI

struct ServerDetailView: View {
    @EnvironmentObject var authService: AuthService
    @StateObject private var serverService = ServerService()
    let server: Server

    @State private var channels: [Channel] = []
    @State private var showCreateChannel = false
    @State private var newChannelName = ""
    @State private var newChannelType = "text"
    @State private var showDeleteConfirm = false
    @State private var showInviteCode = false

    var body: some View {
        ZStack {
            Color(hex: "#313338").ignoresSafeArea()

            VStack(spacing: 0) {
                // Server header
                VStack(spacing: 4) {
                    HStack {
                        ZStack {
                            Circle()
                                .fill(Color(hex: "#5865F2"))
                                .frame(width: 60, height: 60)
                            Text(String(server.name.prefix(2)).uppercased())
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        }
                        Spacer()
                    }
                    .padding(.horizontal)

                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(server.name)
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            Text("\(server.memberIds.count) members")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                        }
                        Spacer()
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical)

                Divider()
                    .background(Color(hex: "#1E1F22"))

                // Invite code
                Button(action: { showInviteCode.toggle() }) {
                    HStack {
                        Image(systemName: "link")
                            .foregroundColor(Color(hex: "#5865F2"))
                        Text("Invite Code: \(server.inviteCode)")
                            .foregroundColor(Color(hex: "#5865F2"))
                            .fontWeight(.semibold)
                        Spacer()
                        Image(systemName: "doc.on.doc")
                            .foregroundColor(Color(hex: "#80848E"))
                    }
                    .padding()
                }
                .alert("Invite Code", isPresented: $showInviteCode) {
                    Button("Copy") {
                        UIPasteboard.general.string = server.inviteCode
                    }
                    Button("OK", role: .cancel) {}
                } message: {
                    Text("Share this code with friends to invite them:\n\(server.inviteCode)")
                }

                Divider()
                    .background(Color(hex: "#1E1F22"))

                // Channel list header
                HStack {
                    Text("CHANNELS")
                        .font(.caption)
                        .foregroundColor(Color(hex: "#80848E"))
                    Spacer()
                    if server.ownerId == authService.currentUser?.uid {
                        Button(action: { showCreateChannel = true }) {
                            Image(systemName: "plus")
                                .foregroundColor(Color(hex: "#80848E"))
                        }
                    }
                }
                .padding()

                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(channels) { channel in
                            NavigationLink(destination: ChannelChatView(channel: channel)
                                .environmentObject(authService)) {
                                ChannelRowView(channel: channel)
                            }
                            Divider()
                                .background(Color(hex: "#1E1F22"))
                                .padding(.leading, 40)
                        }
                    }
                }

                // Delete server (owner only)
                if server.ownerId == authService.currentUser?.uid {
                    Button(action: { showDeleteConfirm = true }) {
                        Text("Delete Server")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(hex: "#ED4245"))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .padding()
                    .alert("Delete Server?", isPresented: $showDeleteConfirm) {
                        Button("Delete", role: .destructive) {
                            Task {
                                try? await serverService.deleteServer(serverId: server.id ?? "")
                            }
                        }
                        Button("Cancel", role: .cancel) {}
                    } message: {
                        Text("This will permanently delete this server and all its channels.")
                    }
                }
            }

            // Create channel dialog
            if showCreateChannel {
                Color.black.opacity(0.5).ignoresSafeArea()
                VStack(spacing: 16) {
                    Text("Create Channel")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    TextField("Channel name", text: $newChannelName)
                        .textFieldStyle(RealCordTextFieldStyle())

                    Picker("Type", selection: $newChannelType) {
                        Text("Text").tag("text")
                        Text("Voice").tag("voice")
                    }
                    .pickerStyle(.segmented)

                    HStack(spacing: 12) {
                        Button("Cancel") {
                            showCreateChannel = false
                        }
                        .foregroundColor(Color(hex: "#80848E"))

                        Button("Create") {
                            Task {
                                try? await serverService.createChannel(
                                    serverId: server.id ?? "",
                                    name: newChannelName,
                                    type: newChannelType,
                                    position: channels.count
                                )
                                showCreateChannel = false
                                newChannelName = ""
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 8)
                        .background(Color(hex: "#5865F2"))
                        .foregroundColor(.white)
                        .cornerRadius(8)
                    }
                }
                .padding()
                .background(Color(hex: "#2B2D31"))
                .cornerRadius(12)
                .padding(.horizontal, 40)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            serverService.observeChannels(serverId: server.id ?? "") { channelList in
                self.channels = channelList
            }
        }
    }
}

struct ChannelRowView: View {
    let channel: Channel

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: channel.isVoice ? "speaker.wave.2.fill" : "number")
                .foregroundColor(Color(hex: "#80848E"))
                .frame(width: 20)

            Text(channel.name)
                .foregroundColor(.white)
                .fontWeight(.medium)

            Spacer()
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}
