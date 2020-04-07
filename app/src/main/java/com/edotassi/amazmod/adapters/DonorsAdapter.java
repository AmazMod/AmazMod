package com.edotassi.amazmod.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.SkuDetails;
import com.edotassi.amazmod.Interface.IDonateProductClickListener;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.ui.DonationActivity;
import com.edotassi.amazmod.ui.DonorsActivity;

import org.tinylog.Logger;

import java.io.InputStream;
import java.util.List;

public class DonorsAdapter extends RecyclerView.Adapter<DonorsAdapter.MyViewHolder> {

    DonorsActivity donorsActivity;
    List<DonorsActivity.Donor> donorsList;

    public DonorsAdapter(DonorsActivity donorsActivity, List<DonorsActivity.Donor> donorList) {
        this.donorsActivity = donorsActivity;
        this.donorsList = donorList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(donorsActivity.getBaseContext())
                .inflate(R.layout.row_donor, parent,false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textDonorName.setText(donorsList.get(position).name);
        new DownloadImageTask(holder.textDonorPicture)
                .execute(donorsList.get(position).image);
    }

    @Override
    public int getItemCount() {
        return donorsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textDonorName;
        ImageView textDonorPicture;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textDonorName = (TextView)itemView.findViewById(R.id.row_donor_name);
            textDonorPicture = (ImageView)itemView.findViewById(R.id.row_donor_avatar);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Logger.error("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
