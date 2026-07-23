package com.allowance.manager.core.domain.model

/**
 * 은행 알림의 마스킹 계좌번호와 등록된 계좌번호가 같은 계좌인지 판정하는 규칙.
 *
 * 은행은 계좌번호의 일부만 가리고 **글자 수는 그대로 유지**한다. (예: `941602-**-***318`)
 * 따라서 구분자(-)만 걷어내면 자리별로 정렬해 비교할 수 있다.
 *
 * 등록된 계좌 값은 두 종류가 섞여 들어온다.
 *  - 온보딩/설정에서 사용자가 직접 입력한 값 → 숫자만
 *  - 감지된 거래를 메인으로 승격해 저장한 값 → 마스킹 문자열
 *
 * 그래서 양쪽 어디든 가려진 자리가 있을 수 있고, **한쪽이라도 가려진 자리는 비교에서 제외**한다.
 */
object MaskedAccount {

    private const val MASK = '*'

    /**
     * 저장·비교용 정규화: 구분자(-, 공백 등)를 걷어내고 **숫자와 마스킹 문자만** 남긴다.
     * - 사용자가 직접 입력한 계좌번호 → 하이픈 유무와 무관하게 동일한 숫자열로 저장
     * - 알림에서 복사한 마스킹 패턴 → `*`를 보존해야 자릿수 정렬이 유지됨
     */
    fun normalize(input: String): String =
        input.filter { it.isDigit() || it == MASK }

    private fun canonical(value: String): String = normalize(value)

    /**
     * 두 계좌 표기가 같은 계좌를 가리키는지 판정.
     * 자릿수가 같아야 하며, 양쪽 모두 보이는 자리의 숫자가 전부 일치해야 한다.
     *
     * 가려진 자리만 다른 계좌는 원리상 구분할 수 없다(정보가 없음).
     */
    fun matches(left: String?, right: String?): Boolean {
        if (left.isNullOrBlank() || right.isNullOrBlank()) return false
        val a = canonical(left)
        val b = canonical(right)
        if (a.isEmpty() || a.length != b.length) return false
        return a.indices.all { a[it] == MASK || b[it] == MASK || a[it] == b[it] }
    }
}
