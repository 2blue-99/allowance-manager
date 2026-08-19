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

android {
    namespace = "com.allowance.manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.allowance.manager"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        // CI(태그 빌드)에서 -PappVersionName 으로 태그 버전 주입, 없으면 기본값
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0.0"
    }

    // release 서명: CI는 환경변수, 로컬은 keystore.properties(선택, gitignore)에서 읽는다.
    // 둘 다 없으면 서명 미설정 → 로컬에서 키 없이도 빌드는 그대로 동작.
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
    }
    val releaseStorePath = System.getenv("KEYSTORE_PATH") ?: keystoreProps.getProperty("storeFile")

    signingConfigs {
        create("release") {
            if (releaseStorePath != null) {
                storeFile = file(releaseStorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
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
            // 서명 정보(CI 환경변수 or keystore.properties)가 있을 때만 release 서명 적용
            if (releaseStorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
