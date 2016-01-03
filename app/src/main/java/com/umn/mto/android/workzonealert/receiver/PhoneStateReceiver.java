package com.umn.mto.android.workzonealert.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.umn.mto.android.workzonealert.LogUtils;
import com.umn.mto.android.workzonealert.SpeedDetectionService;

public class PhoneStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent serviceLauncher = new Intent(context, SpeedDetectionService.class);
            serviceLauncher.setAction(SpeedDetectionService.NotificationConstants.START_SPEED_DETECTION_SERVICE);
            context.startService(serviceLauncher);

        } else {
            LogUtils.log("Entered receiver");
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            PhoneCallStateListener listener = new PhoneCallStateListener(context);
            telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

        }

    }
}