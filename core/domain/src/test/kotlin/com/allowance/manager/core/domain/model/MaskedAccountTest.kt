package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskedAccountTest {

    /** 실제 기기에서 수신한 알림의 계좌 표기 */
    private val noti = "941602-**-***318"

    @Test
    fun `직접 입력한 계좌번호와 매칭`() {
        assertTrue(MaskedAccount.matches(noti, "94160277777318"))
    }

    @Test
    fun `하이픈을 넣어 입력해도 매칭`() {
        assertTrue(MaskedAccount.matches(noti, "941602-77-777318"))
    }

    @Test
    fun `승격으로 저장된 마스킹 값끼리도 매칭`() {
        assertTrue(MaskedAccount.matches(noti, "941602-**-***318"))
    }

    @Test
    fun `뒷자리가 다르면 매칭 실패`() {
        assertFalse(MaskedAccount.matches(noti, "94160277777319"))
    }

    @Test
    fun `앞자리가 다르면 매칭 실패`() {
        assertFalse(MaskedAccount.matches(noti, "94150277777318"))
    }

    @Test
    fun `자릿수가 다르면 매칭 실패`() {
        assertFalse(MaskedAccount.matches(noti, "9416027777318"))
    }

    @Test
    fun `가려진 자리만 다르면 구분할 수 없어 매칭된다`() {
        assertTrue(MaskedAccount.matches(noti, "94160299999318"))
    }

    @Test
    fun `빈 값이면 매칭 실패`() {
        assertFalse(MaskedAccount.matches(null, "94160277777318"))
        assertFalse(MaskedAccount.matches(noti, ""))
    }

    @Test
    fun `normalize는 구분자를 제거하고 숫자만 남긴다`() {
        assertEquals("94160277777318", MaskedAccount.normalize("941602-77-777318"))
    }

    @Test
    fun `normalize는 마스킹 문자를 보존한다`() {
        assertEquals("941602*****318", MaskedAccount.normalize(noti))
    }

    @Test
    fun `정규화해 저장한 값도 알림과 매칭된다`() {
        val stored = MaskedAccount.normalize("941602-77-777318")
        assertTrue(MaskedAccount.matches(noti, stored))
    }
}
