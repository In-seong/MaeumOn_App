plugins {
    alias(libs.plugins.android.application)
    // id("com.google.gms.google-services") // Firebase 나중에 추가
}

android {
    namespace = "com.spoon.maeumon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.spoon.maeumon"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "version"
    productFlavors {
        create("user") {
            dimension = "version"
            applicationId = "com.spoon.maeumon.user"
            versionCode = 1
            versionName = "1.0.0"
        }
        create("agent") {
            dimension = "version"
            applicationId = "com.spoon.maeumon.agent"
            versionCode = 1
            versionName = "1.0.0"
        }
        create("admin") {
            dimension = "version"
            applicationId = "com.spoon.maeumon.admin"
            versionCode = 1
            versionName = "1.0.0"
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true // 코드 난독화
            isShrinkResources = true    // 사용하지 않는 리소스 제거
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["appName"] = "@string/app_name"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_round"
            // NDK 빌드 시 디버그 심볼 전체 포함
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appName"] = "@string/app_name_dev"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_round"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {
    // RxJava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)

    implementation(libs.localbroadcastmanager)
    // Beacon
    implementation(libs.android.beacon.library)

    // 인앱 업데이트
    implementation(libs.app.update)

    implementation(libs.glide) // gif 로드
    implementation(libs.barcode.scanning) //---qr---
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.webkit) //---webview---
    implementation(libs.appcompat.resources)
    // implementation(libs.firebase.messaging) // FCM - Firebase 나중에 추가
    implementation(libs.play.services.location)  // 위치
    implementation(libs.activity.v180) // 설정페이지에서 앱 돌아올때 감지

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
