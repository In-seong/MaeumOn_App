//
//  PermissionCheckView.swift
//  MaeumOn
//
//  Created by scoop dev on 11/7/25.
//
import SwiftUI
import CoreLocation
import CoreBluetooth
import AVFoundation

struct PermissionCheckView: View {
    @StateObject var permissionManager = PermissionManager.shared
    @State var requestPermission: Bool = false
    /// 버튼 문구 계산
    private var permissionText: String {
        if permissionManager.containsNotDeterminedPermissions() {
            return "확인"
        } else if permissionManager.containsDeniedPermissions() {
            return "필수 권한 설정하러가기"
        } else {
            return "모든 권한이 허용되었습니다"
        }
    }
    
    private var showLocationAlert: Bool {
        return permissionManager.locationAuthorized == .notDetermined || permissionManager.locationAuthorized == .denied
    }
    
    private var showBluetoothAlert: Bool {
        return permissionManager.bluetoothAuthorized == .notDetermined || permissionManager.bluetoothAuthorized == .denied
    }
    
    private var showCameraAlert: Bool {
        return permissionManager.cameraAuthorized == .notDetermined || permissionManager.cameraAuthorized == .denied
    }
    
    private var showNotificationAlert: Bool {
        return permissionManager.notificationAuthorized == .notDetermined || permissionManager.notificationAuthorized == .denied
    }
   private var showAlertText: Bool {
      permissionManager.bluetoothAuthorized == .notDetermined || permissionManager.bluetoothAuthorized == .denied || permissionManager.locationAuthorized == .notDetermined || permissionManager.locationAuthorized == .denied
   }
    
    private var showRecommendText: Bool {
        return permissionManager.notificationAuthorized == .notDetermined || permissionManager.notificationAuthorized == .denied || permissionManager.cameraAuthorized == .notDetermined || permissionManager.cameraAuthorized == .denied
    }
    
    var body: some View {
        VStack {
            Text("서비스 이용을 위해 권한을 허용해주세요.")
                .pretendard(.semibold, size: 15)
                .padding(.bottom, 16)
            
           VStack(alignment: .leading) {
              if showAlertText {
                 Text("필수 접근 권한")
                    .pretendard(.bold, size: 15, fontColor: .red)
              }
              // 위치
              if showLocationAlert {
                 HStack {
                    Image("loc_permission")
                       .resizable()
                       .scaledToFit()
                       .frame(width: 20)
                    Text("위치 권한")
                       .pretendard(.bold, size: 15)
                 }
                 Text("정류장을 자동으로 인식하기 위해 사용자의 위치권한이 필요합니다.")
                    .pretendard(.medium, size: 14)
              }
              // 블루투스
              if showBluetoothAlert {
                 HStack {
                    Image("ble_permission")
                       .resizable()
                       .scaledToFit()
                       .frame(width: 20)
                    Text("블루투스 권한")
                       .pretendard(.bold, size: 15)
                 }
                 Text("탑승 시 태그를 위해 블루투스 권한이 필요합니다. 동의 하지 않거나 블루투스가 꺼져 있으면 이용에 제한이 있을 수 있습니다.")
                    .pretendard(.medium, size: 14)
              }
              // 권장 사항
              if showRecommendText {
                 Text("권장 접근 권한")
                    .pretendard(.bold, size: 15, fontColor: .custom("AccentColor"))
                    .padding(.top, 15)
                  if showNotificationAlert {
                      HStack {
                          Image("noti_permission")
                              .resizable()
                              .scaledToFit()
                              .frame(width: 20)
                          Text("알림 권한")
                              .pretendard(.bold, size: 15)
                          
                      }
                      Text("버스 도착 알람 등을 위해 알람권한이 필요합니다.")
                          .pretendard(.medium, size: 14)
                  }
                  if showCameraAlert {
                      HStack {
                          Image("camera_permission")
                              .resizable()
                              .scaledToFit()
                              .frame(width: 20)
                          Text("카메라 사용 권한")
                              .pretendard(.bold, size: 15)
                      }
                      Text("QR 탑승 시 태그를 위해 카메라 권한이 필요합니다. 동의 하지 않거나 카메라가 사용이 불가능하면 이용에 제한이 있을 수 있습니다.")
                          .pretendard(.medium, size: 14)
                  }
              }
              
           }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            RoundedButton(text: permissionText) {
                if permissionManager.containsNotDeterminedPermissions() {
                    Task {
                        await permissionManager.requestPermissions()
                    }
                }
                else if permissionManager.containsDeniedPermissions() {
                    if let appSettings = URL(string: UIApplication.openSettingsURLString),
                       UIApplication.shared.canOpenURL(appSettings) {
                        UIApplication.shared.open(appSettings)
                    }
                }
            }
            Text("필수 권한을 허용하지 않을 시 이용이 불가합니다.")
                .pretendard(size: 12)
        }
        .padding(16)
    }
}
