import SwiftUI
import Kingfisher

struct ProfileEditView: View {
    @EnvironmentObject var authService: AuthService
    @Environment(\.dismiss) var dismiss
    @StateObject private var userService = UserService()

    @State private var username = ""
    @State private var email = ""
    @State private var avatarUrl = ""
    @State private var avatarColor = ""
    @State private var avatarText = ""
    @State private var bio = ""
    @State private var status = "online"

    @State private var usernameColor = ""
    @State private var usernameFont = ""
    @State private var usernameGradientStart = ""
    @State private var usernameGradientEnd = ""

    @State private var chatColor = ""
    @State private var chatGradientStart = ""
    @State private var chatGradientEnd = ""

    @State private var profileBgColor = ""
    @State private var profileBgEmoji = ""
    @State private var emojiOpacity: Double = 0.2
    @State private var bannerUrl = ""

    @State private var isSaving = false
    @State private var errorMessage: String?

    let fontOptions = ["", "bold", "italic", "serif", "monospace"]

    var body: some View {
        ZStack {
            Color(hex: "#313338").ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    // Avatar section
                    VStack(spacing: 8) {
                        KFImage(URL(string: avatarUrl))
                            .placeholder {
                                Circle()
                                    .fill(Color(hex: avatarColor.isEmpty ? "#5865F2" : avatarColor))
                                    .overlay(
                                        Text(avatarText)
                                            .font(.largeTitle)
                                            .fontWeight(.bold)
                                            .foregroundColor(.white)
                                    )
                            }
                            .resizable()
                            .frame(width: 80, height: 80)
                            .clipShape(Circle())
                    }

                    // Basic info
                    sectionHeader("BASIC INFORMATION")
                    formField(label: "Username", text: $username)
                    formField(label: "Email", text: $email)
                    formField(label: "Avatar URL", text: $avatarUrl)
                    formField(label: "Avatar Color (hex)", text: $avatarColor)
                    formField(label: "Avatar Text", text: $avatarText)
                    formField(label: "Bio", text: $bio, isMultiline: true)

                    // Nitro Styling
                    if authService.currentUser?.hasNitro() == true {
                        sectionHeader("NITRO STYLING")
                        formField(label: "Username Color (hex)", text: $usernameColor)

                        VStack(alignment: .leading, spacing: 4) {
                            Text("USERNAME FONT")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                            Picker("Font", selection: $usernameFont) {
                                ForEach(fontOptions, id: \.self) { font in
                                    Text(font.isEmpty ? "Default" : font.capitalized).tag(font)
                                }
                            }
                            .pickerStyle(.segmented)
                        }

                        if authService.currentUser?.hasPremium() == true {
                            formField(label: "Username Gradient Start", text: $usernameGradientStart)
                            formField(label: "Username Gradient End", text: $usernameGradientEnd)
                            formField(label: "Chat Color (hex)", text: $chatColor)
                            formField(label: "Chat Gradient Start", text: $chatGradientStart)
                            formField(label: "Chat Gradient End", text: $chatGradientEnd)
                        }

                        sectionHeader("PROFILE BACKGROUND (PREMIUM)")
                        formField(label: "Background Color (hex)", text: $profileBgColor)
                        formField(label: "Background Emoji", text: $profileBgEmoji)

                        VStack(alignment: .leading, spacing: 4) {
                            Text("EMOJI OPACITY: \(Int(emojiOpacity * 100))%")
                                .font(.caption)
                                .foregroundColor(Color(hex: "#80848E"))
                            Slider(value: $emojiOpacity, in: 0.05...0.5)
                                .accentColor(Color(hex: "#5865F2"))
                        }

                        formField(label: "Banner URL", text: $bannerUrl)
                    }

                    // Save button
                    if let error = errorMessage {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(Color(hex: "#ED4245"))
                    }

                    Button(action: saveProfile) {
                        Text(isSaving ? "Saving..." : "Save Changes")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(hex: "#5865F2"))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .disabled(isSaving)

                    Spacer()
                }
                .padding()
            }
        }
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            loadCurrentValues()
        }
    }

    private func loadCurrentValues() {
        guard let user = authService.currentUser else { return }
        username = user.username
        email = user.email
        avatarUrl = user.avatarUrl ?? ""
        avatarColor = user.avatarColor
        avatarText = user.avatarText
        bio = user.bio
        status = user.status
        usernameColor = user.usernameColor ?? ""
        usernameFont = user.usernameFont ?? ""
        usernameGradientStart = user.usernameGradientStart ?? ""
        usernameGradientEnd = user.usernameGradientEnd ?? ""
        chatColor = user.chatColor ?? ""
        chatGradientStart = user.chatGradientStart ?? ""
        chatGradientEnd = user.chatGradientEnd ?? ""
        profileBgColor = user.profileBackgroundColor ?? ""
        profileBgEmoji = user.profileBackgroundEmoji ?? ""
        emojiOpacity = user.profileBackgroundEmojiOpacity ?? 0.2
        bannerUrl = user.profileBannerUrl ?? ""
    }

    private func saveProfile() {
        guard let uid = authService.currentUser?.uid else { return }
        isSaving = true
        errorMessage = nil

        Task {
            do {
                try await userService.updateFullProfile(
                    userId: uid,
                    username: username,
                    email: email,
                    avatarUrl: avatarUrl.isEmpty ? nil : avatarUrl,
                    avatarColor: avatarColor,
                    avatarText: avatarText,
                    status: status,
                    bio: bio,
                    usernameColor: usernameColor.isEmpty ? nil : usernameColor,
                    usernameFont: usernameFont.isEmpty ? nil : usernameFont,
                    usernameGradientStart: usernameGradientStart.isEmpty ? nil : usernameGradientStart,
                    usernameGradientEnd: usernameGradientEnd.isEmpty ? nil : usernameGradientEnd,
                    chatColor: chatColor.isEmpty ? nil : chatColor,
                    chatGradientStart: chatGradientStart.isEmpty ? nil : chatGradientStart,
                    chatGradientEnd: chatGradientEnd.isEmpty ? nil : chatGradientEnd,
                    profileBackgroundColor: profileBgColor.isEmpty ? nil : profileBgColor,
                    profileBackgroundEmoji: profileBgEmoji.isEmpty ? nil : profileBgEmoji,
                    profileBackgroundEmojiOpacity: emojiOpacity,
                    profileBannerUrl: bannerUrl.isEmpty ? nil : bannerUrl
                )

                // Refresh current user
                if let updated = try? await userService.fetchUser(uid: uid) {
                    await MainActor.run {
                        authService.updateCurrentUser(updated)
                        isSaving = false
                        dismiss()
                    }
                } else {
                    await MainActor.run { isSaving = false }
                }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isSaving = false
                }
            }
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        HStack {
            Text(title)
                .font(.caption)
                .foregroundColor(Color(hex: "#80848E"))
            Spacer()
        }
    }

    @ViewBuilder
    private func formField(label: String, text: Binding<String>, isMultiline: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label.uppercased())
                .font(.caption)
                .foregroundColor(Color(hex: "#80848E"))
            if isMultiline {
                TextEditor(text: text)
                    .frame(height: 80)
                    .padding(8)
                    .background(Color(hex: "#1E1F22"))
                    .cornerRadius(8)
                    .foregroundColor(.white)
            } else {
                TextField(label, text: text)
                    .textFieldStyle(RealCordTextFieldStyle())
                    .autocapitalization(.none)
            }
        }
    }
}
