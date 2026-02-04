# 보험청구 ON Mobile App

> 보험청구 ON 모바일 앱 (Android / iOS) - 하나의 코드베이스로 사용자/설계사/관리자 앱을 관리합니다.

---

## 목차

1. [개요](#1-개요)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [Flavor/Target 목록 및 설정](#3-flavortarget-목록-및-설정)
4. [Android 빌드](#4-android-빌드)
5. [iOS 빌드](#5-ios-빌드)
6. [리소스 수정 방법](#6-리소스-수정-방법)
7. [새 Flavor/Target 추가하기](#7-새-flavortarget-추가하기)
8. [배포 방법](#8-배포-방법)
9. [트러블슈팅](#9-트러블슈팅)
10. [빠른 참조](#10-빠른-참조)

---

## 1. 개요

### Product Flavor / Target이란?

**하나의 코드베이스로 여러 버전의 앱**을 빌드하는 기능입니다.

```
동일한 소스코드 → 사용자 앱, 설계사 앱, 관리자 앱 각각 다른 앱으로 빌드
```

### 리소스 병합 규칙

빌드 시 **flavor 리소스가 main 리소스를 덮어씁니다**:

```
예: userRelease 빌드 시 (Android)

1. main/res/*        ← 기본 리소스 로드
2. user/res/*        ← user에 있는 리소스로 덮어씀
3. 최종 APK 생성
```

### Build Variant (Android)

**Flavor + BuildType = Build Variant**

| Flavor | BuildType | Build Variant | 용도 |
|--------|-----------|---------------|------|
| user | debug | userDebug | 개발/테스트 |
| user | release | userRelease | 스토어 배포 |
| agent | debug | agentDebug | 개발/테스트 |
| agent | release | agentRelease | 스토어 배포 |
| admin | debug | adminDebug | 개발/테스트 |
| admin | release | adminRelease | 스토어 배포 |

---

## 2. 프로젝트 구조

```
MaeumOn_App/
├── android/
│   └── MaeumOn/                    # Android 프로젝트
│       ├── app/
│       │   ├── src/
│       │   │   ├── main/           # 공통 코드 & 리소스 (기본값)
│       │   │   │   ├── java/com/spoon/maeumon/
│       │   │   │   │   ├── feature/
│       │   │   │   │   │   ├── splash/      # 스플래시 화면
│       │   │   │   │   │   ├── webView/     # 웹뷰 액티비티
│       │   │   │   │   │   ├── qr/          # QR 스캔
│       │   │   │   │   │   ├── nfc/         # NFC 태그
│       │   │   │   │   │   ├── permission/  # 권한 관리
│       │   │   │   │   │   └── common/      # 공통 유틸
│       │   │   │   ├── res/
│       │   │   │   │   ├── drawable/        # 이미지 리소스
│       │   │   │   │   ├── layout/          # 레이아웃
│       │   │   │   │   ├── mipmap-*/        # 앱 아이콘
│       │   │   │   │   └── values/          # 문자열, 색상 등
│       │   │   │   └── AndroidManifest.xml
│       │   │   │
│       │   │   ├── user/           # 사용자 앱 전용
│       │   │   │   ├── java/.../WebConstants.java  # 사용자 URL
│       │   │   │   └── res/values/strings.xml      # 앱 이름
│       │   │   │
│       │   │   ├── agent/          # 설계사 앱 전용
│       │   │   │   ├── java/.../WebConstants.java  # 설계사 URL
│       │   │   │   └── res/values/strings.xml      # 앱 이름
│       │   │   │
│       │   │   └── admin/          # 관리자 앱 전용
│       │   │       ├── java/.../WebConstants.java  # 관리자 URL
│       │   │       └── res/values/strings.xml      # 앱 이름
│       │   │
│       │   └── build.gradle.kts
│       ├── build.gradle.kts
│       ├── settings.gradle.kts
│       ├── gradle.properties
│       └── gradlew
│
├── ios/
│   └── MaeumOn/                    # iOS 프로젝트
│       ├── MaeumOn/
│       │   ├── Views/              # SwiftUI 뷰
│       │   ├── plist/              # 타겟별 Info.plist
│       │   └── entitlements/       # 타겟별 권한 설정
│       ├── MaeumOn.xcodeproj
│       ├── MaeumOn.xcworkspace
│       └── Podfile
│
└── README.md                       # 이 문서
```

---

## 3. Flavor/Target 목록 및 설정

### 3.1 전체 Flavor/Target 현황

| Flavor/Target | 앱 이름 | 패키지/번들 ID | versionCode | versionName |
|---------------|--------|----------------|-------------|-------------|
| user | 보험청구 ON | com.spoon.maeumon.user | 1 | 1.0.0 |
| agent | 보험청구 ON 설계사 | com.spoon.maeumon.agent | 1 | 1.0.0 |
| admin | 보험청구 ON 관리자 | com.spoon.maeumon.admin | 1 | 1.0.0 |

> **Debug 빌드**: 패키지명에 `.dev` 접미사 추가, 앱 이름에 `개발` 접미사 추가

### 3.2 WebView URL 설정

| Flavor/Target | Debug URL | Release URL |
|---------------|-----------|-------------|
| user | https://devuser.podo-life.co.kr/ | https://user.podo-life.co.kr/ |
| agent | https://devagent.podo-life.co.kr/ | https://agent.podo-life.co.kr/ |
| admin | https://devadmin.podo-life.co.kr/ | https://admin.podo-life.co.kr/ |

### 3.3 커스텀 리소스 현황

| Flavor/Target | 앱 아이콘 | 스플래시 | WebView URL | 앱 이름 |
|---------------|----------|----------|-------------|--------|
| user | ❌ main 사용 | ❌ main 사용 | ✅ 커스텀 | ✅ 커스텀 |
| agent | ❌ main 사용 | ❌ main 사용 | ✅ 커스텀 | ✅ 커스텀 |
| admin | ❌ main 사용 | ❌ main 사용 | ✅ 커스텀 | ✅ 커스텀 |

---

## 4. Android 빌드

### 4.1 요구사항

- Android Studio (Arctic Fox 이상)
- JDK 11+
- Min SDK: 24 (Android 7.0)
- Target SDK: 36

### 4.2 Android Studio에서 빌드

1. Android Studio에서 `MaeumOn_App/android/MaeumOn` 폴더 열기
2. Gradle Sync 완료 대기
3. 좌측 하단 **Build Variants** 패널에서 variant 선택:
   - `userDebug` / `userRelease` - 보험청구 ON
   - `agentDebug` / `agentRelease` - 보험청구 ON 설계사
   - `adminDebug` / `adminRelease` - 보험청구 ON 관리자
4. Run 버튼 클릭 또는 `Build > Build Bundle(s) / APK(s)`

### 4.3 터미널에서 빌드

```bash
cd android/MaeumOn

# Debug APK 빌드
./gradlew assembleUserDebug      # 보험청구 ON Debug
./gradlew assembleAgentDebug     # 보험청구 ON 설계사 Debug
./gradlew assembleAdminDebug     # 보험청구 ON 관리자 Debug

# Release APK 빌드
./gradlew assembleUserRelease    # 보험청구 ON Release
./gradlew assembleAgentRelease   # 보험청구 ON 설계사 Release
./gradlew assembleAdminRelease   # 보험청구 ON 관리자 Release

# 모든 variant 빌드
./gradlew assemble

# AAB (App Bundle) 빌드 - 스토어 배포용
./gradlew bundleUserRelease
./gradlew bundleAgentRelease
./gradlew bundleAdminRelease

# 프로젝트 클린
./gradlew clean

# 연결된 기기에 설치
./gradlew installUserDebug
```

### 4.4 빌드 결과물 위치

```
app/build/outputs/
├── apk/
│   ├── user/
│   │   ├── debug/app-user-debug.apk
│   │   └── release/app-user-release.apk
│   ├── agent/
│   │   ├── debug/app-agent-debug.apk
│   │   └── release/app-agent-release.apk
│   └── admin/
│       ├── debug/app-admin-debug.apk
│       └── release/app-admin-release.apk
└── bundle/
    ├── userRelease/app-user-release.aab
    ├── agentRelease/app-agent-release.aab
    └── adminRelease/app-admin-release.aab
```

---

## 5. iOS 빌드

### 5.1 요구사항

- macOS
- Xcode 15+
- CocoaPods
- Min iOS: 15.6

### 5.2 초기 설정

```bash
cd ios/MaeumOn

# CocoaPods 설치 (미설치시)
sudo gem install cocoapods

# Pod 설치
pod install
```

### 5.3 Xcode에서 빌드

1. `MaeumOn.xcworkspace` 열기 (**xcodeproj가 아님!**)
2. 상단 Scheme 선택:
   - `User` - 보험청구 ON
   - `Agent` - 보험청구 ON 설계사
   - `Admin` - 보험청구 ON 관리자
3. 대상 디바이스/시뮬레이터 선택
4. `Cmd + B` (빌드) 또는 `Cmd + R` (실행)

### 5.4 터미널에서 빌드

```bash
cd ios/MaeumOn

# 시뮬레이터용 빌드
xcodebuild -workspace MaeumOn.xcworkspace -scheme User -sdk iphonesimulator build

# 실기기용 빌드
xcodebuild -workspace MaeumOn.xcworkspace -scheme User -sdk iphoneos build

# Archive (배포용)
xcodebuild -workspace MaeumOn.xcworkspace -scheme User \
  -sdk iphoneos -configuration Release \
  archive -archivePath ./build/User.xcarchive
```

---

## 6. 리소스 수정 방법

### 6.1 앱 이름 변경

#### Android
**파일 위치**: `app/src/{flavor}/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">새로운 앱 이름</string>
    <string name="app_name_dev">새로운 앱 이름 개발</string>
</resources>
```

#### iOS
Xcode > Target > Build Settings > `PRODUCT_NAME` 수정

---

### 6.2 앱 아이콘 변경

#### Android (Adaptive Icon)

**필요한 파일**:
```
app/src/{flavor}/res/
├── drawable/
│   └── ic_launcher_foreground.xml    ← 전경 이미지
├── values/
│   └── ic_launcher_background.xml    ← 배경 색상
└── mipmap-anydpi-v26/
    ├── ic_launcher.xml
    └── ic_launcher_round.xml
```

**권장 방법**: Android Studio > 우클릭 > `New` > `Image Asset`

#### iOS
Xcode > Assets.xcassets > AppIcon 교체

---

### 6.3 WebView URL 변경

#### Android
**파일 위치**: `app/src/{flavor}/java/com/spoon/maeumon/feature/common/value/WebConstants.java`

```java
package com.spoon.maeumon.feature.common.value;

public class WebConstants {
   // Debug 빌드용 URL
   public static final String DEBUG_WebView_URL = "https://dev.example.com/";

   // Release 빌드용 URL
   public static final String WebView_URL = "https://example.com/";
}
```

#### iOS
**파일 위치**: `MaeumOn/Views/MainView.swift`

Swift 전처리기 플래그로 분기 처리

---

### 6.4 버전 정보 변경

#### Android
**파일 위치**: `app/build.gradle.kts`

```kotlin
productFlavors {
    create("user") {
        dimension = "version"
        applicationId = "com.spoon.maeumon.user"
        versionCode = 2          // 스토어 업로드마다 +1
        versionName = "1.1.0"    // 사용자에게 표시되는 버전
    }
}
```

> **중요**: 스토어에 업로드할 때마다 `versionCode`를 반드시 증가시켜야 합니다.

#### iOS
Xcode > Target > General > Version / Build 수정

---

## 7. 새 Flavor/Target 추가하기

### 7.1 Android - 새 Flavor 추가

#### 1. build.gradle.kts에 Flavor 추가

```kotlin
productFlavors {
    // 기존 flavors...

    create("newclient") {
        dimension = "version"
        applicationId = "com.spoon.maeumon.newclient"
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

#### 2. 디렉토리 구조 생성

```bash
mkdir -p app/src/newclient/res/values
mkdir -p app/src/newclient/java/com/spoon/maeumon/feature/common/value
```

#### 3. 필수 파일 생성

**strings.xml**:
```xml
<resources>
    <string name="app_name">새 클라이언트</string>
    <string name="app_name_dev">새 클라이언트 개발</string>
</resources>
```

**WebConstants.java**:
```java
package com.spoon.maeumon.feature.common.value;

public class WebConstants {
   public static final String DEBUG_WebView_URL = "https://dev.newclient.com/";
   public static final String WebView_URL = "https://newclient.com/";
}
```

#### 4. Gradle Sync 및 빌드 테스트

```bash
./gradlew assembleNewclientDebug
```

### 7.2 iOS - 새 Target 추가

1. Xcode에서 File > New > Target > iOS > App
2. Product Name, Bundle ID 설정
3. Build Settings에서 Info.plist, Entitlements 경로 설정
4. Swift Compiler Flags 추가 (예: `-D NEWCLIENT`)
5. 소스 파일 Target Membership 설정

---

## 8. 배포 방법

### 8.1 Android - Google Play Store

1. **AAB 빌드**: `./gradlew bundleUserRelease`
2. **서명**: Android Studio > Build > Generate Signed Bundle
3. **Play Console 업로드**: https://play.google.com/console

### 8.2 iOS - App Store

1. Xcode > Product > Archive
2. Organizer > Distribute App
3. App Store Connect 선택

### 8.3 배포 체크리스트

- [ ] versionCode/Build 증가 확인
- [ ] versionName/Version 업데이트 확인
- [ ] Release 빌드로 테스트 완료
- [ ] 서명된 AAB/IPA 생성
- [ ] 스토어 스크린샷 업데이트 (필요시)
- [ ] 출시 노트 작성

---

## 9. 트러블슈팅

### Android

#### 리소스가 반영되지 않음
```bash
./gradlew clean
./gradlew assembleUserDebug
```

#### Flavor가 Build Variants에 안 보임
- `File` > `Sync Project with Gradle Files`
- Android Studio 재시작

#### versionCode 오류 (스토어 업로드 실패)
- `build.gradle.kts`에서 해당 flavor의 `versionCode` 증가
- 이전에 업로드한 버전보다 높아야 함

### iOS

#### Pod 설치 오류
```bash
pod deintegrate
pod install
```

#### 서명 오류
- Signing & Capabilities에서 Team 설정 확인
- Provisioning Profile 재생성

#### 빌드 오류
- `Product` > `Clean Build Folder` (Cmd + Shift + K)

---

## 10. 빠른 참조

### 자주 사용하는 명령어

```bash
# Android
./gradlew assemble{Flavor}Debug      # Debug APK
./gradlew assemble{Flavor}Release    # Release APK
./gradlew bundle{Flavor}Release      # Release AAB
./gradlew install{Flavor}Debug       # 기기에 설치
./gradlew clean                      # 클린 빌드

# iOS
xcodebuild -workspace MaeumOn.xcworkspace -scheme {Target} build
```

### Android 파일 위치 요약

| 항목 | 파일 위치 |
|------|----------|
| 앱 이름 | `app/src/{flavor}/res/values/strings.xml` |
| 앱 아이콘 | `app/src/{flavor}/res/drawable/ic_launcher_foreground.xml` |
| 아이콘 배경색 | `app/src/{flavor}/res/values/ic_launcher_background.xml` |
| 스플래시 이미지 | `app/src/main/res/drawable/img_splash.png` |
| WebView URL | `app/src/{flavor}/java/.../WebConstants.java` |
| 버전 정보 | `app/build.gradle.kts` (productFlavors 섹션) |
| Manifest | `app/src/main/AndroidManifest.xml` |

### iOS 파일 위치 요약

| 항목 | 파일 위치 |
|------|----------|
| WebView URL | `MaeumOn/Views/MainView.swift` |
| Info.plist | `MaeumOn/plist/Info-{Target}.plist` |
| Entitlements | `MaeumOn/entitlements/{Target}.entitlements` |
| Podfile | `Podfile` |

---

## 현재 상태 (참고)

- **Firebase**: 비활성화 상태 (나중에 google-services.json 추가 필요)
- **핸드폰 인증**: 비활성화 (바로 웹뷰로 이동)

---

*문서 최종 업데이트: 2025-02-04*