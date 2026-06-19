import SwiftUI
import FirebaseCore
import FirebaseAuth

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        NotificationDelegate.shared.configure()
        return true
    }
}

@main
struct RealCordApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var authService = AuthService()

    var body: some Scene {
        WindowGroup {
            if authService.isLoading {
                SplashView()
                    .environmentObject(authService)
            } else if authService.isLoggedIn {
                MainTabView()
                    .environmentObject(authService)
            } else {
                NavigationView {
                    LoginView()
                        .environmentObject(authService)
                }
                .navigationViewStyle(.stack)
            }
        }
    }
}
