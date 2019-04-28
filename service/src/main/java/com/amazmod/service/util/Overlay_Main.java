package com.amazmod.service.util;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.amazmod.service.ui.overlay_launcher;

    public class Overlay_Main extends Activity {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent svc = new Intent(this, overlay_launcher.class);
            startService(svc);
            finish();
        }
    }
