//
//  BasicModalContainer.swift
//  MaeumOn
//
//  Created by scoop dev on 11/18/25.
//

import SwiftUI

struct BasicModalContainer: View {
    @ObservedObject private var manager = BasicModalManager.shared
    @State private var showSheet: Bool = false

    var body: some View {
        ZStack {
            if manager.isPresented {
                // 반투명 배경
                Color.black.opacity(showSheet ? 0.4 : 0)
                    .ignoresSafeArea()
                    .onTapGesture {
                        if !manager.blockDismiss {
                            manager.dismiss()
                        }
                    }
                    .animation(.easeInOut(duration: 0.25), value: showSheet)
                manager.content
                    .frame(maxWidth: .infinity, minHeight: 150)
                    .padding(16)
                .onAppear {
                    // 콘텐츠가 로드되면 부드럽게 올라오게
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.01) {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                            showSheet = true
                        }
                    }
                }
                .onDisappear {
                    showSheet = false
                }
            }
        }
    }
}

