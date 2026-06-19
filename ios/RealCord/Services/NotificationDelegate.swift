import Foundation
import FirebaseAuth
import FirebaseMessaging
import UserNotifications
import UIKit

class NotificationDelegate: NSObject, ObservableObject {
    static let shared = NotificationDelegate()

    @Published var incomingCall: CallOffer?
    @Published var navigateToChannel: String?

    func configure() {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        Task {
            try? await UNUserNotificationCenter.current().requestAuthorization(options: authOptions)
        }
    }
}

extension NotificationDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo

        if let callId = userInfo["callId"] as? String {
            // Incoming call — handled via Firestore listener
            completionHandler([.sound, .banner])
            return
        }

        if let channelId = userInfo["channelId"] as? String {
            navigateToChannel = channelId
        }

        completionHandler([.sound, .banner, .badge])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo

        if let channelId = userInfo["channelId"] as? String {
            navigateToChannel = channelId
        }

        completionHandler()
    }
}

extension NotificationDelegate: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, let uid = Auth.auth().currentUser?.uid else { return }
        FirebaseManager.shared.usersCollection.document(uid).updateData(["fcmToken": token])
    }
}
