//
//  AppDelegate.swift
//  MaeumOn
//
//  Created by scoop dev on 11/7/25.
//

import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging

class AppDelegate: UIResponder, UIApplicationDelegate {
    
    // FCM 토큰
    var fcmToken: String?
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
       
       // Firebase 초기화
       FirebaseApp.configure()
       
       // FCM 메시징 델리게이트 등록
       Messaging.messaging().delegate = self
       
       // 푸시 알림 권한 요청
       UNUserNotificationCenter.current().delegate = self
       //        requestNotificationAuthorization()
       
       application.registerForRemoteNotifications()
       return true
    }
    
    // MARK: - 권한 요청
//    private func requestNotificationAuthorization() {
//        let center = UNUserNotificationCenter.current()
//        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
//            if let error = error {
//                print("🔴 Notification permission error: \(error.localizedDescription)")
//            }
//            print("✅ Notification permission granted: \(granted)")
//        }
//    }
    
    // MARK: - APNs 토큰 등록
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("✅ APNs token: \(deviceToken.map { String(format: "%02.2hhx", $0) }.joined())")
        // FCM에 APNs 토큰 전달
        Messaging.messaging().apnsToken = deviceToken
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("🔴 Failed to register for remote notifications: \(error)")
    }
}

// MARK: - Messaging Delegate
extension AppDelegate: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        self.fcmToken = token
        print("✅ FCM registration token: \(token)")
        
        // 토큰을 저장하고 WebView로 전달
        UserDefaults.standard.set(fcmToken, forKey: "fcmToken")

        // MainView에 토큰 전달 (NotificationCenter 또는 다른 방식 사용)
        NotificationCenter.default.post(
            name: Notification.Name("FCMTokenReceived"),
            object: nil,
            userInfo: ["token": token]
        )    }
}

// MARK: - Notification Delegate
extension AppDelegate: UNUserNotificationCenterDelegate {
    // 앱이 foreground일 때 알림 수신 처리
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler:
        @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        print("📩 Foreground Notification: \(notification.request.content.userInfo)")
        completionHandler([.banner, .sound, .badge])
    }
    
    // 사용자가 알림을 탭했을 때 동작
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        print("📬 User tapped notification: \(response.notification.request.content.userInfo)")
        completionHandler()
    }
}

