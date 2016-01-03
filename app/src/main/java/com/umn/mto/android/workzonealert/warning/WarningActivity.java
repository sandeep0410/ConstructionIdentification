package com.umn.mto.android.workzonealert.warning;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

import com.umn.mto.android.workzonealert.LogUtils;

/**
 * Created by Sandeep on 9/29/2015.
 */
public class WarningActivity extends Activity {

    static WarningActivity _instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.log("inside warning activity: ");

        final AlertDialog d = new AlertDialog.Builder(this)
                .setMessage("Speed is over limit. Reduce to activate phone")
                .setTitle("Warning")
                .setCancelable(false)
                .create();
        d.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _instance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        _instance = null;
    }

    public static WarningActivity getInstance() {
        return _instance;
    }
}
