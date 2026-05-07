//
//  Bluetooth.swift
//  MaeumOn
//
//  Created by scoop dev on 11/4/25.
//

import Foundation
import CoreBluetooth
import SwiftUI
import Combine

class BLEManager: NSObject, ObservableObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    @Published var isBluetoothOn = false
    @Published var peripherals: [CBPeripheral] = []
    @Published var connectedPeripheral: CBPeripheral?
    @Published var logMessages: [String] = []

    private var centralManager: CBCentralManager!

    override init() {
        super.init()
        self.centralManager = CBCentralManager(delegate: self, queue: .main)
    }

    // MARK: - Bluetooth 상태 감시
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            isBluetoothOn = true
            log("✅ Bluetooth ON")
        case .poweredOff:
            isBluetoothOn = false
            log("⚠️ Bluetooth OFF")
        case .unauthorized:
            log("🚫 Bluetooth Unauthorized")
        case .unsupported:
            log("❌ Bluetooth Unsupported")
        default:
            log("ℹ️ Unknown Bluetooth State")
        }
    }

    // MARK: - 스캔
    func startScan() {
        peripherals.removeAll()
        log("🔍 Scanning for peripherals...")
        centralManager.scanForPeripherals(
            withServices: [CBUUID(string: "1234")],
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: true]
        )
    }

    func stopScan() {
        centralManager.stopScan()
        log("🛑 Scan stopped.")
    }

    func centralManager(_ central: CBCentralManager,
                        didDiscover peripheral: CBPeripheral,
                        advertisementData: [String : Any],
                        rssi RSSI: NSNumber) {
        if !peripherals.contains(peripheral) {
            peripherals.append(peripheral)
        }
        log("📡 Found: \(peripheral.name ?? "Unknown") RSSI: \(RSSI)")
    }

    // MARK: - 연결 및 해제
    func connect(to peripheral: CBPeripheral) {
        stopScan()
        connectedPeripheral = peripheral
        centralManager.connect(peripheral, options: nil)
        peripheral.delegate = self
        log("🔗 Connecting to \(peripheral.name ?? "Unknown")")
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        log("✅ Connected to \(peripheral.name ?? "Unknown")")
        peripheral.discoverServices(nil)
    }

    func disconnect() {
        if let peripheral = connectedPeripheral {
            centralManager.cancelPeripheralConnection(peripheral)
            log("🔌 Disconnected from \(peripheral.name ?? "Unknown")")
            connectedPeripheral = nil
        }
    }

    // MARK: - 서비스 탐색
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        for service in peripheral.services ?? [] {
            log("🧩 Service found: \(service.uuid)")
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didDiscoverCharacteristicsFor service: CBService,
                    error: Error?) {
        for characteristic in service.characteristics ?? [] {
            log("📶 Characteristic: \(characteristic.uuid)")
            if characteristic.properties.contains(.read) {
                peripheral.readValue(for: characteristic)
            }
            if characteristic.properties.contains(.notify) {
                peripheral.setNotifyValue(true, for: characteristic)
            }
        }
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didUpdateValueFor characteristic: CBCharacteristic,
                    error: Error?) {
        if let value = characteristic.value {
            log("📥 Data received: \(value.map { String(format: "%02hhx", $0) }.joined())")
        }
    }

    // MARK: - 로그 기록
    private func log(_ message: String) {
        DispatchQueue.main.async {
            withAnimation {
                self.logMessages.append("[\(DateFormatter.localizedString(from: Date(), dateStyle: .none, timeStyle: .medium))] \(message)")
            }
        }
        print(message)
    }
}
