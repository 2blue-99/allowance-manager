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
import timber.log.Timber

/**
 * 개발자 후원(커피값) — Play 인앱 결제(소비성 상품).
 *
 * ⚠️ Play Console에서 상품 ID [DONATION_PRODUCT_ID] 를 **소비성(consumable) 관리 상품**으로 등록하고
 *    가격을 2,000원으로 설정해야 실제 동작한다. (디버그 빌드/미등록 상품에선 결제창이 뜨지 않음)
 * 소비성으로 처리해 여러 번 후원할 수 있게 한다.
 */
class DonationManager(context: Context) {

    enum class Result { SUCCESS, CANCELED, ERROR }

    private var resultCallback: ((Result) -> Unit)? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when {
            result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                purchases.forEach { consume(it) }
                resultCallback?.invoke(Result.SUCCESS)
            }
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                resultCallback?.invoke(Result.CANCELED)
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
                client.launchBillingFlow(activity, params)
            }
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
        client.queryProductDetailsAsync(params) { result, productDetailsList ->
            onResult(
                if (result.responseCode == BillingClient.BillingResponseCode.OK) productDetailsList.firstOrNull()
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
        const val DONATION_PRODUCT_ID = "donate_coffee"
    }
}
