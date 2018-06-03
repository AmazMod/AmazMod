package com.edotassi.amazmodcompanionservice.support;

import android.app.Activity;

public class ActivityFinishRunnable implements Runnable {

    private Activity activity;

    public ActivityFinishRunnable(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {
        activity.finish();
    }
}
