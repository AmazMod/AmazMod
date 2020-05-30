package com.edotassi.amazmod.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.edotassi.amazmod.Interface.IDonateProductClickListener
import com.edotassi.amazmod.R
import com.edotassi.amazmod.ui.DonationActivity

class DonateProductsAdapter(var donationActivity: DonationActivity, var skuDetailsList: List<SkuDetails>, var billingClient: BillingClient) : RecyclerView.Adapter<DonateProductsAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(donationActivity).inflate(R.layout.row_donate_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textProductName.text = skuDetailsList[position].title
        holder.textProductDescription.text = skuDetailsList[position].description
        holder.textProductPrice.text = skuDetailsList[position].price
        holder.setiDonateProductClickListener(IDonateProductClickListener { _, p ->
            val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetailsList[p])
                    .build()
            billingClient.launchBillingFlow(donationActivity, billingFlowParams)
        })
    }

    override fun getItemCount(): Int {
        return skuDetailsList.size
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var textProductName: TextView = view.findViewById(R.id.row_donate_product_name)
        var textProductDescription: TextView = view.findViewById(R.id.row_donate_product_description)
        var textProductPrice: TextView = view.findViewById(R.id.row_donate_product_price)
        var iDonateProductClickListener: IDonateProductClickListener? = null
        fun setiDonateProductClickListener(iDonateProductClickListener: IDonateProductClickListener?) {
            this.iDonateProductClickListener = iDonateProductClickListener
        }

        override fun onClick(v: View) {
            iDonateProductClickListener!!.onProductClickListener(v, adapterPosition)
        }

        init {
            view.setOnClickListener(this)
        }
    }
}