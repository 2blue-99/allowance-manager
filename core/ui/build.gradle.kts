plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.allowance.manager.core.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
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
    }
}

dependencies {
    // 디자인 시스템(토큰·기초 컴포넌트)을 api로 재노출 → core:ui를 쓰는 모듈이 토큰에 바로 접근
    api(project(":core:design-system"))
    // presentation 공통 인프라(BaseViewModel 등)도 재노출
    api(project(":core:common"))
    // 도메인 모델(Transaction 등)을 다루는 공용 컴포넌트를 위해 재노출
    api(project(":core:domain"))
    // 분석 로거(AnalyticsHelper·LocalAnalyticsHelper) 재노출 → core:ui 쓰는 피처가 바로 사용
    api(project(":core:analytics"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.timber)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
