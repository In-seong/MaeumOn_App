//
//  PerpheralManager.swift
//  MaeumOn
//
//  Created by scoop dev on 11/12/25.
//

import Foundation
import Combine
import CoreBluetooth
import SwiftUI

final class BLEPeripheralManager: NSObject, ObservableObject {
    private var peripheralManager: CBPeripheralManager!
    
    @Published var isAdvertising = false
    @Published var logMessages: [String] = []

    // iPhone이 광고할 서비스 및 특성 UUID
    private let serviceUUID = CBUUID(string: "00001234-0000-1000-8000-00805F9B34FB")
    private let characteristicUUID = CBUUID(string: "00005678-0000-1000-8000-00805F9B34FB")
    
    private var transferCharacteristic: CBMutableCharacteristic?

    override init() {
        super.init()
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
    }

    func startAdvertising() {
        guard peripheralManager.state == .poweredOn else {
            log("⚠️ Bluetooth not powered on.")
            return
        }

        let advertisementData: [String: Any] = [
            CBAdvertisementDataLocalNameKey: "Mobiliteasy-Peripheral",
            CBAdvertisementDataServiceUUIDsKey: [serviceUUID]
        ]
        peripheralManager.startAdvertising(advertisementData)
        isAdvertising = true
        log("📡 Advertising started as Peripheral")
    }

    func stopAdvertising() {
        peripheralManager.stopAdvertising()
        isAdvertising = false
        log("🛑 Advertising stopped")
    }

    private func log(_ text: String) {
        DispatchQueue.main.async {
            self.logMessages.append(text)
            print(text)
        }
    }
}

// MARK: - Delegate
extension BLEPeripheralManager: CBPeripheralManagerDelegate {
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        switch peripheral.state {
        case .poweredOn:
            log("✅ Peripheral ready")
            
            // 서비스 및 특성 구성
            let properties: CBCharacteristicProperties = [.notify, .read, .write]
            let permissions: CBAttributePermissions = [.readable, .writeable]
            
            transferCharacteristic = CBMutableCharacteristic(
                type: characteristicUUID,
                properties: properties,
                value: nil,
                permissions: permissions
            )

            let service = CBMutableService(type: serviceUUID, primary: true)
            service.characteristics = [transferCharacteristic!]
            
            peripheralManager.add(service)
            log("🧩 Service added: \(serviceUUID)")
            
        case .poweredOff:
            log("⚠️ Bluetooth OFF")
        case .unauthorized:
            log("🚫 Unauthorized")
        case .unsupported:
            log("❌ Unsupported")
        default:
            log("ℹ️ Unknown state")
        }
    }
    
    func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?) {
        if let error = error {
            log("❌ Failed to add service: \(error.localizedDescription)")
        } else {
            log("✅ Service added successfully")
        }
    }
}
