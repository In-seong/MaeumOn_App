//
//  Text.swift
//  MaeumOn
//
//  Created by scoop dev on 11/7/25.
//

import SwiftUI

extension Text {
    enum PretendardWeight: String {
        case regular = "Pretendard-Regular"
        case medium = "Pretendard-Medium"
        case semibold = "Pretendard-SemiBold"
        case bold = "Pretendard-Bold"
    }

    /// Pretendard 폰트 적용 함수
    func pretendard(
        _ weight: PretendardWeight = .regular,
        size: CGFloat,
        fontColor: FontColor = .main
    ) -> some View {
        self
            .font(.custom(weight.rawValue, fixedSize: size))
            .foregroundStyle(fontColor.color)
    }
    func pretendard(
        _ weight: PretendardWeight = .regular,
        size: CGFloat,
        color: Color
    ) -> some View {
        self
            .font(.custom(weight.rawValue, fixedSize: size))
            .foregroundStyle(color)
    }
    
}

extension Text {
    enum FontColor {
        case main
        case brightGray
        case gray
        case red
        case white
        case custom(String)
        
        var color: Color {
            switch self {
            case .main:
                return Color("fontMain")
            case .brightGray:
                return Color("fontBrightGray")
            case .gray:
                return Color("fontGray")
            case .red:
                return Color("fontRed")
            case .white:
                return Color.white
            case let .custom(name):
                return Color(name)
            }
        }
    }
}

extension Color {
    init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        // #FFF → #FFFFFF 변환
        if hexSanitized.count == 3 {
            let chars = Array(hexSanitized)
            hexSanitized = "\(chars[0])\(chars[0])\(chars[1])\(chars[1])\(chars[2])\(chars[2])"
        }

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }

        let red = Double((rgb >> 16) & 0xFF) / 255.0
        let green = Double((rgb >> 8) & 0xFF) / 255.0
        let blue = Double(rgb & 0xFF) / 255.0

        self.init(red: red, green: green, blue: blue)
    }
}
