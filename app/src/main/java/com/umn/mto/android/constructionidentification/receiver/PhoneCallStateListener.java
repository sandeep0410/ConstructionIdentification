package com.umn.mto.android.constructionidentification.receiver;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.umn.mto.android.constructionidentification.MyApplication;
import com.umn.mto.android.constructionidentification.SpeedDetectionService;
import com.umn.mto.android.constructionidentification.settings.Settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PhoneCallStateListener extends PhoneStateListener {

    private Context mContext;
    public static final double MAX_ALLOWED_SPEED = 5;

    public PhoneCallStateListener(Context context) {
        super();
        this.mContext = context;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                if (!Settings.enable_calls && SpeedDetectionService.mSpeed > MAX_ALLOWED_SPEED)
                    endCall(incomingNumber);
                if (MyApplication.isActivityVisible())
                    Toast.makeText(mContext, "Incoming call from: " + incomingNumber, Toast.LENGTH_SHORT).show();
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (!Settings.enable_calls && SpeedDetectionService.mSpeed > MAX_ALLOWED_SPEED) {
                    endCall(incomingNumber);
                    Toast.makeText(mContext, "Please reduce speed to make calls ", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                break;
        }
    }

    private void endCall(String callingNumber) {
        try {
            /*if(!ScanningActivity.mScanning)
                return;*/

            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Class c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(tm);
            //telephonyService.silenceRinger();
            telephonyService.endCall();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
