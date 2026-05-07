#!/usr/bin/env python3
"""
iOS MaeumOn Project Setup Script
This script provides instructions for manually setting up the Xcode project.

Run this after opening the project in Xcode.
"""

instructions = """
=== MaeumOn iOS 프로젝트 설정 가이드 ===

Xcode에서 MaeumOn.xcodeproj를 열고 다음 단계를 수행하세요:

1. 기존 타겟 삭제:
   - MobiliteasyBase, MobiliteasyLite, KwangdongCustomer 등 기존 타겟 모두 삭제
   
2. 새 타겟 생성 (3개):

   [User 타겟]
   - File > New > Target > iOS App
   - Product Name: User
   - Bundle Identifier: com.spoon.maeumon.user
   - Display Name: 마음온
   - Info.plist: MaeumOn/plist/Info-User.plist
   - Code Signing Entitlements: MaeumOn/entitlements/User.entitlements
   - Swift Compiler - Custom Flags: -D USER
   
   [Agent 타겟]
   - File > New > Target > iOS App
   - Product Name: Agent  
   - Bundle Identifier: com.spoon.maeumon.agent
   - Display Name: 마음온 설계사
   - Info.plist: MaeumOn/plist/Info-Agent.plist
   - Code Signing Entitlements: MaeumOn/entitlements/Agent.entitlements
   - Swift Compiler - Custom Flags: -D AGENT
   
   [Admin 타겟]
   - File > New > Target > iOS App
   - Product Name: Admin
   - Bundle Identifier: com.spoon.maeumon.admin
   - Display Name: 마음온 관리자
   - Info.plist: MaeumOn/plist/Info-Admin.plist
   - Code Signing Entitlements: MaeumOn/entitlements/Admin.entitlements
   - Swift Compiler - Custom Flags: -D ADMIN

3. 각 타겟에 소스 파일 추가:
   - MaeumOn 폴더의 모든 Swift 파일을 각 타겟에 포함

4. CocoaPods 설치:
   cd /Users/scoop/MaeumOn/MaeumOn_APP/iOS/MaeumOn
   pod install

5. 빌드 테스트:
   - 각 scheme(User, Agent, Admin)을 선택하고 빌드 확인
"""

print(instructions)
