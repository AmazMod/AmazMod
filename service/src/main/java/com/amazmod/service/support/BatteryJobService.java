package com.amazmod.service.support;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

import com.amazmod.service.Constants;
import com.amazmod.service.util.DeviceUtil;

import org.tinylog.Logger;

public class BatteryJobService extends JobService {

    JobParameters params;
    Context mContext;

    @Override
    public boolean onStartJob(JobParameters params) {

        this.params = params;
        mContext = this;

        final int id = params.getJobId();

        Logger.debug("BatteryJobService onStartJob id: " + id);

        if (id == 0)
            saveBattery();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Logger.debug("BatteryJobService onStopJob id: " + params.getJobId());

        return true;
    }

    @Override
    public void onDestroy() {
        Logger.debug("BatteryJobService onDestroy");

        super.onDestroy();
    }

    private void saveBattery() {
        if (DeviceUtil.saveBatteryData(mContext, false)) {
            Logger.debug("BatteryJobService saveBattery finished");
            jobFinished(params, false);
        } else {
            Logger.debug("BatteryJobService saveBattery rescheduled");
            jobFinished(params, true);
        }
    }

}
