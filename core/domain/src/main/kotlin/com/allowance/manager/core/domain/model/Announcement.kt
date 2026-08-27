package com.allowance.manager.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 홈 진입 시 띄우는 공지 다이얼로그 내용 — Remote Config(`announcement`)로 내려받는다.
 *
 * 같은 공지를 반복해서 띄우지 않도록 [id]를 기준으로 "본 공지"를 기록한다(마지막으로 확인한 id 저장).
 * `active`가 false면 노출하지 않는다 — 운영에서 값은 남겨둔 채 on/off 하기 위한 스위치.
 *
 * @param id 공지 식별자. 값을 바꾸면 다시 1회 노출된다(예: "2026-08-27-payday").
 * @param title 제목. @param body 본문(`\n` 줄바꿈 지원).
 */
data class Announcement(
    val id: String,
    val title: String,
    val body: String,
) {
    companion object {
        @Serializable
        private data class Payload(
            val id: String = "",
            val active: Boolean = false,
            val title: String = "",
            val body: String = "",
        )

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Remote Config 문자열 → [Announcement]. 다음 중 하나라도면 `null`(노출 안 함):
         * 값이 비었거나 · JSON이 깨졌거나 · `active`가 false거나 · id/제목이 비었다.
         */
        fun parse(raw: String): Announcement? {
            if (raw.isBlank()) return null
            val p = runCatching { json.decodeFromString<Payload>(raw) }.getOrNull() ?: return null
            if (!p.active || p.id.isBlank() || p.title.isBlank()) return null
            return Announcement(id = p.id, title = p.title, body = p.body)
        }
    }
}
