//
//  MobiliteasyBaseApp.swift
//  MaeumOn
//
//  Created by (주)스쿱 개발팀 팀장 on 12/4/25.
//

import SwiftUI

@main
struct MobiliteasyBaseApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
