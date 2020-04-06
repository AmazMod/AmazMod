package com.edotassi.amazmod.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.DonateProductsAdapter;
import com.huami.watch.util.Log;

import org.tinylog.Logger;

import java.util.Arrays;
import java.util.List;

public class DonationActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    BillingClient billingClient;
    Button loadProductButton;
    RecyclerView recyclerViewProducts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        
        setupBillingClient();

        loadProductButton = (Button)findViewById(R.id.donation_actibity_btnload);
        recyclerViewProducts = (RecyclerView)findViewById(R.id.donation_activity_products);

        recyclerViewProducts.setHasFixedSize(true);
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));

        loadProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (billingClient.isReady()) {
                    SkuDetailsParams params = SkuDetailsParams.newBuilder()
                            .setSkusList(Arrays.asList("coffee","beer","donors","hall_of_fame"))
                            .setType(BillingClient.SkuType.INAPP)
                            .build();
                    billingClient.querySkuDetailsAsync(params, new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                                loadProductToRecyclerView(list);
                            }else{
                                Toast.makeText(DonationActivity.this, "Cannot query available products!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Toast.makeText(DonationActivity.this, "Billing client not ready!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void loadProductToRecyclerView(List<SkuDetails> list) {
        DonateProductsAdapter adapter = new DonateProductsAdapter(this,list,billingClient);
        recyclerViewProducts.setAdapter(adapter);
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    Toast.makeText(DonationActivity.this, "Success to connect Billing", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(DonationActivity.this, "Failed to connect to Billing. Error " + billingResult.getResponseCode(), Toast.LENGTH_SHORT).show();
                    Logger.debug("Failed to connect to billing. Error " + billingResult.getResponseCode() + " : " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Toast.makeText(DonationActivity.this, "You are disconnected from Billing", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        //Here, if user Taps BUY, we get the data
        int items = 0;
        if (list != null){
            items = list.size();
        }
        Toast.makeText(this, "Purchased " + items + " item(s)", Toast.LENGTH_SHORT).show();
    }
}
