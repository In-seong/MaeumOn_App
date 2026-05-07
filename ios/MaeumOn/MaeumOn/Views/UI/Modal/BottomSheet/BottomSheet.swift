//
//  BottomSheet.swift
//  MaeumOn
//
//  Created by scoop dev on 11/7/25.
//

import SwiftUI

struct BottomSheet<Content: View>: View {
   @Binding var isPresented: Bool
   let content: Content
   @State private var sheetHeight: CGFloat = 0
   @State private var showSheet = false  // ✅ 추가
   
   init(isPresented: Binding<Bool>, @ViewBuilder content: () -> Content) {
      self._isPresented = isPresented
      self.content = content()
   }
   
   var body: some View {
      ZStack(alignment: .bottom) {
         if isPresented {
            // 배경
            Color.black.opacity(0.4)
               .ignoresSafeArea()
            //                    .onTapGesture { close() }
               .transition(.opacity)
            
            // 시트
            VStack(spacing: 0) {
               content
                  .background(
                     GeometryReader { proxy in
                        Color.clear
                           .onAppear {
                              sheetHeight = proxy.size.height
                           }
                     }
                  )
            }
            .background(
               Color(.white)
                  .clipShape(RoundedCorner(radius: 20, corners: [.topLeft, .topRight]))
                  .shadow(radius: 10)
            )
            // ✅ showSheet 상태로 애니메이션 제어
            .offset(y: showSheet ? 0 : sheetHeight + 100)
            .onAppear {
               withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                  showSheet = true
               }
            }
            .onDisappear {
               showSheet = false
            }
            .transition(.move(edge: .bottom))
         }
      }
      .ignoresSafeArea()
   }
   
   private func close() {
      withAnimation(.easeInOut(duration: 0.25)) {
         showSheet = false  // ✅ 먼저 내려가게
      }
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
         isPresented = false // 내려간 후 제거
      }
   }
}

extension View {
    func bottomSheet<Content: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        ZStack {
            self
            BottomSheet(isPresented: isPresented, content: content)
        }
    }
}
