//
//  BottomSheetContainer.swift
//  MaeumOn
//
//  Created by scoop dev on 10/27/25.
//
import SwiftUI

struct BottomSheetContainer: View {
    @ObservedObject private var manager = BottomSheetManager.shared
    @GestureState private var dragOffset: CGFloat = 0
    @State private var contentHeight: CGFloat = 0
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

                VStack {
                    Spacer()

                    VStack(spacing: 0) {
//                        Capsule()
//                            .fill(Color.gray.opacity(0.5))
//                            .frame(width: 40, height: 5)
//                            .padding(.top, 8)
                        Rectangle()
                            .fill(Color.clear)
                            .frame(width: 40, height: 5)
                            .padding(.top, 8)
                        // 시트 콘텐츠
                        manager.content
                            .frame(maxWidth: .infinity, minHeight: 150)
                            .padding(.bottom, 20)
                            .background(
                                GeometryReader { proxy in
                                    Color.clear
                                        .onAppear {
                                            contentHeight = proxy.size.height + 40 // 드래그 핸들 포함
                                        }
                                }
                            )
                    }
                    .background(Color.white)
                    .cornerRadius(20, corners: [.topLeft, .topRight])
                    .offset(y: showSheet ? max(dragOffset, 0) : contentHeight)
                    .gesture(
                        manager.blockDismiss ? nil :
                        DragGesture()
                            .updating($dragOffset) { value, state, _ in
                                // 드래그 범위를 콘텐츠 높이 이하로 제한
                                state = max(0, value.translation.height)
                            }
                            .onEnded { value in
                                if value.translation.height > contentHeight * 0.3 {
                                    manager.dismiss()
                                }
                            }
                    )
//                    .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showSheet)
                }
                .ignoresSafeArea(edges: .bottom)
                .transition(.move(edge: .bottom))
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

