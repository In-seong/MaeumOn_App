//
//  ContentView.swift
//  NolUniverseCustomer
//
//  Created by (주)스쿱 개발팀 팀장 on 12/4/25.
//

import SwiftUI

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    
    @State private var isSplashActive: Bool = true
    @State var isLoading: Bool = false
    @ObservedObject var permissionManager = PermissionManager.shared
    
    var body: some View {
        ZStack {
            if isSplashActive || permissionManager.isPermissionDenied || isLoading {
                SplashView(isPresented: $isSplashActive)
            }
            else {
                MainView()
            }

            // 토스트 컨테이너 추가
            ToastContainer()
        }
        .onAppear {
            isLoading = true
            Task {
                await permissionManager.checkPermissions()
                
                await MainActor.run {
                    isLoading = false
                }
            }
        }
        .onChange(of: scenePhase) { newPhase in
            handleAppLifecycle(phase: newPhase)
        }
        .bottomSheet(isPresented: $permissionManager.isPermissionDenied) {
            PermissionCheckView()
        }

    }
}

extension ContentView {
    // MARK: - 백그라운드 상태 관리
    private func handleAppLifecycle(phase: ScenePhase) {
        switch phase {
        case .active:
            print("🟢 앱이 포그라운드로 돌아왔습니다.")
            Task {
                // 권한 상태를 다시 확인
                await permissionManager.checkPermissions()
            }
            
        case .background:
            print("🔵 앱이 백그라운드로 전환되었습니다.")
            
        case .inactive:
            print("🟡 앱이 일시 중단(inactive) 상태입니다.")
            
        @unknown default:
            break
        }
    }
}


#Preview {
    ContentView()
}
