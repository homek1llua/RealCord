import SwiftUI

struct JoinServerView: View {
    @EnvironmentObject var authService: AuthService
    @Environment(\.dismiss) var dismiss
    @StateObject private var serverService = ServerService()
    @State private var inviteCode = ""
    @State private var isJoining = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "#313338").ignoresSafeArea()

                VStack(spacing: 20) {
                    Image(systemName: "arrow.right.circle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(Color(hex: "#5865F2"))

                    Text("Join a Server")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text("Enter an invite code to join a server")
                        .font(.subheadline)
                        .foregroundColor(Color(hex: "#80848E"))

                    TextField("Invite Code", text: $inviteCode)
                        .textFieldStyle(RealCordTextFieldStyle())
                        .autocapitalization(.allCharacters)
                        .disableAutocorrection(true)
                        .padding(.horizontal, 32)

                    if let error = errorMessage {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(Color(hex: "#ED4245"))
                    }

                    Button(action: joinServer) {
                        Text(isJoining ? "Joining..." : "Join Server")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(hex: "#5865F2"))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .padding(.horizontal, 32)
                    .disabled(isJoining || inviteCode.trimmingCharacters(in: .whitespaces).isEmpty)

                    Spacer()
                }
                .padding(.top, 40)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundColor(Color(hex: "#80848E"))
                }
            }
        }
    }

    private func joinServer() {
        guard let uid = authService.currentUser?.uid else { return }
        isJoining = true
        errorMessage = nil
        Task {
            do {
                if let serverId = try await serverService.joinServer(inviteCode: inviteCode, userId: uid) {
                    await MainActor.run { dismiss() }
                } else {
                    await MainActor.run {
                        errorMessage = "Invalid invite code"
                        isJoining = false
                    }
                }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isJoining = false
                }
            }
        }
    }
}
