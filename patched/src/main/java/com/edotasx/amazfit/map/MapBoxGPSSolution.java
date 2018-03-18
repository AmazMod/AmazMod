package com.edotasx.amazfit.map;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.app.FragmentManager;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.huami.watch.companion.CompanionApplication;
import com.huami.watch.companion.sport.map.GPSSportTrackLoader;
import com.huami.watch.companion.sport.map.GoogleGPSSolution;
import com.huami.watch.companion.sport.map.IGPSSolution;
import com.huami.watch.companion.sport.map.IMapAnimationCallback;
import com.huami.watch.companion.sport.model.GPSPoint;
import com.huami.watch.companion.sport.model.HeatmapData;
import com.huami.watch.companion.sport.model.RouteLineInfo;

import java.util.List;

/**
 * Created by edoardotassinari on 23/02/18.
 */

public class MapBoxGPSSolution implements IGPSSolution, OnMapReadyCallback {

    private GPSSportTrackLoader gpsSportTrackLoader;
    private GoogleMap googleMap;
    private Handler d = new Handler(Looper.getMainLooper());

    public MapBoxGPSSolution(final Fragment fragment, final FragmentManager fragmentManager, final GPSSportTrackLoader gpsSportTrackLoader) {
        this.gpsSportTrackLoader = gpsSportTrackLoader;

        this.d.post(new Runnable(){
            @Override
            public void run() {
                MapBoxGPSSolution.this.init(fragment, fragmentManager);
            }
        });
    }

    private void init(Fragment fragment, FragmentManager fragmentManager) {
        MapFragment mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.mapGoogle);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void addMarker(List<GPSPoint> list, int i) {
        /*
        if (googleMap == null) return;


        for(GPSPoint gpsPoint : list) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(gpsPoint.mLatitude, gpsPoint.mLongitude));
            markerOptions.title(String.valueOf(gpsPoint.getIndex()));

            googleMap.addMarker(markerOptions);
        }
        */
    }

    @Override
    public void addMaskOverlay(boolean b) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void destroyMap() {
    }

    @Override
    public void drawGPSLine(List<GPSPoint> list, RouteLineInfo routeLineInfo) {
        if ((googleMap == null) || (list.size() == 0)) return;

        LatLngBounds.Builder builder = LatLngBounds.builder();
        PolylineOptions polylineOptions = new PolylineOptions();

        for(GPSPoint gpsPoint : list) {
            LatLng latLng = new LatLng(gpsPoint.mLatitude, gpsPoint.mLongitude);
            polylineOptions.add(latLng);

            builder.include(latLng);
        }

        polylineOptions
                .color(Color.parseColor("#AA1E88E5"))
                .width(10);

        this.googleMap.addPolyline(polylineOptions);

        GPSPoint firstGpsPoint = list.get(0);
        addMarker(new LatLng(firstGpsPoint.mLatitude, firstGpsPoint.mLongitude), "Start");

        GPSPoint lastGpsPoint = list.get(list.size() - 1);
        addMarker(new LatLng(lastGpsPoint.mLatitude, lastGpsPoint.mLongitude), "Finish");

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 0));
    }

    @Override
    public void drawHeatmap(List<HeatmapData> var1) {

    }

    @Override
    public void location() {
        if (googleMap == null) return;

        Location location = googleMap.getMyLocation();

        if (location == null) {
            return;
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
    }

    @Override
    public void mapScreenShot(IOnMapScreenShotListener iOnMapScreenShotListener) {

    }

    @Override
    public void pauseMap() {

    }

    @Override
    public void resumeMap() {

    }

    @Override
    public void setDarkBackground(List<GPSPoint> list) {

    }

    @Override
    public void setHeatmapVisible(int i) {

    }

    @Override
    public void setMarkerVisible(int i) {

    }

    @Override
    public void setSpeedRange(float[] floats) {

    }

    @Override
    public void setSportType(int i) {

    }

    @Override
    public void zoomToShowEntireTrack(List<GPSPoint> list) {
        LatLngBounds.Builder builder = LatLngBounds.builder();

        for(GPSPoint gpsPoint : list) {
            LatLng latLng = new LatLng(gpsPoint.mLatitude, gpsPoint.mLongitude);
            builder.include(latLng);
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 160));
    }

    @Override
    public void zoomToShowEntireTrack(List<GPSPoint> list, IMapAnimationCallback iMapAnimationCallback) {
        LatLngBounds.Builder builder = LatLngBounds.builder();

        for(GPSPoint gpsPoint : list) {
            LatLng latLng = new LatLng(gpsPoint.mLatitude, gpsPoint.mLongitude);
            builder.include(latLng);
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 160));
        iMapAnimationCallback.onFinish();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        gpsSportTrackLoader.onMapLoaded();

        String prefMapType = PreferenceManager.getString(CompanionApplication.getContext(), Constants.PREFERENCE_MAPS_TYPE, "roadmap");
        switch (prefMapType) {
            case "roadmap": {
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            }
            case "satellite": {
                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            }
            case "hybrid": {
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            }
            case "terrain": {
                googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            }
        }

        googleMap.setMyLocationEnabled(true);
    }

    private void addMarker(LatLng latLng, String title) {
        if (googleMap == null) return;

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(title);

        googleMap.addMarker(markerOptions);
    }
}
