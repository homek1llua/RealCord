import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseMessaging
import UIKit

class AuthService: ObservableObject {
    @Published var isLoggedIn = false
    @Published var isLoading = true
    @Published var currentUser: User?
    @Published var errorMessage: String?

    private let fb = FirebaseManager.shared
    private var authListener: AuthStateDidChangeListenerHandle?

    init() {
        authListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            DispatchQueue.main.async {
                if let user = user {
                    self?.fetchCurrentUser(uid: user.uid)
                } else {
                    self?.isLoggedIn = false
                    self?.isLoading = false
                    self?.currentUser = nil
                }
            }
        }
    }

    deinit {
        if let listener = authListener {
            Auth.auth().removeStateDidChangeListener(listener)
        }
    }

    private func fetchCurrentUser(uid: String) {
        fb.usersCollection.document(uid).addSnapshotListener { [weak self] snapshot, error in
            DispatchQueue.main.async {
                guard let self = self, let snapshot = snapshot else {
                    self?.isLoading = false
                    return
                }
                if let user = try? snapshot.data(as: User.self) {
                    self.currentUser = user
                    self.isLoggedIn = true
                } else {
                    self.isLoggedIn = false
                }
                self.isLoading = false
            }
        }
    }

    func login(email: String, password: String) {
        isLoading = true
        errorMessage = nil
        Auth.auth().signIn(withEmail: email, password: password) { [weak self] result, error in
            DispatchQueue.main.async {
                if let error = error {
                    self?.errorMessage = error.localizedDescription
                    self?.isLoading = false
                }
            }
        }
    }

    func register(username: String, email: String, password: String) {
        isLoading = true
        errorMessage = nil

        let filteredUsername = username.replacingOccurrences(of: "[^a-zA-Z0-9_]", with: "", options: .regularExpression)
        guard !filteredUsername.isEmpty else {
            errorMessage = "Username can only contain letters, numbers, and underscores"
            isLoading = false
            return
        }

        Auth.auth().createUser(withEmail: email, password: password) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self = self else { return }
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                    return
                }
                guard let authUser = result?.user else {
                    self.isLoading = false
                    return
                }

                let avatarColor = self.randomColor()
                let newUser = User(
                    uid: authUser.uid,
                    username: filteredUsername,
                    email: email,
                    avatarColor: avatarColor,
                    avatarText: String(filteredUsername.prefix(2)).uppercased(),
                    status: "online",
                    createdAt: Timestamp(date: Date())
                )

                do {
                    try self.fb.usersCollection.document(authUser.uid).setData(from: newUser)
                    self.saveFCMToken(uid: authUser.uid)
                } catch {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                }
            }
        }
    }

    func logout() {
        guard let uid = currentUser?.uid else {
            try? Auth.auth().signOut()
            return
        }
        fb.updateStatus("offline", for: uid)
        try? Auth.auth().signOut()
    }

    private func saveFCMToken(uid: String) {
        Messaging.messaging().token { token, error in
            if let token = token {
                self.fb.usersCollection.document(uid).updateData(["fcmToken": token])
            }
        }
    }

    private func randomColor() -> String {
        let colors = ["#5865F2", "#ED4245", "#F0B232", "#23A559", "#80848E",
                       "#9B59B6", "#1ABC9C", "#E91E63", "#00BCD4", "#FF9800"]
        return colors.randomElement() ?? "#5865F2"
    }

    func updateCurrentUser(_ user: User) {
        self.currentUser = user
    }
}
