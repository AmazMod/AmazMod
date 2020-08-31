package com.edotassi.amazmod.helpers;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityCamera extends AccessibilityService {


        @Override
        public void onCreate() {
            super.onCreate();
        }

        @Override
        protected void onServiceConnected() {
            super.onServiceConnected();
            Logger.debug("AccesibilityCamera service connected");

            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if(nodeInfo == null) return;

            final List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("Take photo");

            for (AccessibilityNodeInfo node : list) {
                Logger.info("AmazMod Accesibility Camera: click node " + node.toString());
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

            stopSelf(); //Stop service after taking picture
        }

        @Override
        public void onAccessibilityEvent(AccessibilityEvent event) { }

        @Override
        public void onInterrupt() {}

}
