package com.allowance.manager.core.domain.util

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * '오늘' 날짜 스트림 — 오래 사는 ViewModel(홈·월별·통계 탭)이 자정마다 사이클을 다시 구독할 때 쓴다.
 *
 * [todayTicks]를 ViewModel 에서 직접 부르지 않고 이 인터페이스로 주입받는 이유: 무한 delay 루프는
 * 테스트 디스패처(가상 시간)에서 영원히 끝나지 않는다 → 테스트는 한 번만 방출하는 가짜를 넣는다.
 */
interface TodayTicker {
    fun ticks(): Flow<LocalDate>
}
