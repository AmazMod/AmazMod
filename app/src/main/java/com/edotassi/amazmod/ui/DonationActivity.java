package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.DonateProductsAdapter;
import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import amazmod.com.transport.Constants;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class DonationActivity extends BaseAppCompatActivity implements PurchasesUpdatedListener {

    BillingClient billingClient;
    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener;
    ConsumeResponseListener consumeResponseListener;
    Button donateButton, donorsButton;
    MaterialProgressBar materialProgressBar;
    RecyclerView recyclerViewProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.support_us);
        } catch (NullPointerException exception) {
            Logger.error(exception, "AboutActivity onCreate exception: {}", exception.getMessage());
        }

        setContentView(R.layout.activity_donation);

        setupBillingClient();

        donateButton = findViewById(R.id.donation_activity_donate_opencollective);
        donorsButton = findViewById(R.id.donation_activity_donate_view_donors);
        materialProgressBar = findViewById(R.id.activity_donation_progress);
        recyclerViewProducts = findViewById(R.id.donation_activity_products);

        recyclerViewProducts.setHasFixedSize(true);
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));

        donateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = Constants.DONATE_URL;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        donorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DonationActivity.this, DonorsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

    }

    private void loadSkuDetails() {
        if (billingClient.isReady()) {
            SkuDetailsParams params = SkuDetailsParams.newBuilder()
                    .setSkusList(Arrays.asList(Constants.DONATE_SKU_LIST))
                    .setType(BillingClient.SkuType.INAPP)
                    .build();
            billingClient.querySkuDetailsAsync(params, new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        //Sort SKU items by price
                        Collections.sort(list, new Comparator<SkuDetails>() {
                            public int compare(SkuDetails o1, SkuDetails o2) {
                                return (int) (o1.getPriceAmountMicros() -  o2.getPriceAmountMicros());
                            }
                        });
                        loadProductToRecyclerView(list);
                    } else {
                        Logger.error("Cannot query available products!");
                    }
                }
            });
        } else {
            Logger.error("Billing client not ready!!");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadProductToRecyclerView(List<SkuDetails> list) {
        DonateProductsAdapter adapter = new DonateProductsAdapter(this, list, billingClient);
        recyclerViewProducts.setAdapter(adapter);
        materialProgressBar.setVisibility(View.GONE);
        recyclerViewProducts.setVisibility(View.VISIBLE);
    }

    private void setupBillingClient() {
        consumeResponseListener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String s) {
                //TODO
                Logger.debug("onConsumeResponse " + billingResult.getResponseCode() + ": " + billingResult.getDebugMessage() + " // " + s);
            }
        };

        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Logger.debug("Success to connect Billing");

                    //Consume all purchases not consumed yet
                    Purchase.PurchasesResult pr = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                    List<Purchase> pList = pr.getPurchasesList();
                    for (Purchase iitem : pList) {
                        consume(iitem);
                    }

                    //List available products
                    loadSkuDetails();
                } else {
                    Logger.error("Failed to connect to billing. Error " + billingResult.getResponseCode() + " : " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Logger.debug("You are disconnected from Billing");
            }
        });
        acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                Logger.debug("onAcknowledgePurchaseResponse: " + billingResult.getResponseCode() + ": " + billingResult.getDebugMessage());
            }
        };
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && list != null) {
            for (Purchase purchase : list) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Toast.makeText(this, getString(R.string.no_donation), Toast.LENGTH_SHORT).show();
        } else {
            // Handle any other error codes.
            Toast.makeText(this, "Error " + billingResult.getResponseCode() + ": " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            //...
            Toast.makeText(this, getString(R.string.thanks_donation), Toast.LENGTH_SHORT).show();
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                consume(purchase);
            }
            //If user donates, doesn't ask for donation for the next 90 days
            long day = 1000 * 60 * 60 * 24;
            long now = Calendar.getInstance().getTimeInMillis();
            Prefs.putLong(Constants.PREF_LAST_DONATION_ALERT, now + 90 * day);

        }else{
            Toast.makeText(this, "Purchase Code: " + purchase.getPurchaseState() + " // " + purchase.getDeveloperPayload(), Toast.LENGTH_SHORT).show();
        }
    }


    void consume(Purchase purchase ){
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.consumeAsync(consumeParams, consumeResponseListener);
    }
}
