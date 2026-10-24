plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.allowance.manager.core.local"
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
    // Room 쿼리(승격·해제 CASE 문 등)를 JVM 에서 인메모리 DB 로 검증 — Robolectric
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    // Room 스키마 스냅샷 export → DB 스키마 변경 감지(CI 가드)·마이그레이션 검증용.
    // 스키마를 바꾸면 이 폴더의 JSON이 갱신되므로, version↑·Migration 작성 후 함께 커밋해야 한다.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.timber)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}
