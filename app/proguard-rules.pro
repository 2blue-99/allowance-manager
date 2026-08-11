# ==========================================================================
# 내돈지켜 R8/ProGuard keep 룰
# 대부분의 라이브러리(Room·Hilt·Compose·Firebase)는 자체 consumer 룰을 포함하므로
# 여기서는 이 앱에서 리플렉션/직렬화로 접근하는 부분만 명시적으로 보존한다.
# ==========================================================================

# ── Crashlytics: 스택트레이스 가독성(소스·라인) 유지 ──
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# ── kotlinx.serialization (type-safe 네비게이션 라우트가 @Serializable) ──
# @Serializable 클래스와 생성된 serializer 를 보존한다.
-keep @kotlinx.serialization.Serializable class com.allowance.manager.** { *; }
-keepclassmembers class com.allowance.manager.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.allowance.manager.**$$serializer { *; }
-if @kotlinx.serialization.Serializable class com.allowance.manager.**
-keepclassmembers class com.allowance.manager.** {
    static <1>$Companion Companion;
}
-dontnote kotlinx.serialization.**

# ── enum: DB에 name 문자열로 저장 후 valueOf 로 복원(TxScope/TransactionType/TransactionCategory) ──
-keepclassmembers enum com.allowance.manager.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Glance 위젯: 매니페스트 리시버는 자동 보존되나, 액션 콜백은 리플렉션 참조라 보존 ──
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }

# ── Timber ──
-dontwarn org.jetbrains.annotations.**
