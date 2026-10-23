package com.allowance.manager.service

/**
 * 같은 알림의 중복 전달을 거른다.
 *
 * 삼성 One UI 는 모든 알림을 자동 그룹(Aggregate_NormalNotificationSection)으로 묶으면서 같은 알림을
 * **같은 밀리초에 한 번 더** 리스너에 전달한다(키·postTime·내용 전부 동일). 그대로 두면 금액 알림마다
 * 거래가 두 줄 생긴다. 알림 갱신(내용이나 postTime 이 바뀜)은 다른 알림으로 보고 통과시킨다.
 *
 * 기억은 최근 [capacity]건만 (LRU). 리스너가 살아 있는 동안만 유효 — 프로세스가 죽으면 비워지지만,
 * 중복 전달은 수 ms 안에 오므로 충분하다.
 */
class NotificationDeduper(private val capacity: Int = 200) {

    private val seen = object : LinkedHashMap<String, Boolean>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > capacity
    }

    /**
     * 처음 보는 알림이면 true (기록하고 통과), 이미 본 알림이면 false (무시).
     * @param key 알림 키(sbn.key) — 패키지·id·태그·사용자 조합
     * @param postTime 게시 시각(sbn.postTime)
     * @param content 파싱에 쓰는 제목+본문 — 같은 키로 내용이 바뀌어 다시 오면(갱신) 새 알림으로 본다
     */
    fun shouldProcess(key: String, postTime: Long, content: String): Boolean {
        val id = "$key|$postTime|${content.hashCode()}"
        synchronized(seen) {
            if (seen.containsKey(id)) return false
            seen[id] = true
            return true
        }
    }
}
