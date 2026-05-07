//
//  ToastManager.swift
//  MaeumOn
//
//  Created by scoop dev on 11/11/25.
//

import SwiftUI
import UIKit
import Combine

final class ToastManager: ObservableObject {
    static let shared = ToastManager()
    
    @Published var isShowing = false
    @Published var message: String = ""
    @Published var icon: Image? = nil
    
    private var hideTask: DispatchWorkItem?
    
    private init() {}
    
    func show(
        _ message: String,
        icon: Image? = nil,
        duration: TimeInterval = 2.0,
        haptic: UINotificationFeedbackGenerator.FeedbackType = .success
    ) {
        DispatchQueue.main.async {
            self.hideTask?.cancel()
            self.message = message
            self.icon = icon
            self.isShowing = true
            
            // ✅ Haptic 진동
            let generator = UINotificationFeedbackGenerator()
            generator.notificationOccurred(haptic)
            
            // ✅ 자동 dismiss
            let task = DispatchWorkItem {
                withAnimation {
                    self.isShowing = false
                }
            }
            self.hideTask = task
            DispatchQueue.main.asyncAfter(deadline: .now() + duration, execute: task)
        }
    }
}
