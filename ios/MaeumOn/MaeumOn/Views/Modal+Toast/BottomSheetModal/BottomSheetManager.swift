//
//  BottomSheetModal.swift
//  MaeumOn
//
//  Created by scoop dev on 10/27/25.
//

import SwiftUI
import Combine

final class BottomSheetManager: ObservableObject {
    static let shared = BottomSheetManager()

    @Published var isPresented: Bool = false
    @Published var content: AnyView = AnyView(EmptyView())
    @Published var blockDismiss: Bool = false
    private init() {}

    func present<Content: View>(blockDismiss: Bool=false, @ViewBuilder content: @escaping () -> Content) {
        withAnimation(.spring()) {
            self.content = AnyView(content())
            self.blockDismiss = blockDismiss
            self.isPresented = true
        }
    }

    func dismiss() {
        self.isPresented = false
    }
}
