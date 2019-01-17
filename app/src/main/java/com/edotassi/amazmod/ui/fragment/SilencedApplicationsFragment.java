package com.edotassi.amazmod.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.SilencedApplicationsAdapter;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.ui.card.Card;

import java.util.ArrayList;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;

public class SilencedApplicationsFragment extends Card implements SilencedApplicationsAdapter.Bridge {

    @BindView(R.id.fragment_silenced_apps_grid)
    GridView silencedApplicationsView;

    private SilencedApplicationsAdapter silencedApplicationsAdapter;
    private Context mContext;
    private View card;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        card = inflater.inflate(R.layout.fragment_silenced_apps, container, false);

        ButterKnife.bind(this, card);
        silencedApplicationsAdapter = new SilencedApplicationsAdapter(this, R.layout.item_silenced_app, new ArrayList<NotificationPreferencesEntity>());
        silencedApplicationsView.setAdapter(silencedApplicationsAdapter);
        updateSilencedApps();
        return card;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(Constants.TAG, "SilencedApplicationsFragment onResume");

        updateSilencedApps();
    }

    @Override
    public String getName() {
        return "silenced-apps";
    }

    private void updateSilencedApps(){
        if (SilenceApplicationHelper.getSilencedApplicationsCount() > 0) {
            card.setVisibility(View.VISIBLE);
        }else{
            card.setVisibility(View.GONE);
        }
        silencedApplicationsAdapter.clear();
        silencedApplicationsAdapter.addAll(SilenceApplicationHelper.listSilencedApplications());
        silencedApplicationsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSilencedApplicationStatusChange() {
        updateSilencedApps();
    }
}
