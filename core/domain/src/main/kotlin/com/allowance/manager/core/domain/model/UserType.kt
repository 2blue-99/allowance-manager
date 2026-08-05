package com.allowance.manager.core.domain.model

/**
 * 앱 사용자 유형 — 온보딩 첫 단계에서 선택하고 이후 개인화에 사용.
 * 타겟이 학생~성인으로 섞여 있어 유형에 따라 예산 호칭(용돈/생활비/예산) 등을 맞춘다.
 *
 * - key: DataStore 저장 키 (student/youth/common)
 * - label: 예산 호칭 (화면 노출)
 * - hint: 유형 설명 (온보딩 선택지 보조 문구)
 * - topic: 호칭 뒤 조사 은/는 (예: "월 용돈은 …")
 * - object_: 호칭 뒤 조사 을/를 (예: "용돈을 …" / "생활비를 …")
 * - paydayLabel: 수급일 호칭 (학생=용돈 받는 날 / 직장인·일반=월급일). 설정·온보딩 등 풀네임.
 * - paydayShort: 수급일 짧은 호칭 (학생=용돈날 / 직장인·일반=월급일). 홈 D-day 뱃지 등 좁은 공간.
 */
enum class UserType(
    val key: String,
    val label: String,
    val hint: String,
    val topic: String,
    val objectParticle: String,
    val paydayLabel: String,
    val paydayShort: String,
) {
    STUDENT("student", "용돈", "아직 학생이시라면", "은", "을", "용돈 받는 날", "용돈날"),
    YOUTH("youth", "생활비", "경제활동을 하고 계시다면", "는", "를", "월급일", "월급일"),
    COMMON("common", "예산", "둘 다 애매할 땐", "은", "을", "월급일", "월급일");

    companion object {
        val Default = COMMON

        fun fromKey(key: String?): UserType = entries.firstOrNull { it.key == key } ?: Default
    }
}
