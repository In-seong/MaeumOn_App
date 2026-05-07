//
//  NFCReader.swift
//  MaeumOn
//
//  Created by scoop dev on 11/3/25.
//

import SwiftUI
import CoreNFC
import Combine

class NFCReader: NSObject, ObservableObject, NFCNDEFReaderSessionDelegate {
    @Published var data: String = ""
    @Published var status: NFCState = .none
    @Published var message: String = ""
    enum NFCState {
        case success, failure, none
    }
    var session: NFCNDEFReaderSession?
    
    func clear() {
        data = ""
        status = .none
        message = ""
    }
    func success(message: String) {
        status = .success
        self.message = message
    }
    func error(message: String) {
        status = .failure
        self.message = message
    }
    func beginScanning() {
        guard NFCNDEFReaderSession.readingAvailable else {
            error(message: "❌ NFC 스캔을 지원하지 않습니다.")
            return
        }
        clear()
        session = NFCNDEFReaderSession(delegate: self, queue: nil, invalidateAfterFirstRead: true)
        session?.alertMessage = "탑승하는 노선의 NFC 태그를 스캔해 주세요."
        session?.begin()
    }
    
    func stopScanning() {
        session?.invalidate()
        session = nil
        clear()
    }
    
    // ✅ 세션 활성화 시 호출
    func readerSessionDidBecomeActive(_ session: NFCNDEFReaderSession) {
        print("readerSessionDidBecomeActive - 세션이 활성화되었습니다.")
    }
    
    // ✅ 스캔 성공 시 호출
    func readerSession(_ session: NFCNDEFReaderSession, didDetectNDEFs messages: [NFCNDEFMessage]) {
        for message in messages {
            for record in message.records {
                if let text = String(data: record.payload, encoding: .utf8) {
                    print("📥 태그 데이터:", text)
                }
            }
        }
    }
    // ✅ 태그 직접 감지 시 (세밀한 제어 가능)
    func readerSession(_ session: NFCNDEFReaderSession, didDetect tags: [NFCNDEFTag]) {
        guard let firstTag = tags.first else { return }
        
        if tags.count > 1 {
            session.alertMessage = "하나의 태그만 가까이 대세요."
            session.restartPolling()
            return
        }
        
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.2) { // 지연 추가
            session.connect(to: firstTag) { error in
                if let error = error {
                    print("❌ 연결 실패:", error.localizedDescription)
                    session.invalidate(errorMessage: "연결이 실패하였습니다. 다시 시도해 주세요.")
                    self.error(message: "연결이 실패")
                    return
                }
                
                firstTag.queryNDEFStatus { status, capacity, error in
                    if let error = error {
                        print("❌ 상태 조회 실패:", error.localizedDescription)
                        session.invalidate(errorMessage: "NFC 태그를 읽을 수 없습니다. 다시 시도해 주세요.")
                        self.error(message: error.localizedDescription)
                        return
                    }
                    
                    print("🔍 NDEF 상태: \(status.rawValue), 용량: \(capacity) bytes")
                    
                    guard status == .readOnly || status == .readWrite else {
                        session.invalidate(errorMessage: "NFC 태그를 읽을 수 없습니다. 다시 시도해 주세요.")
                        self.error(message: "NDEF 미지원 태그")
                        return
                    }
                    
                    firstTag.readNDEF { message, error in
                        if let error = error {
                            print("❌ 읽기 실패:", error.localizedDescription)
                            session.invalidate(errorMessage: "NFC 태그를 읽을 수 없습니다. 다시 시도해 주세요.")
                            self.error(message: "읽기 실패")
                            return
                        }
                        
                        guard let message = message else {
                            print("⚠️ 태그가 비어 있습니다.")
                            session.invalidate(errorMessage: "등록되지 않았거나 분실 처리된 NFC 태그입니다.")
                            self.error(message: "빈 태그")
                            return
                        }
                        
                        for _ in message.records {
                            if let firstRecord = message.records.first {
                                if let text = firstRecord.wellKnownTypeTextPayload().0 {
                                    self.data = text
                                    self.success(message: text)
                                }
                                else {
                                    self.data = ""
                                    self.error(message: "데이터 없음")
                                }
                            }
                        }
                        
//                        session.alertMessage = "읽기 성공"
                        session.invalidate()
                    }
                }
            }
        }
    }
    
    // ❌ 오류 / 종료 시 호출
    func readerSession(_ session: NFCNDEFReaderSession, didInvalidateWithError error: Error) {
        // 사용자가 스캔 취소한 경우 (오류가 아니므로 무시 가능)
        if (error as NSError).code == 201 {
            print("🙋‍♂️ 사용자가 스캔을 취소했습니다.")
            return
        }
        
        print("⚠️ NFC 세션 종료:", error.localizedDescription)
    }
}
