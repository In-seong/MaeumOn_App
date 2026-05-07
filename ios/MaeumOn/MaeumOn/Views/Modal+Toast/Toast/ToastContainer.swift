//
//  ToastContainer.swift
//  MaeumOn
//
//  Created by scoop dev on 11/11/25.
//
import SwiftUI
import Combine

struct ToastContainer: View {
    @ObservedObject var manager = ToastManager.shared
    
    var body: some View {
        ZStack {
            if manager.isShowing {
                VStack {
                    Spacer()
                    HStack(spacing: 10) {
                        if let icon = manager.icon {
                            icon
                                .resizable()
                                .scaledToFit()
                                .frame(width: 18, height: 18)
                        }
                        Text(manager.message)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.black)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(.ultraThinMaterial)
                    .clipShape(Capsule())
                    .shadow(radius: 8)
                    .padding(.bottom, 80)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                .animation(.spring(response: 0.35, dampingFraction: 0.85), value: manager.isShowing)
                .allowsHitTesting(false)
                .zIndex(100)
            }
        }
    }
}
