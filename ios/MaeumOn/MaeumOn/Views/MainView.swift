//
//  Main.swift
//  MaeumOn
//
//  Created by scoop dev on 11/4/25.
//

import SwiftUI
import WebKit
import MapKit
import Network
import CoreNFC
import DeviceKit
import AVFoundation

struct MainView :View {
    @Environment(\.scenePhase) private var scenePhase

    @State var showScanner: Bool = false
    @State var callbackName: String?
    @State var webView: WKWebView?
    @State var webViewError: Bool = false
    @State var shouldReload: Bool = false
    @State var isWebViewLoaded: Bool = false
    @State var networkState: NWPath.Status = .unsatisfied
    @ObservedObject var permissionManager = PermissionManager.shared
    @State var goSettings: Bool = false

    @State var nfcReader: NFCReader = NFCReader()
    @State private var bleScanner = BleScanner()
    @State private var bleAggregated: [UInt32: Int] = [:]
    @State private var bleAggregationTask: Task<Void, Never>?
    @StateObject private var locationManager = LocationManager()

    #if USER
    let WebURL = "https://user.bohumon.co.kr"
    #elseif AGENT
    let WebURL = "https://agent.bohumon.co.kr"
    #elseif ADMIN
    let WebURL = "https://admin.bohumon.co.kr"
    #else
    let WebURL = "https://user.bohumon.co.kr"
    #endif

    var body: some View {
        NavigationView {
            ZStack {
                WebViewContainer(url: URL(string: WebURL), webView: $webView, isError: $webViewError, shouldReload: $shouldReload, onMessage: { action, body in
                    switch action {
                    case "closeApp" :
                        UIApplication.shared.perform(#selector(NSXPCConnection.suspend))
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            exit(0)
                        }
                    case "startBLE":
                        startBLE()
                        
                    case "stopBLE":
                        stopBLE()
                        
                    case "startNFCScan" :
                        self.callbackName = body["callback"] as? String ?? "qrCallback"
                        nfcReader.beginScanning()
                        
                    case "stopNFCScan":
                        nfcReader.stopScanning()
                        
                    case "startQRScan":
                        if PermissionManager.shared.cameraAuthorized == .denied {
                            // 토스트 표시
                            let appIcon = UIImage(named: "AppIcon") ?? nil
                            ToastManager.shared.show(
                                "QR 탑승 시 태그를 위해 카메라 권한이 필요합니다. 설정으로 이동합니다.",
                                icon: appIcon != nil ? Image(uiImage: appIcon!) : nil,
                                duration: 2.0,
                                haptic: .warning
                            )

                            // 1초 후 설정 앱으로 이동
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                                if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }
                            closeQRscan()
                        } else {
                            showScanner = true
                        }
                        break
                        
                    case "stopScan":
                        showScanner = false
                        break
                        
                    case "startLocationTracking":
                        locationManager.startLocationTracking()
                        
                    case "stopLocationTracking":
                        locationManager.stopLocationTracking()
                        
                    case "getNotificationPermission":
                        callbackName = body["callback"] as? String ?? "????"
                        sendNotificationPermission()
                        
                    case "requestNotificationPermission":
                        callbackName = body["callback"] as? String ?? "????"
                        
                        goSettings = true
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    case "openExternalUrl":
                        if let urlString = body["url"] as? String, let url = URL(string: urlString) {
                            UIApplication.shared.open(url)
                        }
                        break
                    case "debugLog":
                        let step = body["step"] as? String ?? "?"
                        let msg = body["message"] as? String ?? ""
                        let data = body["data"] as? String ?? ""
                        print("🔍 [Step \(step)] \(msg) \(data)")
                    default:
                        break
                    }
                }, onLoadComplete: {
                    print("[WebView] 로드 완료")
                    isWebViewLoaded = true

                    // JavaScript 함수들을 순차적으로 실행 (각각 완료 후 다음 실행)
                    executeJavaScriptSequentially()
                })
                .onChange(of: nfcReader.status) { state in
                    if state == .success {
                        sendNFC(data: nfcReader.data)
                    }
                    if state == .failure {
//                        sendNFC(data: "") // ** 오류 처리해야 함.
                    }
                }
                .onChange(of: locationManager.point) { newValue in
                    guard let point = newValue else { return  }
                    print("좌표 변경 감지!")
                    sendLocationToWebView(location: point)
                }
                //            .onChange(of: scenePhase) { newPhase in
                //                handleAppLifecycle(phase: newPhase)
                //            }
                //            .onChange(of: goSettings) { newValue in
                //               if !newValue {
                //                  sendNotificationPermission()
                //               }
                //            }
                .onChange(of: permissionManager.notificationAuthorized) { _ in
                    // ContentView에서 checkPermissions() 완료 후 권한 상태가 갱신되면 트리거
                    if goSettings {
                        goSettings = false
                        sendNotificationPermission()
                    }
                }
                .onChange(of: networkState) { _ in
                    updateModalState()
                }
                .onChange(of: permissionManager.bluetoothState) { _ in
                    statusBLE(permissionManager.isBLEOn() ? 1 : 0)
                }
                .onReceive(NotificationCenter.default.publisher(for: Notification.Name("FCMTokenReceived"))) { notification in
                    if let token = notification.userInfo?["token"] as? String {
                        // WebView가 이미 로드된 경우에만 즉시 전송
                        if isWebViewLoaded {
                            sendFCMTokenToWebView(token: token)
                        }
                        // 아직 로드 안됐으면 UserDefaults에 저장되어 있으므로 onLoadComplete에서 전송됨
                    }
                }
                NavigationLink(destination: BarcodeScannerView(showScanner: $showScanner) { code in
                    handleScannedCode(code: code)
                }
                .navigationBarHidden(true), isActive: $showScanner) {
                    EmptyView()
                }
                .hidden()
                if webViewError {
                    WebViewReloadAlertView(onTap: {
                        shouldReload = true     // 리로드 트리거
                    })
                }
            }
            .navigationBarHidden(true)
            .onAppear {
                
                startNetworkMonitoring()
                updateModalState()
            }
        }
    }
}

