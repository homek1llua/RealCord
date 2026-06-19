import SwiftUI
import Kingfisher

struct ProfileDialogView: View {
    let user: User
    let currentUserId: String
    let isFriend: Bool
    let onUnfriend: (() -> Void)?
    let onClose: () -> Void
    @State private var friendUser: User?

    var body: some View {
        ZStack(alignment: .topTrailing) {
            VStack(spacing: 0) {
                // Banner
                ZStack(alignment: .bottom) {
                    if let bannerUrl = user.profileBannerUrl, let url = URL(string: bannerUrl) {
                        KFImage(url)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(height: 120)
                            .clipped()
                    } else {
                        Color(hex: user.profileBackgroundColor ?? "#2B2D31")
                            .frame(height: 120)
                    }

                    // Avatar
                    KFImage(URL(string: user.avatarUrl ?? ""))
                        .placeholder {
                            Circle()
                                .fill(Color(hex: user.avatarColor))
                                .overlay(
                                    Text(user.avatarText)
                                        .font(.title)
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                )
                        }
                        .resizable()
                        .frame(width: 80, height: 80)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color(hex: "#1E1F22"), lineWidth: 4))
                        .offset(y: 40)
                }

                Spacer().frame(height: 50)

                // Info
                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                        Text(user.username)
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        if user.hasNitro() {
                            Image(systemName: "star.fill")
                                .foregroundColor(Color(hex: "#F0B232"))
                                .font(.caption)
                        }
                    }

                    HStack(spacing: 4) {
                        Circle()
                            .fill(Color(hex: user.statusColor()))
                            .frame(width: 8, height: 8)
                        Text(user.status.capitalized)
                            .font(.subheadline)
                            .foregroundColor(Color(hex: user.statusColor()))
                    }

                    if !user.bio.isEmpty {
                        Text(user.bio)
                            .font(.body)
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }

                    if user.hasNitro() {
                        VStack(spacing: 4) {
                            HStack {
                                Image(systemName: "star.fill")
                                    .foregroundColor(Color(hex: "#F0B232"))
                                Text("Nitro")
                                    .foregroundColor(Color(hex: "#F0B232"))
                                if user.hasPremium() {
                                    Text("Premium")
                                        .foregroundColor(Color(hex: "#F0B232"))
                                }
                            }

                            if let expiresAt = user.nitroExpiresAt {
                                Text("Expires \(expiresAt.dateValue(), style: .date)")
                                    .font(.caption)
                                    .foregroundColor(Color(hex: "#80848E"))
                            }
                        }
                        .padding(.top, 4)
                    }

                    if let friendUser = friendUser {
                        Button(action: { onUnfriend?() }) {
                            Text("Remove Friend")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color(hex: "#ED4245"))
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                        .padding(.horizontal, 32)
                        .padding(.top, 8)
                    }
                }

                Spacer()
            }

            Button(action: onClose) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title2)
                    .foregroundColor(.white.opacity(0.7))
                    .padding(8)
            }
        }
        .frame(width: 300, height: 400)
        .background(Color(hex: "#2B2D31"))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(hex: "#1E1F22"), lineWidth: 1)
        )
        .onAppear {
            // Check if friend with more data
        }
    }
}
