import SwiftUI

struct SplashView: View {
    @EnvironmentObject var authService: AuthService

    var body: some View {
        ZStack {
            Color(hex: "#1E1F22").ignoresSafeArea()

            VStack(spacing: 16) {
                Image(systemName: "message.fill")
                    .font(.system(size: 60))
                    .foregroundColor(Color(hex: "#5865F2"))

                Text("RealCord")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.white)

                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.2)
            }
        }
    }
}
