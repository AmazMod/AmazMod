package com.amazmod.service.support;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.util.Log;

import com.amazmod.service.Constants;
import com.amazmod.service.util.DeviceUtil;

public class BatteryJobService extends JobService {

    JobParameters params;
    Context mContext;

    @Override
    public boolean onStartJob(JobParameters params) {

        this.params = params;
        mContext = this;

        final int id = params.getJobId();

        Log.d(Constants.TAG, "BatteryJobService onStartJob id: " + id);

        if (id == 0)
            saveBattery();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(Constants.TAG, "BatteryJobService onStopJob id: " + params.getJobId());

        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "BatteryJobService onDestroy");

        super.onDestroy();
    }

    private void saveBattery() {
        if (DeviceUtil.saveBatteryData(mContext, false)) {
            Log.d(Constants.TAG, "BatteryJobService saveBattery finished");
            jobFinished(params, false);
        } else {
            Log.d(Constants.TAG, "BatteryJobService saveBattery rescheduled");
            jobFinished(params, true);
        }
    }

}
