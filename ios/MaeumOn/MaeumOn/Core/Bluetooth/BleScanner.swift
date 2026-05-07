//
//  BleScanner.swift
//  NolUniverseCustomer
//
//  Created by (주)스쿱 개발팀 팀장 on 12/5/25.
//

import CoreBluetooth

/// BLE 스캐너: SCOOP-BUS 이름 + Service UUID 또는 Manufacturer Data에서 routeId 파싱
final class BleScanner: NSObject, CBCentralManagerDelegate {
    private var central: CBCentralManager?
    private let manufacturerId: UInt16 = 0xFFFF
    private let deviceName = "SCOOP-BUS"
    
    var onDiscover: ((Int, UInt32) -> Void)?

    func start() {
        central = CBCentralManager(delegate: self, queue: nil)
    }

    func stop() {
        central?.stopScan()
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        guard central.state == .poweredOn else { return }
        central.scanForPeripherals(
            withServices: nil,
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: true] // 연속 수신
        )
        print("✅ BLE 스캔 시작")
    }

    func centralManager(_ central: CBCentralManager,
                        didDiscover peripheral: CBPeripheral,
                        advertisementData: [String: Any],
                        rssi RSSI: NSNumber) {
        
        let name = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let serviceUUIDs = advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID]
        let mfgData = advertisementData[CBAdvertisementDataManufacturerDataKey] as? Data
        
        // 디바이스 이름 확인 (SCOOP-BUS 또는 SCOOP-BUS-{routeId} 형식)
        let isScoopDevice = name?.starts(with: deviceName) ?? false
        
        // 1️⃣ 방법 1: Service UUID에서 RouteId 추출 (iOS 송신용)
        if isScoopDevice, let uuids = serviceUUIDs, !uuids.isEmpty {
            for uuid in uuids {
                if let routeId = extractRouteIdFromServiceUUID(uuid) {
                    print("📱 iOS BLE 수신 (Service UUID) - RouteId: \(routeId), RSSI: \(RSSI)")
                    onDiscover?(RSSI.intValue, routeId)
                    return
                }
            }
        }
        
        // 2️⃣ 방법 2: Device Name에서 RouteId 추출 (iOS 송신 백업)
        // 포맷: "SCOOP-BUS-12345"
        if let name = name, name.starts(with: "\(deviceName)-") {
            let components = name.split(separator: "-")
            if components.count >= 3, let routeId = UInt32(components[2]) {
                print("📱 iOS BLE 수신 (Device Name) - RouteId: \(routeId), RSSI: \(RSSI)")
                onDiscover?(RSSI.intValue, routeId)
                return
            }
        }
        
        // 3️⃣ 방법 3: Manufacturer Data에서 RouteId 추출 (안드로이드 송신용)
        if let mfg = mfgData, mfg.count >= 6 {
            // CompanyId little-endian (2 bytes)
            let companyId = UInt16(mfg[0]) | (UInt16(mfg[1]) << 8)
            
            guard companyId == manufacturerId else { return }
            
            // 이름이 제공되면 SCOOP-BUS만 필터링, 없으면 제조사 데이터만으로 통과
            if let name = name, name != deviceName { return }
            
            // ManufacturerId 뒤 4바이트가 routeId
            let payload = mfg.dropFirst(2)
            guard payload.count >= 4 else { return }
            
            // Big-endian으로 읽기 (안드로이드 송신 포맷)
            let big = UInt32(payload[payload.startIndex]) << 24
                | UInt32(payload[payload.index(after: payload.startIndex)]) << 16
                | UInt32(payload[payload.index(payload.startIndex, offsetBy: 2)]) << 8
                | UInt32(payload[payload.index(payload.startIndex, offsetBy: 3)])
            
            // Little-endian으로도 시도 (만약을 위한 백업)
            let little = UInt32(payload[payload.startIndex])
                | UInt32(payload[payload.index(after: payload.startIndex)]) << 8
                | UInt32(payload[payload.index(payload.startIndex, offsetBy: 2)]) << 16
                | UInt32(payload[payload.index(payload.startIndex, offsetBy: 3)]) << 24
            
            // 우선 big-endian 사용, 값이 0이면 little-endian으로 대체
            let routeId = big != 0 ? big : little
            
            print("📱 안드로이드 BLE 수신 (Manufacturer Data) - RouteId: \(routeId), RSSI: \(RSSI)")
            onDiscover?(RSSI.intValue, routeId)
            return
        }
    }
    
    /// Service UUID에서 RouteId 추출
    /// UUID 포맷: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
    /// 첫 8자리(32비트)가 RouteId
    private func extractRouteIdFromServiceUUID(_ uuid: CBUUID) -> UInt32? {
        let uuidString = uuid.uuidString
        
        // UUID 문자열이 최소 8자 이상인지 확인
        guard uuidString.count >= 8 else { return nil }
        
        // 첫 8자리 추출
        let routeIdHex = String(uuidString.prefix(8))
        
        // Hex 문자열을 UInt32로 변환
        guard let routeId = UInt32(routeIdHex, radix: 16) else { return nil }
        
        // 0은 유효하지 않은 RouteId로 간주
        guard routeId != 0 else { return nil }
        
        return routeId
    }
}
