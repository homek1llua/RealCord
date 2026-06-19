import SwiftUI

struct MainTabView: View {
    @EnvironmentObject var authService: AuthService
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            FriendsListView()
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Friends")
                }
                .tag(0)

            ServerListView()
                .tabItem {
                    Image(systemName: "server.rack")
                    Text("Servers")
                }
                .tag(1)

            SettingsView()
                .tabItem {
                    Image(systemName: "gearshape.fill")
                    Text("Settings")
                }
                .tag(2)
        }
        .accentColor(Color(hex: "#5865F2"))
        .preferredColorScheme(.dark)
    }
}
