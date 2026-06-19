import SwiftUI
import Kingfisher

struct SettingsView: View {
    @EnvironmentObject var authService: AuthService
    @State private var showProfile = false
    @State private var showLogoutConfirm = false

    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "#313338").ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // Profile header
                        VStack(spacing: 12) {
                            Button(action: { showProfile = true }) {
                                KFImage(URL(string: authService.currentUser?.avatarUrl ?? ""))
                                    .placeholder {
                                        Circle()
                                            .fill(Color(hex: authService.currentUser?.avatarColor ?? "#5865F2"))
                                            .overlay(
                                                Text(authService.currentUser?.avatarText ?? "")
                                                    .font(.largeTitle)
                                                    .fontWeight(.bold)
                                                    .foregroundColor(.white)
                                            )
                                    }
                                    .resizable()
                                    .frame(width: 80, height: 80)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color(hex: "#5865F2"), lineWidth: 3))
                            }

                            Text(authService.currentUser?.username ?? "")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)

                            if authService.currentUser?.hasNitro() == true {
                                HStack {
                                    Image(systemName: "star.fill")
                                        .foregroundColor(Color(hex: "#F0B232"))
                                    Text("Nitro")
                                        .foregroundColor(Color(hex: "#F0B232"))
                                        .fontWeight(.semibold)
                                    if authService.currentUser?.hasPremium() == true {
                                        Text("Premium")
                                            .foregroundColor(Color(hex: "#F0B232"))
                                    }
                                }
                                .font(.caption)
                            }
                        }
                        .padding(.top)

                        // Status picker
                        VStack(alignment: .leading, spacing: 8) {
                            Text("STATUS")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))

                            HStack(spacing: 12) {
                                ForEach(["online", "idle", "dnd", "offline"], id: \.self) { status in
                                    Button(action: { updateStatus(status) }) {
                                        VStack(spacing: 4) {
                                            Circle()
                                                .fill(Color(hex: statusColor(status)))
                                                .frame(width: 12, height: 12)
                                            Text(status.capitalized)
                                                .font(.caption2)
                                        }
                                        .padding(8)
                                        .background(
                                            authService.currentUser?.status == status
                                            ? Color(hex: "#5865F2").opacity(0.3)
                                            : Color.clear
                                        )
                                        .cornerRadius(8)
                                    }
                                    .foregroundColor(.white)
                                }
                            }
                        }
                        .padding(.horizontal)

                        Divider()
                            .background(Color(hex: "#1E1F22"))
                            .padding(.horizontal)

                        // Navigation links
                        VStack(spacing: 0) {
                            NavigationLink(destination: ProfileEditView()
                                .environmentObject(authService)) {
                                SettingsRowView(icon: "pencil", title: "Edit Profile")
                            }

                            SettingsRowView(icon: "bell", title: "Notifications")
                                .onTapGesture {
                                    // Open notification settings (simplified)
                                }

                            if authService.currentUser?.hasNitro() == true {
                                SettingsRowView(icon: "star.fill", title: "Nitro Styling")
                            }
                        }
                        .background(Color(hex: "#2B2D31"))
                        .cornerRadius(8)
                        .padding(.horizontal)

                        // About section
                        VStack(spacing: 0) {
                            SettingsRowView(icon: "info.circle", title: "About RealCord")
                            SettingsRowView(icon: "doc.text", title: "Terms of Service")
                            SettingsRowView(icon: "hand.raised", title: "Privacy Policy")
                        }
                        .background(Color(hex: "#2B2D31"))
                        .cornerRadius(8)
                        .padding(.horizontal)

                        // Logout
                        Button(action: { showLogoutConfirm = true }) {
                            Text("Log Out")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color(hex: "#ED4245"))
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                        .padding(.horizontal)
                        .alert("Log Out?", isPresented: $showLogoutConfirm) {
                            Button("Log Out", role: .destructive) {
                                authService.logout()
                            }
                            Button("Cancel", role: .cancel) {}
                        } message: {
                            Text("Are you sure you want to log out?")
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showProfile) {
                ProfileDialogView(
                    user: authService.currentUser ?? User(uid: "", username: "", email: ""),
                    currentUserId: authService.currentUser?.uid ?? "",
                    isFriend: false,
                    onUnfriend: nil,
                    onClose: { showProfile = false }
                )
            }
        }
        .navigationViewStyle(.stack)
    }

    private func updateStatus(_ status: String) {
        guard let uid = authService.currentUser?.uid else { return }
        FirebaseManager.shared.updateStatus(status, for: uid)
    }

    private func statusColor(_ status: String) -> String {
        switch status {
        case "online": return "#23A559"
        case "idle": return "#F0B232"
        case "dnd": return "#ED4245"
        default: return "#80848E"
        }
    }
}

struct SettingsRowView: View {
    let icon: String
    let title: String

    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(Color(hex: "#80848E"))
                .frame(width: 24)
            Text(title)
                .foregroundColor(.white)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(Color(hex: "#80848E"))
        }
        .padding()
        .background(Color(hex: "#2B2D31"))
    }
}
