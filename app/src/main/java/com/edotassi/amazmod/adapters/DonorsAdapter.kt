package com.edotassi.amazmod.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edotassi.amazmod.R
import com.edotassi.amazmod.ui.DonorsActivity
import com.edotassi.amazmod.ui.DonorsActivity.Donor
import com.edotassi.amazmod.util.picasso.CircleTransform
import com.squareup.picasso.Picasso

class DonorsAdapter(private val donorsActivity: DonorsActivity, private val donorsList: List<Donor>) : RecyclerView.Adapter<DonorsAdapter.DonorsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonorsViewHolder {
        val view = LayoutInflater.from(donorsActivity).inflate(R.layout.row_donor, parent, false)
        return DonorsViewHolder(view)
    }

    override fun onBindViewHolder(holder: DonorsViewHolder, position: Int) {
        val donor = donorsList[position]
        holder.name.text = donor.name
        holder.role.text = donor.role
        holder.amountDonated.text = "${donor.currency} ${donor.totalAmountDonated}"
        Picasso.get()
                .load(donor.image)
                .placeholder(R.drawable.account_box)
                .resize(128, 128)
                .centerCrop()
                .transform(CircleTransform(10, 0))
                .into(holder.picture)
    }

    override fun getItemCount(): Int {
        return donorsList.size
    }

    inner class DonorsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var name: TextView = view.findViewById(R.id.row_donor_name)
        var role: TextView = view.findViewById(R.id.row_donor_role)
        var amountDonated: TextView = view.findViewById(R.id.row_donor_amount)
        var picture: ImageView = view.findViewById(R.id.row_donor_avatar)
    }
}