import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.compose.screenshot)
}

// 릴리즈 서명 정보(keystore.properties, gitignore). 파일 없으면 unsigned 로 빌드(R8 검증엔 충분).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

android {
    namespace = "com.allowance.manager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.allowance.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        // keystore.properties 있을 때만 release 서명 설정 (없으면 unsigned 로 빌드됨)
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // debug는 별도 패키지(com.allowance.manager.dev) → release와 한 폰에 동시 설치 가능,
            // 개발용 Firebase 프로젝트(allowance-manager-dev)로 분리 수집
            applicationIdSuffix = ".dev"
        }
        release {
            // R8: 코드 축소·난독화 (keep 룰은 proguard-rules.pro)
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        compose = true
        buildConfig = true   // BuildConfig.DEBUG (디버그 메뉴 노출 판별)
    }
    // Compose Preview 스크린샷 테스트 (전 화면을 HTML 리포트로 확인)
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(project(":feature:splash"))
    implementation(project(":feature:intro"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:setting"))
    implementation(project(":feature:account"))
    implementation(project(":feature:widget"))
    implementation(project(":core:analytics"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:local"))
    implementation(project(":core:data-store"))
    implementation(project(":core:config"))
    implementation(project(":core:ui"))
    implementation(project(":core:design-system"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.timber)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Preview 스크린샷 테스트용 (DesignGallery)
    screenshotTestImplementation(platform(libs.androidx.compose.bom))
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    testImplementation("junit:junit:4.13.2")
}
