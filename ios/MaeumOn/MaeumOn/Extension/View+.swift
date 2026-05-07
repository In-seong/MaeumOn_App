//
//  View+.swift
//  MaeumOn
//
//  Created by scoop dev on 11/11/25.
//

import SwiftUI

// MARK: - 특정 코너만 둥글게 처리하는 Shape
struct RoundedCorner: Shape {
    var radius: CGFloat = 0
    var corners: UIRectCorner = .allCorners
    
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

// MARK: - View Extension
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
    
    func roundedEdge(width: CGFloat = 4, color: Color = .primary, cornerRadius: CGFloat = 8) -> some View {
        modifier(RoundedEdge(width: width, color: color, cornerRadius: cornerRadius))
        
    }
}

// rounded edge 둥근 테두리
struct RoundedEdge: ViewModifier {
    let width: CGFloat
    let color: Color
    let cornerRadius: CGFloat

    func body(content: Content) -> some View {
        content.cornerRadius(cornerRadius - width)
            .padding(width)
            .background(color)
            .cornerRadius(cornerRadius)
    }
}