// MARK: - Modal State Management
extension MainView {
    private func updateModalState() {
        // 우선순위 1: 네트워크
        if networkState != .satisfied {
            BasicModalManager.shared.present(blockDismiss: true) {
                networkAlert
            }
            return
        }
        
        // 우선순위 2: BLE
        
        if !permissionManager.isBLEOn() {
            return
        }
        
        // 둘 다 정상이면 모달 닫기
        BasicModalManager.shared.dismiss()
    }
}
extension MainView {
    var networkAlert: some View {
        VStack(spacing: 10) {
            Image(systemName: "wifi.slash")
                .resizable()
                .scaledToFit()
                .frame(width: 50)
                .foregroundStyle(Color.accent)
            Text("네트워크에 접속할 수 없습니다.\n네트워크 연결 상태를 확인해주세요.")
                .pretendard(size: 14)
                .multilineTextAlignment(.center)
                .lineSpacing(10)
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(10)
    }
}

extension MainView {
    /// JavaScript 함수들을 순차적으로 실행 (evaluateJavaScript 완료 콜백 기반)
    private func executeJavaScriptSequentially() {
        // 1. sendTypeToWebView 실행
        executeWithCompletion(
            name: "sendTypeToWebView",
            action: { completion in
                sendTypeToWebView(completion: completion)
            },
            onComplete: {
                // 2. FCM 토큰 전송
                self.executeWithCompletion(
                    name: "sendFCMToken",
                    action: { completion in
                        if let savedToken = UserDefaults.standard.string(forKey: "fcmToken") {
                            self.sendFCMTokenToWebView(token: savedToken, completion: completion)
                        } else {
                            completion()
                        }
                    },
                    onComplete: {
                        // 3. NFC 상태 전송
                        self.executeWithCompletion(
                            name: "statusNfc",
                            action: { completion in
                                self.statusNfc(completion: completion)
                            },
                            onComplete: {
                                // 4. BLE 상태 전송
                                self.executeWithCompletion(
                                    name: "statusBLE",
                                    action: { completion in
                                        self.statusBLE(self.permissionManager.isBLEOn() ? 1 : 0, completion: completion)
                                    },
                                    onComplete: {
                                        // 5. 카메라 상태 전송
                                        self.executeWithCompletion(
                                            name: "cameraStatus",
                                            action: { completion in
                                                self.cameraStatus(completion: completion)
                                            },
                                            onComplete: {
                                                print("[WebView] 모든 초기화 JavaScript 실행 완료")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    /// JavaScript 실행 완료를 보장하는 헬퍼 함수
    private func executeWithCompletion(name: String, action: @escaping (@escaping () -> Void) -> Void, onComplete: @escaping () -> Void) {
        print("[WebView] \(name) 실행 시작")
        action {
            print("[WebView] \(name) 완료")
            // 약간의 여유를 두고 다음 작업 실행 (50ms)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                onComplete()
            }
        }
    }

    func handleScannedCode(code: String) {
        guard !showScanner , let _ = webView else  { return }
        guard !code.isEmpty else {
            closeQRscan()
            return
        }
        // QR 코드 스캔 완료 시 웹뷰로 결과 전송
//        sendResultToWebView(callbackName: callbackName!, data: code, error: nil)
        sendQRscan(code: code)
        closeQRscan()
    }
    
    func closeQRscan() {
        guard let webView = webView else { return }
        let jsCode = "if(window.__closeQRScan__) { window.__closeQRScan__({ status: 'close' }); }";
        webView.evaluateJavaScript(jsCode) { result, error in
            if let error = error {
                print("JavaScript evaluation error: \(error.localizedDescription)")
            }
        }
    }
    func sendQRscan(code: String) {
        guard let webView = webView else { return }
        let jsCode = "if(window.__sendQRCode__) { window.__sendQRCode__({ code: '\(code)' }); }"
        webView.evaluateJavaScript(jsCode) { result, error in
            if let error = error {
                print("JavaScript evaluation error: \(error.localizedDescription)")
            }
        }
    }
    
    func cameraStatus(completion: (() -> Void)? = nil) {
        guard let webView = webView else {
            completion?()
            return
        }
        let jsCode = "if(window.__handleCameraStatus__) { window.__handleCameraStatus__({ status: 1 }); }"
        webView.evaluateJavaScript(jsCode) { result, error in
            if let error = error {
                print("JavaScript evaluation error: \(error.localizedDescription)")
            }
            completion?()
        }
    }

    func sendResultToWebView(callbackName: String, data: String?, error: String?) {
        guard let webView = webView else { return }
        
        let jsCode: String
        if let error = error {
            let escapedError = error
                .replacingOccurrences(of: "'", with: "\\'")
                .replacingOccurrences(of: "\n", with: "\\n")
            jsCode = """
            if (typeof window.\(callbackName) === 'function') {
                window.\(callbackName)({ success: false, error: '\(escapedError)' });
            }
            """
        } else if let data = data {
            let escapedData = data
                .replacingOccurrences(of: "'", with: "\\'")
                .replacingOccurrences(of: "\n", with: "\\n")
            jsCode = """
            if (typeof window.\(callbackName) === 'function') {
                window.\(callbackName)({ success: true, data: '\(escapedData)' });
            }
            """
        } else {
            return
        }
        
        webView.evaluateJavaScript(jsCode) { result, error in
            if let error = error {
                print("JavaScript evaluation error: \(error.localizedDescription)")
            }
        }
        
    }
    
    func sendNotificationPermission() {
        let permission = permissionManager.notificationAuthorized == .denied ? "denied": "granted"
        let jsCode = """
        window.\(callbackName!)({
            success: true,
            data: JSON.stringify({
                status: '\(permission)',  
                enabled: true       
            })
        })
        """
        guard let webView = webView else { return }
        webView.evaluateJavaScript(jsCode) { result, error in
            if let error = error {
                print("JavaScript evaluation error: \(error.localizedDescription)")
            }
        }
    }
    
    private func sendLocationToWebView(location: CLLocation) {
        let lat = location.coordinate.latitude
        let lng = location.coordinate.longitude
        let accuracy = location.horizontalAccuracy
        let timestamp = Int64(location.timestamp.timeIntervalSince1970 * 1000)
        
        let js = """
        if (window.__handleNativeLocation__) {
            window.__handleNativeLocation__({
                lat: \(lat),
                lng: \(lng),
                accuracy: \(accuracy),
                timestamp: \(timestamp)
            });
        }
        """
        
        webView?.evaluateJavaScript(js) { result, error in
            if let error = error {
                print("[WebViewBridge] Error sending location: \(error)")
            } else {
                print("[WebViewBridge] Location sent: (\(lat), \(lng)) accuracy: \(accuracy)m timestamp: \(timestamp)")
            }
        }
    }
    //   앱 타입, 앱 버전, 디바이스 Type 값 전송
    private func sendTypeToWebView(completion: (() -> Void)? = nil) {
        guard let webView = webView else {
            print("[TYPE] WebView가 아직 준비되지 않음")
            completion?()
            return
        }

        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown"
        let osVersion = UIDevice.current.systemVersion
        let deviceName = Device.current.description
        let deviceType = "\(deviceName) (iOS \(osVersion))"

        let escapedDeviceType = deviceType
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "'", with: "\\'")
        let escapedAppVersion = appVersion
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "'", with: "\\'")

        let js = """
        if (window.__handleType__) {
            window.__handleType__({
               device_type: '\(escapedDeviceType)',
               app_version: '\(escapedAppVersion)',
               device: 1
            });
        }
        """

        webView.evaluateJavaScript(js) { _, error in
            if let error = error {
                print("[WebViewBridge] Error sending type: \(error)")
            } else {
                print("[WebViewBridge] type sent: (device_type: '\(escapedDeviceType)', app_version: '\(escapedAppVersion)', device: 1)")
            }
            completion?()
        }
    }
    //   토큰 전송
    private func sendFCMTokenToWebView(token: String, completion: (() -> Void)? = nil) {
        guard let webView = webView else {
            print("[FCM] WebView가 아직 준비되지 않음")
            completion?()
            return
        }

        // 토큰에서 특수문자 이스케이프 처리
        let escapedToken = token
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "'", with: "\\'")

        let js = """
        if (window.__handleFCMToken__) {
            window.__handleFCMToken__('\(escapedToken)');
            console.log('[FCM] 토큰 수신 완료');
        } else {
            console.log('[FCM] 핸들러가 아직 준비되지 않음');
        }
        """

        webView.evaluateJavaScript(js) { result, error in
            if let error = error {
                print("[FCM] Error sending token: \(error.localizedDescription)")
            } else {
                print("[FCM] Token sent to WebView successfully")
            }
            completion?()
        }
    }
    private func sendError(callbackName: String, error: String) {
        sendResultToWebView(callbackName: callbackName, data: nil, error: error)
    }
    
    private func startNetworkMonitoring() {
        let monitor = NWPathMonitor()
        monitor.pathUpdateHandler = { path in
            DispatchQueue.main.async {
                let previousState = networkState
                networkState = path.status
                
                // 네트워크 복구 시 웹뷰 리로드
                if path.status == .satisfied && previousState == .unsatisfied {
                    shouldReload = true
                }
            }
        }
        monitor.start(queue: DispatchQueue.global())
    }
    
    //   NFC 사용가능 여부 체크
    private func statusNfc(completion: (() -> Void)? = nil) {
        guard let webView = webView else {
            print("[Status NFC] WebView가 아직 준비되지 않음")
            completion?()
            return
        }
        let status: Int = NFCNDEFReaderSession.readingAvailable ? 1 : 0

        let js = """
        if (window.__handleNFCStatus__) {
            window.__handleNFCStatus__({
               status: \(status)
            });
        }
        """

        webView.evaluateJavaScript(js) { result, error in
            if let error = error {
                print("[WebViewBridge] Error sending status nfc: \(error)")
            } else {
                print("[WebViewBridge] status nfc sent: (status: \(status))")
            }
            completion?()
        }
    }
    // to send NFC
    private func sendNFC(data: String) {
        guard let webView = webView else {
            print("[Send NFC] WebView가 아직 준비되지 않음")
            return
        }
        
        let js = "if(window.__handleSendNFCCode__) { window.__handleSendNFCCode__({ code: '\(data)' }); }"
        
        webView.evaluateJavaScript(js) { result, error in
            if let error = error {
                print("[WebViewBridge] Error sending status nfc: \(error)")
            } else {
                print("[WebViewBridge] status nfc sent: (status: \(data))")
            }
        }
    }
    
    //   GPS 상태 변화 체크
    private func statusGps(_ status: Int) {
        guard let webView = webView else {
            print("[Status GPS] WebView가 아직 준비되지 않음")
            return
        }
        
        let js = """
        if (window.__handleGPSStatus__) { 
            window.__handleGPSStatus__({ 
               status: \(status)
            }); 
        }
        """
        
        webView.evaluateJavaScript(js) { result, error in
            if let error = error {
                print("[WebViewBridge] Error sending status gps: \(error)")
            } else {
                print("[WebViewBridge] status gps sent: (status: \(status))")
            }
        }
    }
    
    //   BLE 상태 변화 체크
    private func statusBLE(_ status: Int, completion: (() -> Void)? = nil) {
        guard let webView = webView else {
            print("[Status BLE] WebView가 아직 준비되지 않음")
            completion?()
            return
        }

        let js = """
        if (window.__handleBLEStatus__) {
            window.__handleBLEStatus__({
               status: \(status)
            });
        }
        """

        webView.evaluateJavaScript(js) { result, error in
            if let error = error {
                print("[WebViewBridge] Error sending status ble: \(error)")
            } else {
                print("[WebViewBridge] status ble sent: (status: \(status))")
            }
            completion?()
        }
    }
    
    private func startBLE() {
        bleAggregationTask?.cancel()
        bleAggregated.removeAll()
        
        bleScanner.onDiscover = { rssi, routeId in
            DispatchQueue.main.async {
                // 동일 routeId는 마지막으로 수신한 데이터로 덮어씀
                bleAggregated[routeId] = rssi
            }
        }
        
        bleScanner.start()
        
        bleAggregationTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000) // 1초 간격
                
                let snapshot: [UInt32: Int] = await MainActor.run {
                    defer { bleAggregated.removeAll() }
                    return bleAggregated
                }
                
                guard !snapshot.isEmpty else { continue }
                await MainActor.run {
                    sendBLEToWebView(snapshot)
                }
            }
        }
    }
    
    private func stopBLE() {
        bleAggregationTask?.cancel()
        bleAggregationTask = nil
        bleScanner.stop()
        bleAggregated.removeAll()
    }
    
    private func sendBLEToWebView(_ entries: [UInt32: Int]) {
        guard let webView = webView else {
            print("[BLE] WebView가 아직 준비되지 않음")
            return
        }
        
        let txPower: Double = -59 // 1m 기준 RSSI(필요에 맞게 조정)
        let items = entries.map { routeId, rssi -> String in
            let distance = pow(10.0, (txPower - Double(rssi)) / 20.0)
            let distanceString = String(format: "%.2f", distance)
            return "{ routeId: \(routeId), rssi: \(rssi), distance: \(distanceString) }"
        }.joined(separator: ",")
        
        let js = """
        if (window.__handleSendBLE__) {
            window.__handleSendBLE__([\(items)]);
        }
        """
        
        webView.evaluateJavaScript(js) { _, error in
            if let error = error {
                print("[WebViewBridge] Error sending BLE scan: \(error)")
            } else {
                //            print("[WebViewBridge] BLE scan sent: \(entries.count) \(items)")
            }
        }
    }
}

extension MainView {
    // MARK: - 백그라운드 상태 관리
    private func handleAppLifecycle(phase: ScenePhase) {
        switch phase {
        case .active:
            Task {
                // 권한 상태를 다시 확인
                await permissionManager.checkPermissions()
                goSettings = false
            }
            
        case .background:
            break
        case .inactive:
            break
        @unknown default:
            break
        }
    }
}
