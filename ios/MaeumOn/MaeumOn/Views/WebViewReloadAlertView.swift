//
//  WebViewReloadView.swift
//  MaeumOn
//
//  Created by scoop dev on 11/26/25.
//

import SwiftUI

struct WebViewReloadAlertView: View {
   var onTap: (() -> Void)
   
   var body: some View {
      ZStack {
         Color.white
         VStack(spacing: 10) {
            Image(systemName: "wifi.slash")
               .resizable()
               .scaledToFit()
               .frame(width: 50)
               .foregroundStyle(Color.accent)
            Text("네트워크에 접속할 수 없습니다.\n네트워크 연결 상태를 확인해주세요.")
               .pretendard(size: 14)
               .multilineTextAlignment(.center)
               .lineSpacing(10)
            
            Button {
               onTap()
            }
            label: {
               Text("다시 시도")
                  .pretendard(size: 14, fontColor: .white)
                  .padding(.vertical, 10)
                  .padding(.horizontal, 15)
                  .background(Color.accent)
                  .cornerRadius(50)
            }
         }
      }
   }
}
