package com.huami.watch.companion.sport.map;

import android.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 23/02/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class GPSSportTrackLoader {

    @DexIgnore
    private Fragment f;
    @DexIgnore
    private IGPSSolution e;
    @DexIgnore
    private ExecutorService d;
    @DexIgnore
    private SparseIntArray k;


    @DexIgnore
    public void registerListener(ITrackDataListener iTrackDataListener) {
    }

    @DexIgnore
    public void onMapLoaded() {
    }

    @DexEdit(defaultAction = DexAction.IGNORE)
    final class b implements ITrackDataListener {

        @DexEdit
        public b() {}

        @DexIgnore
        @Override
        public void onTrackDataLoaded(long l, int i) {

        }

        @DexIgnore
        @Override
        public void onTrackDataReceived(long l, LoadData loadData) {

        }
    }
}
*/