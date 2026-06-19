import SwiftUI

struct ServerListView: View {
    @EnvironmentObject var authService: AuthService
    @StateObject private var serverService = ServerService()
    @State private var servers: [Server] = []
    @State private var showCreateServer = false
    @State private var showJoinServer = false

    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "#313338").ignoresSafeArea()

                VStack(spacing: 0) {
                    HStack {
                        Text("Servers")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)

                        Spacer()

                        Button(action: { showCreateServer = true }) {
                            Image(systemName: "plus.circle.fill")
                                .font(.title3)
                                .foregroundColor(Color(hex: "#23A559"))
                        }

                        Button(action: { showJoinServer = true }) {
                            Image(systemName: "arrow.right.circle.fill")
                                .font(.title3)
                                .foregroundColor(Color(hex: "#5865F2"))
                                .padding(.leading, 8)
                        }
                    }
                    .padding()

                    if servers.isEmpty {
                        Spacer()
                        VStack(spacing: 12) {
                            Image(systemName: "server.rack")
                                .font(.system(size: 40))
                                .foregroundColor(Color(hex: "#80848E"))
                            Text("No servers yet")
                                .foregroundColor(Color(hex: "#80848E"))
                            Text("Create or join a server to get started")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                        }
                        Spacer()
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 0) {
                                ForEach(servers) { server in
                                    NavigationLink(destination: ServerDetailView(server: server)
                                        .environmentObject(authService)) {
                                        ServerRowView(server: server)
                                    }
                                    Divider()
                                        .background(Color(hex: "#1E1F22"))
                                }
                            }
                        }
                    }
                }
            }
            .sheet(isPresented: $showCreateServer) {
                CreateServerView()
                    .environmentObject(authService)
            }
            .sheet(isPresented: $showJoinServer) {
                JoinServerView()
                    .environmentObject(authService)
            }
            .onAppear {
                guard let uid = authService.currentUser?.uid else { return }
                serverService.observeServers(memberId: uid) { serverList in
                    self.servers = serverList
                }
            }
        }
        .navigationViewStyle(.stack)
    }
}

struct ServerRowView: View {
    let server: Server

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color(hex: "#5865F2"))
                    .frame(width: 44, height: 44)
                Text(String(server.name.prefix(2)).uppercased())
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(server.name)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                Text("\(server.memberIds.count) members")
                    .font(.caption)
                    .foregroundColor(Color(hex: "#80848E"))
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(Color(hex: "#80848E"))
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}
