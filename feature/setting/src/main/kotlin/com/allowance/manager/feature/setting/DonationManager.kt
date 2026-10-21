package com.allowance.manager.feature.setting

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import timber.log.Timber

/**
 * 개발자 후원(커피값) — Play 인앱 결제(소비성 상품).
 *
 * ⚠️ Play Console에서 상품 ID [DONATION_PRODUCT_ID] 를 **소비성(consumable) 관리 상품**으로 등록하고
 *    가격을 2,000원으로 설정해야 실제 동작한다. (디버그 빌드/미등록 상품에선 결제창이 뜨지 않음)
 * 소비성으로 처리해 여러 번 후원할 수 있게 한다.
 */
class DonationManager(context: Context) {

    /** PENDING = 결제 수단 승인 대기(편의점 결제 등). 아직 돈이 안 들어왔으므로 감사 다이얼로그를 띄우지 않는다 */
    enum class Result { SUCCESS, PENDING, CANCELED, ERROR }

    private var resultCallback: ((Result) -> Unit)? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                val purchased = list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                purchased.forEach { consume(it) }
                resultCallback?.invoke(
                    when {
                        purchased.isNotEmpty() -> Result.SUCCESS
                        list.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> Result.PENDING
                        else -> Result.ERROR
                    },
                )
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> resultCallback?.invoke(Result.CANCELED)
            // 소비 안 된 구매가 남아 있는 경우(결제 직후 프로세스 종료 등) → 정리해 두면 다음 후원부터 정상
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                consumeOwned { resultCallback?.invoke(Result.ERROR) }
            }
            else -> resultCallback?.invoke(Result.ERROR)
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    /** 후원 결제 시작. onResult 로 성공/취소/실패를 알린다. */
    fun donate(activity: Activity, onResult: (Result) -> Unit) {
        resultCallback = onResult
        ensureConnected { ok ->
            if (!ok) { onResult(Result.ERROR); return@ensureConnected }
            // 결제창을 띄우기 전에 소비 안 된 구매를 먼저 정리 — 소비성 상품은 소비 전까지 재구매가 막힌다
            consumeOwned {
                queryProduct { product ->
                    if (product == null) { onResult(Result.ERROR); return@queryProduct }
                    val params = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(product)
                                    .build(),
                            ),
                        )
                        .build()
                    val launch = client.launchBillingFlow(activity, params)
                    // 결제창 자체가 안 뜨면 PurchasesUpdatedListener 가 호출되지 않으므로 여기서 결과를 알린다
                    if (launch.responseCode != BillingClient.BillingResponseCode.OK) onResult(Result.ERROR)
                }
            }
        }
    }

    /**
     * 기기에 남아 있는 구매 중 소비 안 된 PURCHASED 를 소비한다 (복구).
     * 결제 직후 앱이 죽었거나, PENDING 이 나중에 PURCHASED 로 바뀐 경우를 커버. 조회 실패해도 다음 단계로 진행.
     */
    private fun consumeOwned(onDone: () -> Unit) {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach { consume(it) }
            onDone()
        }
    }

    private fun ensureConnected(onReady: (Boolean) -> Unit) {
        if (client.isReady) { onReady(true); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                onReady(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() { /* 다음 요청 시 재연결 */ }
        })
    }

    private fun queryProduct(onResult: (ProductDetails?) -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(DONATION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        // Billing 8.0.0+ : 콜백 2번째 인자가 List<ProductDetails> → QueryProductDetailsResult 로 변경
        client.queryProductDetailsAsync(params) { result, queryResult ->
            onResult(
                if (result.responseCode == BillingClient.BillingResponseCode.OK) queryResult.productDetailsList.firstOrNull()
                else null,
            )
        }
    }

    // 소비 처리 → 다시 후원할 수 있게
    private fun consume(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.consumeAsync(params) { result, _ ->
            Timber.tag("Donation").d("consume: ${result.responseCode}")
        }
    }

    fun release() {
        runCatching { client.endConnection() }
    }

    companion object {
        // Play Console 일회성 제품 ID (개발자 커피값 후원). 콘솔의 '제품 ID'와 정확히 일치해야 결제창이 뜬다.
        const val DONATION_PRODUCT_ID = "com.allowance.coffee"
    }
}
