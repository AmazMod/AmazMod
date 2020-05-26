package com.edotassi.amazmod.ui

import amazmod.com.transport.Constants
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.edotassi.amazmod.R
import com.edotassi.amazmod.adapters.DonateProductsAdapter
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.android.synthetic.main.activity_donation.*
import org.tinylog.kotlin.Logger
import java.util.*

class DonationActivity : BaseAppCompatActivity(), PurchasesUpdatedListener {
    private lateinit var billingClient: BillingClient
    private lateinit var acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener
    private lateinit var consumeResponseListener: ConsumeResponseListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setTitle(R.string.support_us)
        } catch (exception: NullPointerException) {
            Logger.error(exception, "AboutActivity onCreate exception: {}", exception.message)
        }
        setContentView(R.layout.activity_donation)
        setupBillingClient()
        donation_activity_products.layoutManager = LinearLayoutManager(this)
        donation_activity_donate_opencollective.setOnClickListener {
            val url = Constants.DONATE_URL
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        donation_activity_donate_view_donors.setOnClickListener {
            val i = Intent(this@DonationActivity, DonorsActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(i)
        }
    }

    private fun loadSkuDetails() {
        if (billingClient.isReady) {
            val params = SkuDetailsParams.newBuilder()
                    .setSkusList(Arrays.asList(*Constants.DONATE_SKU_LIST))
                    .setType(BillingClient.SkuType.INAPP)
                    .build()
            billingClient.querySkuDetailsAsync(params) { billingResult, list ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    //Sort SKU items by price
                    list.sortWith(Comparator { o1, o2 -> (o1.priceAmountMicros - o2.priceAmountMicros).toInt() })
                    loadProductToRecyclerView(list)
                } else {
                    Logger.error("Cannot query available products!")
                }
            }
        } else {
            Logger.error("Billing client not ready!!")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadProductToRecyclerView(list: List<SkuDetails>) {
        val adapter = DonateProductsAdapter(this, list, billingClient)
        donation_activity_products.adapter = adapter
        activity_donation_progress.visibility = View.GONE
        donation_activity_products.visibility = View.VISIBLE
    }

    private fun setupBillingClient() {
        consumeResponseListener = ConsumeResponseListener { billingResult, s -> //TODO
            Logger.debug("onConsumeResponse " + billingResult.responseCode + ": " + billingResult.debugMessage + " // " + s)
        }
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Logger.debug("Success to connect Billing")

                    //Consume all purchases not consumed yet
                    val pr = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
                    val pList = pr.purchasesList
                    for (iitem in pList) {
                        consume(iitem)
                    }

                    //List available products
                    loadSkuDetails()
                } else {
                    Logger.error("Failed to connect to billing. Error " + billingResult.responseCode + " : " + billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                Logger.debug("You are disconnected from Billing")
            }
        })
        acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult -> Logger.debug("onAcknowledgePurchaseResponse: " + billingResult.responseCode + ": " + billingResult.debugMessage) }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, list: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
                && list != null) {
            for (purchase in list) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Toast.makeText(this, getString(R.string.no_donation), Toast.LENGTH_SHORT).show()
        } else {
            // Handle any other error codes.
            Toast.makeText(this, "Error " + billingResult.responseCode + ": " + billingResult.debugMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            //...
            Toast.makeText(this, getString(R.string.thanks_donation), Toast.LENGTH_SHORT).show()
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged) {
                consume(purchase)
            }
            //If user donates, doesn't ask for donation for the next 90 days
            val day = 1000 * 60 * 60 * 24.toLong()
            val now = Calendar.getInstance().timeInMillis
            Prefs.putLong(Constants.PREF_LAST_DONATION_ALERT, now + 90 * day)
        } else {
            Toast.makeText(this, "Purchase Code: " + purchase.purchaseState + " // " + purchase.developerPayload, Toast.LENGTH_SHORT).show()
        }
    }

    fun consume(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        billingClient.consumeAsync(consumeParams, consumeResponseListener)
    }
}