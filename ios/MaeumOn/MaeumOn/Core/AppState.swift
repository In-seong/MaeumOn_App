//
//  AppState.swift
//  NolUniverseCustomer
//
//  Created by scoop dev on 12/8/25.
//

import Combine

class AppState: ObservableObject {
    static let shared = AppState()
    
    // qr 기능 추가할 것 인지
    let USE_QR_FUNCTION: Bool = true
    
}
