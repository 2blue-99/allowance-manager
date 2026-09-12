package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.Cycle
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 수급일 사이클 저장소 — 사이클 계산·예산·규칙일의 **단일 진입점**.
 *
 * 사이클은 `cycles` 테이블의 행이다. 읽기 요청이 들어오면 관문이 먼저 돈다:
 * 마지막 행이 오늘을 못 덮으면 규칙일로 새 행을 이어 만들고(예산은 직전 값 이월),
 * 가장 오래된 거래가 첫 행보다 과거면 뒤로도 채운다(백필) — 모든 거래가 어떤 사이클엔가 속하도록.
 *
 * 온보딩 전(행이 하나도 없을 때)은 기본 규칙일로 **계산만** 해서 돌려준다(저장하지 않음).
 */
interface CycleRepository {

    /** [date]가 속한 사이클 관찰 — 행 변경(예산·규칙·경계) 시 재방출. */
    fun observeCycleAt(date: LocalDate = LocalDate.now()): Flow<Cycle>

    /** [date]가 속한 사이클 1회 조회. */
    suspend fun cycleAt(date: LocalDate = LocalDate.now()): Cycle

    /** 전체 사이클 관찰 (오래된 → 최신). 통계·피커용. */
    fun observeAll(): Flow<List<Cycle>>

    /** 전체 사이클 1회 조회 (오래된 → 최신). */
    suspend fun getAll(): List<Cycle>

    /** 그 사이클의 예산 변경(그 행만). 이후 새로 생기는 사이클은 이 값을 이월한다. */
    suspend fun setBudget(cycleStart: LocalDate, amount: Long)

    /** 그 사이클의 예산. 행이 없으면 0. */
    suspend fun budgetFor(cycleStart: LocalDate): Long

    /**
     * "이번 회차만 이 날 받아요" — 그 사이클의 끝(다음 받는 날)을 [end]로 **고정**.
     * 규칙일은 그대로라 그 날이 지나면 다음 회차는 다시 규칙으로 계산된다. 규칙일을 바꾸면 고정이 풀린다.
     */
    suspend fun setCycleEnd(cycleStart: LocalDate, end: LocalDate)

    /** 온보딩: 첫 사이클을 심는다 — 오늘이 속한 사이클을 규칙일로 계산해 행으로 저장. */
    suspend fun init(payday: Int, today: LocalDate = LocalDate.now())

    /**
     * "이번 월급일은 사실 [boundary]였다" — 현재 회차의 **시작만** 옮긴다. 끝(다음 월급일)·예산·규칙은 그대로.
     *
     * 규칙으로 끝을 다시 계산하지 않는다 — 말일 규칙에 8/20을 넣었을 때 "8/20 다음 말일 = 8/31"로 11일짜리
     * 회차가 생기면 안 된다. 8/20 지급은 8/31 지급을 대신한 것이고, 다음 월급일은 여전히 9/30이다.
     * 그래서 새 행의 끝은 시작으로 유도되지 않는 값 → 고정 표시(endPinned)로 관문 재계산에서 제외한다.
     * 직전 회차의 끝은 [boundary]로 맞춰져 빈틈·겹침이 없다.
     */
    suspend fun moveCycleStart(boundary: LocalDate, today: LocalDate = LocalDate.now())

    /**
     * 월급일 변경 — [boundary]("이번에 받은 날")부터 새 사이클이 시작하고 이후 규칙은 [payday].
     *
     * [boundary] 이후에 시작하던 행은 대체되어 삭제되고, 직전 행의 끝이 [boundary]로 맞춰져
     * 빈틈·겹침이 생기지 않는다. 경계를 그대로 두면 규칙일만 바뀌어 끝이 다시 계산된다.
     */
    suspend fun changePayday(boundary: LocalDate, payday: Int, today: LocalDate = LocalDate.now())
}
