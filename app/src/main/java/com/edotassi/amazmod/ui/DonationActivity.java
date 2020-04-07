package com.edotassi.amazmod.ui;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
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

import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import amazmod.com.transport.Constants;

public class DonationActivity extends BaseAppCompatActivity implements PurchasesUpdatedListener {

    BillingClient billingClient;
    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener;
    ConsumeResponseListener consumeResponseListener;
    Button donateButton;
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

        donateButton = (Button) findViewById(R.id.donation_actibity_donate_opencollective);
        recyclerViewProducts = (RecyclerView) findViewById(R.id.donation_activity_products);

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
    }

    private void setupBillingClient() {
        consumeResponseListener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String s) {
                //TODO
                Toast.makeText(DonationActivity.this, "onConsumeResponse " + billingResult.getResponseCode() + ": " + billingResult.getDebugMessage() + " // " + s, Toast.LENGTH_SHORT).show();
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
                //TODO
                Toast.makeText(DonationActivity.this, "onAcknowledgePurchaseResponse: " + billingResult.getResponseCode() + ": " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "onPurchasesUpdated " + billingResult.getResponseCode() + ": " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            //...
            Toast.makeText(this, getString(R.string.thanks_donation), Toast.LENGTH_SHORT).show();
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                /*
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                 */
                consume(purchase);
            }
        }else{
            Toast.makeText(this, "handlePurchase Code: " + purchase.getPurchaseState() + " // " + purchase.getDeveloperPayload(), Toast.LENGTH_SHORT).show();
        }
    }


    void consume(Purchase purchase ){
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.consumeAsync(consumeParams, consumeResponseListener);
    }
}
