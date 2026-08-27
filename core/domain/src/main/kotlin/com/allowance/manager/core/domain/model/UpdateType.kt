package com.allowance.manager.core.domain.model

/**
 * 업데이트 팝업 종류. 공통 팝업을 쓰되 이 타입으로 문구·버튼·재노출 규칙이 갈린다.
 *
 * - [FORCED] 필수 업데이트 — 닫기 없이 업데이트 버튼만, 매번 노출(업데이트해야 벗어남).
 * - [RECOMMEND] 추천 업데이트 — 확인/업데이트 두 버튼, **하루 1회**만 노출.
 *
 * 둘 다 해당되면 [FORCED]가 우선한다.
 */
enum class UpdateType { FORCED, RECOMMEND }
