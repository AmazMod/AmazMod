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

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
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
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Logger.debug("Success to connect Billing");
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
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        //Here, if user Taps BUY, we get the data
        int items = 0;
        String message = getString(R.string.no_donation);
        if (list != null) {
            items = list.size();
            message = getString(R.string.thanks_donation);
        }else{

        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
