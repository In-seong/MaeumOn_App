//
//  SplashView.swift
//  MaeumOn
//
//  Created by scoop dev on 11/7/25.
//
import SwiftUI
import UIKit
import AppTrackingTransparency

struct SplashView: View {
    @Binding var isPresented: Bool

    var body: some View {
        ZStack {
            Image("splash_img")
                .resizable()
                .scaledToFill()
                .ignoresSafeArea()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            ATTrackingManager.requestTrackingAuthorization(completionHandler: { status in
                switch status {
                case .authorized:
                    print("Authorized")
                case .denied:
                    print("Denied")
                case .notDetermined:
                    print("Not Determined")
                case .restricted:
                    print("Restricted")
                @unknown default:
                    print("Unknown")
                }
            })
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                withAnimation {
                    isPresented = false
                }
            }
        }
    }
}
