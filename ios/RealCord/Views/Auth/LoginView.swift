import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authService: AuthService
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        ZStack {
            Color(hex: "#1E1F22").ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    Image(systemName: "message.fill")
                        .font(.system(size: 50))
                        .foregroundColor(Color(hex: "#5865F2"))
                        .padding(.top, 60)

                    Text("Welcome back!")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text("We're so excited to see you again!")
                        .font(.subheadline)
                        .foregroundColor(Color(hex: "#80848E"))

                    VStack(spacing: 16) {
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

                        if let error = authService.errorMessage {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(Color(hex: "#ED4245"))
                        }

                        Button(action: {
                            authService.login(email: email, password: password)
                        }) {
                            Text("Log In")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color(hex: "#5865F2"))
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }

                        NavigationLink(destination: RegisterView().environmentObject(authService)) {
                            Text("Need an account? Register")
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

struct RealCordTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(12)
            .background(Color(hex: "#313338"))
            .cornerRadius(8)
            .foregroundColor(.white)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color(hex: "#1E1F22"), lineWidth: 1)
            )
    }
}
