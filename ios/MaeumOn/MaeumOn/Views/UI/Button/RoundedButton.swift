//
//  ActiveButton.swift
//  MaeumOn
//
//  Created by scoop dev on 11/7/25.
//

import SwiftUI

struct RoundedButton: View {
    let text: String
    var cornerRadius: CGFloat = 15
    var color: Color = .accentColor
    var onTap: (() -> Void)?
    
    var body: some View {
        Button {
            onTap?()
        } label: {
            Text(text)
                .font(.custom("Pretendard-SemiBold", fixedSize: 17))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    color
                        .cornerRadius(cornerRadius)
                )
        }
    }
}
