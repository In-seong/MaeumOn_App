//
//  PermissionManager.swift
//  MaeumOn
//
//  Created by scoop dev on 11/7/25.
//

import Foundation
import CoreLocation
import AVFoundation
import CoreBluetooth
//import CoreNFC
import UserNotifications
import Combine

@MainActor
final class PermissionManager: NSObject, ObservableObject {
    static let shared = PermissionManager()
    
    private let locationManager = CLLocationManager()
    private var bluetoothManager: CBCentralManager?
    
    // MARK: - Published States
    @Published var locationAuthorized: CLAuthorizationStatus = .notDetermined
    @Published var cameraAuthorized: AVAuthorizationStatus = .notDetermined
    @Published var bluetoothAuthorized: CBManagerAuthorization = .notDetermined
    @Published var bluetoothState: CBManagerState = .unknown
    @Published var notificationAuthorized: UNAuthorizationStatus = .notDetermined
//    @Published var nfcAvailable: Bool = NFCNDEFReaderSession.readingAvailable
    
    @Published var isPermissionDenied = false
    
    // MARK: - Init
    private override init() {
        super.init()
        locationManager.delegate = self
//        bluetoothManager = CBCentralManager(delegate: self, queue: nil, options: [
//            CBCentralManagerOptionShowPowerAlertKey: true
//        ])
    }
}

// MARK: - Location
extension PermissionManager: CLLocationManagerDelegate {
    func requestLocationPermission() {
        if CLLocationManager.authorizationStatus() == .notDetermined {
            locationManager.requestWhenInUseAuthorization()
        }
    }

    func checkLocationPermission() {
        locationAuthorized = CLLocationManager.authorizationStatus()
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        locationAuthorized = manager.authorizationStatus
    }
}

//// MARK: - Camera
extension PermissionManager {
    func requestCameraPermission() async {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        switch status {
        case .notDetermined:
            _ = await AVCaptureDevice.requestAccess(for: .video)
        default:
            break
        }
        cameraAuthorized = AVCaptureDevice.authorizationStatus(for: .video)
    }

    func checkCameraPermission() {
        cameraAuthorized = AVCaptureDevice.authorizationStatus(for: .video)
    }
}

// MARK: - Bluetooth (BLE)
extension PermissionManager: CBCentralManagerDelegate {
    func checkBluetoothPermission() {
        bluetoothAuthorized = CBManager.authorization
        bluetoothState = bluetoothManager?.state ?? .unknown
    }

    func requestBluetoothPermission() {
        bluetoothManager = CBCentralManager(delegate: self, queue: nil)
    }
    
    /// ble 상태 업데이트
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        bluetoothState = central.state
        bluetoothAuthorized = CBManager.authorization
    }

    /// BLE 사용 가능한지 (권한 + 전원 + 하드웨어)
    var isBLEAvailable: Bool {
        bluetoothAuthorized == .allowedAlways && bluetoothState == .poweredOn
    }
    
    /// 지원여부
    var isBleSupported: Bool {
        bluetoothState == .unsupported
    }
}

// MARK: - NFC
//extension PermissionManager {
//    func checkNFCStatus() {
//        nfcAvailable = NFCNDEFReaderSession.readingAvailable
//    }
//}

// MARK: - Notification
extension PermissionManager {
    func requestNotificationPermission() async {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        
        if settings.authorizationStatus == .notDetermined {
            do {
                _ = try await center.requestAuthorization(options: [.alert, .badge, .sound])
            } catch {
                print("🔴 Notification permission error: \(error)")
            }
        }
        
        let updatedSettings = await center.notificationSettings()
        notificationAuthorized = updatedSettings.authorizationStatus
    }

    func checkNotificationPermission() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        notificationAuthorized = settings.authorizationStatus
    }
}

// MARK: - Public Interface
extension PermissionManager {
    func checkPermissions() async {
        checkLocationPermission()
        if AppState.shared.USE_QR_FUNCTION {
            checkCameraPermission()
        }
        checkBluetoothPermission()
//        checkNFCStatus()
        await checkNotificationPermission()
        
        isPermissionDenied = containsDeniedPermissions() || containsNotDeterminedPermissions()
    }

    func requestPermissions() async {
        requestLocationPermission()
        requestBluetoothPermission()
        if AppState.shared.USE_QR_FUNCTION {
            await requestCameraPermission()
        }
        await requestNotificationPermission()
        await checkPermissions()
    }

    func containsDeniedPermissions() -> Bool {
        [
            locationAuthorized == .denied,
//            cameraAuthorized == .denied,
            bluetoothAuthorized == .denied,
//            notificationAuthorized == .denied
        ].contains(true)
    }

    func containsNotDeterminedPermissions() -> Bool {
        var auths = [
            locationAuthorized == .notDetermined,
            bluetoothAuthorized == .notDetermined,
            notificationAuthorized == .notDetermined
        ]
        if AppState.shared.USE_QR_FUNCTION {
            auths.append(cameraAuthorized == .notDetermined)
        }
        
        return auths.contains(true)
    }
   
   func isBLEOn() -> Bool {
      bluetoothManager = CBCentralManager(delegate: self, queue: nil)
      return bluetoothState == .poweredOn
   }
}
