import SwiftUI

struct CreateServerView: View {
    @EnvironmentObject var authService: AuthService
    @Environment(\.dismiss) var dismiss
    @StateObject private var serverService = ServerService()
    @State private var serverName = ""
    @State private var isCreating = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "#313338").ignoresSafeArea()

                VStack(spacing: 20) {
                    Image(systemName: "server.rack")
                        .font(.system(size: 50))
                        .foregroundColor(Color(hex: "#5865F2"))

                    Text("Create a Server")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    TextField("Server name", text: $serverName)
                        .textFieldStyle(RealCordTextFieldStyle())
                        .padding(.horizontal, 32)

                    if let error = errorMessage {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(Color(hex: "#ED4245"))
                    }

                    Button(action: createServer) {
                        Text(isCreating ? "Creating..." : "Create Server")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(hex: "#5865F2"))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .padding(.horizontal, 32)
                    .disabled(isCreating || serverName.trimmingCharacters(in: .whitespaces).isEmpty)

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

    private func createServer() {
        guard let uid = authService.currentUser?.uid else { return }
        isCreating = true
        errorMessage = nil
        Task {
            do {
                _ = try await serverService.createServer(name: serverName, ownerId: uid)
                await MainActor.run { dismiss() }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isCreating = false
                }
            }
        }
    }
}
