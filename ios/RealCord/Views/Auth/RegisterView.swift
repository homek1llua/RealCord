import SwiftUI

struct RegisterView: View {
    @EnvironmentObject var authService: AuthService
    @Environment(\.dismiss) var dismiss
    @State private var username = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""

    var body: some View {
        ZStack {
            Color(hex: "#1E1F22").ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    Text("Create an account")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.top, 40)

                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("USERNAME")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                            TextField("Username", text: $username)
                                .textFieldStyle(RealCordTextFieldStyle())
                                .autocapitalization(.none)
                                .disableAutocorrection(true)
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text("EMAIL")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                            TextField("Email", text: $email)
                                .textFieldStyle(RealCordTextFieldStyle())
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .disableAutocorrection(true)
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text("PASSWORD")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                            SecureField("Password", text: $password)
                                .textFieldStyle(RealCordTextFieldStyle())
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text("CONFIRM PASSWORD")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                            SecureField("Confirm Password", text: $confirmPassword)
                                .textFieldStyle(RealCordTextFieldStyle())
                        }

                        if let error = authService.errorMessage {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(Color(hex: "#ED4245"))
                        }

                        Button(action: {
                            guard password == confirmPassword else {
                                authService.errorMessage = "Passwords do not match"
                                return
                            }
                            guard password.count >= 6 else {
                                authService.errorMessage = "Password must be at least 6 characters"
                                return
                            }
                            authService.register(username: username, email: email, password: password)
                        }) {
                            Text("Continue")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color(hex: "#5865F2"))
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }

                        Button(action: { dismiss() }) {
                            Text("Already have an account? Log In")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#5865F2"))
                        }
                    }
                    .padding(.horizontal, 32)
                }
            }
        }
        .navigationBarHidden(true)
    }
}
