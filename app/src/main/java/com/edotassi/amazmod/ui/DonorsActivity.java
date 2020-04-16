package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.DonorsAdapter;
import com.google.gson.Gson;

import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DonorsActivity extends BaseAppCompatActivity {

    private TextView textTitle;
    RecyclerView recyclerViewDonorList;
    MaterialProgressBar materialProgressBar;
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
        materialProgressBar = findViewById(R.id.activity_donors_progress);
        recyclerViewDonorList = (RecyclerView) findViewById(R.id.activity_donors_list);
        recyclerViewDonorList.setHasFixedSize(true);
        recyclerViewDonorList.setLayoutManager(new LinearLayoutManager(this));
        //recyclerViewDonorList.setLayoutManager(new GridLayoutManager(this, 2));
        listDonors();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void listDonors() {
        String donorsURL = "https://opencollective.com/amazmod-33/members/users.json";

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

                        donors = onlyBackers(donors);
                        donors = removeDuplicates(donors);

                        //Sort Donors by total amount
                        Collections.sort(donors, new Comparator<Donor>() {
                            public int compare(Donor o1, Donor o2) {
                                return (int) (o2.totalAmountDonated - o1.totalAmountDonated);
                            }
                        });

                        DonorsAdapter adapter = new DonorsAdapter(DonorsActivity.this, donors);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recyclerViewDonorList.setAdapter(adapter);
                                materialProgressBar.setVisibility(View.GONE);
                                recyclerViewDonorList.setVisibility(View.VISIBLE);
                            }
                        });


                    }
                });
    }

    private List<Donor> onlyBackers(List<Donor> originalList) {
        //This code is faster and optimized for Java 8 but needs, at least, Android N (api 25)
        // Predicate<Donor> byRole = donor -> donor.role.equals("BACKER");
        //Predicate<Donor> byAmount = donor -> donor.totalAmountDonated > 0;
        //List<Donor> filtered = originalList.stream().filter(byRole).filter(byAmount)
        //        .collect(Collectors.toList());

        //Code compatible with Java 7
        List<Donor> filtered = new ArrayList<>();

        for (Donor donor : originalList) {
            if (donor.role.equals("BACKER") & donor.totalAmountDonated > 0) {
                filtered.add(donor);
            }
        }
        return filtered;
    }

    private List<Donor> removeDuplicates(List<Donor> originalList) {
        List<Donor> listDonor = new ArrayList<>();
        for (Donor donor : originalList) {
            if (!listDonor.contains(donor)) {
                listDonor.add(donor);
            }
        }
        return listDonor;
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
        public String currency;

        Donor() {
            // no-args constructor
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Donor)) {
                return false;
            }
            Donor other = (Donor) obj;
            return this.name.equals(other.name);
        }

    }
}
