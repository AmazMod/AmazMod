package com.edotassi.amazmod.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.SkuDetails;
import com.edotassi.amazmod.Interface.IDonateProductClickListener;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.ui.DonationActivity;

import java.util.List;

public class DonateProductsAdapter extends RecyclerView.Adapter<DonateProductsAdapter.MyViewHolder> {

    DonationActivity donationActivity;
    List<SkuDetails> skuDetailsList;
    BillingClient billingClient;

    public DonateProductsAdapter(DonationActivity donationActivity, List<SkuDetails> skuDetailsList, BillingClient billingClient) {
        this.donationActivity = donationActivity;
        this.skuDetailsList = skuDetailsList;
        this.billingClient = billingClient;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(donationActivity.getBaseContext())
                .inflate(R.layout.row_donate_item, parent,false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textProductName.setText(skuDetailsList.get(position).getTitle());
        holder.textProductDescription.setText(skuDetailsList.get(position).getDescription());
        holder.textProductPrice.setText(skuDetailsList.get(position).getPrice());

        holder.setiDonateProductClickListener(new IDonateProductClickListener() {
            @Override
            public void onProductClickListener(View view, int position) {
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetailsList.get(position))
                        .build();
                billingClient.launchBillingFlow(donationActivity, billingFlowParams);
            }
        });

    }

    @Override
    public int getItemCount() {
        return skuDetailsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textProductName;
        TextView textProductDescription;
        TextView textProductPrice;

        IDonateProductClickListener iDonateProductClickListener;

        public void setiDonateProductClickListener(IDonateProductClickListener iDonateProductClickListener) {
            this.iDonateProductClickListener = iDonateProductClickListener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textProductName = (TextView)itemView.findViewById(R.id.row_donate_product_name);
            textProductDescription = (TextView)itemView.findViewById(R.id.row_donate_product_description);
            textProductPrice = (TextView)itemView.findViewById(R.id.row_donate_product_price);
            itemView.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            iDonateProductClickListener.onProductClickListener(v,getAdapterPosition());
        }
    }
}
