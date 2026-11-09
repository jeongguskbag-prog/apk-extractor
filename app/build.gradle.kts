plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jeongguskbag.apkvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jeongguskbag.apkvault"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    // 출시용 서명: 키 파일과 비밀번호는 저장소에 넣지 않고 환경 변수로만 받음
    // (GitHub Actions 에서는 Secrets → .github/workflows/release.yml 이 넘겨줌).
    // 값이 없으면 release 빌드는 서명 없이 만들어짐 (빌드 확인용).
    val releaseKeystore = System.getenv("APKVAULT_KEYSTORE_FILE")
    signingConfigs {
        if (!releaseKeystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("APKVAULT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("APKVAULT_KEY_ALIAS")
                keyPassword = System.getenv("APKVAULT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (!releaseKeystore.isNullOrBlank()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
