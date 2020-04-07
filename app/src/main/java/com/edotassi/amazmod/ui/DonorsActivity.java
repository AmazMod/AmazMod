package com.edotassi.amazmod.ui;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.DonateProductsAdapter;
import com.edotassi.amazmod.adapters.DonorsAdapter;
import com.google.gson.Gson;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DonorsActivity extends BaseAppCompatActivity {

    private TextView textTitle;
    RecyclerView recyclerViewDonorList;
    private List<Donor> donors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.donors);
        } catch (NullPointerException exception) {
            Logger.error(exception, "AboutActivity onCreate exception: {}", exception.getMessage());
        }
        setContentView(R.layout.activity_donors);
        textTitle = findViewById(R.id.activity_donors_title);
        recyclerViewDonorList = (RecyclerView) findViewById(R.id.activity_donors_list);
        recyclerViewDonorList.setHasFixedSize(true);
        //recyclerViewDonorList.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDonorList.setLayoutManager(new GridLayoutManager(this, 2));
        listDonors();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void listDonors() {
        //String donorsURL = "https://opencollective.com/amazmod-33/members/all.json";
        String donorsURL = "https://opencollective.com/amazmod-33/members/users.json";
        //String donorsURL = "https://opencollective.com/amazmod-33/members/all.json?TierId=7058";

        Request request = new Request.Builder()
                .url(donorsURL)
                .build();
        Logger.info("listDonors: checking donors list in OpenCollective");

        OkHttpClient client = new OkHttpClient();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Logger.debug("listDonors: failed to check donors");
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String json = response.body().string();
                        Donor[] d = new Gson().fromJson(json, Donor[].class);
                        donors = Arrays.asList(d);

                        DonorsAdapter adapter = new DonorsAdapter(DonorsActivity.this, donors);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recyclerViewDonorList.setAdapter(adapter);
                            }
                        });


                    }
                });
    }

    public class Donor {
        public int MemberId;
        public String createdAt;
        public String type;
        public String role;
        public boolean isActive;
        public float totalAmountDonated;
        public String lastTransactionAt;
        public float lastTransactionAmount;
        public String profile;
        public String name;
        public String company;
        public String description;
        public String image;
        public String email;
        public String twitter;
        public String github;
        public String website;
        Donor() {
            // no-args constructor
        }
    }
}
