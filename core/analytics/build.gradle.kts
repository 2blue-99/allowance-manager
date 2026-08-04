plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.allowance.manager.core.analytics"
    compileSdk = 35

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
        buildConfig = true   // BuildConfig.DEBUG → 디버그 빌드에서 수집 OFF
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // CompositionLocal(LocalAnalyticsHelper) 제공용 — compose runtime만 사용
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.runtime:runtime")

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.timber)
}
