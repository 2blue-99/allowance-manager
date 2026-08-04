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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.allowance.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
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
