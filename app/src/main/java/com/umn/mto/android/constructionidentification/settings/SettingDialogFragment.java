package com.umn.mto.android.constructionidentification.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.umn.mto.android.constructionidentification.R;
import com.umn.mto.android.constructionidentification.ScanningActivity;
import com.umn.mto.android.constructionidentification.SpeedDetectionService;

/**
 * Created by Sandeep on 8/8/2015.
 */
public class SettingDialogFragment extends DialogFragment {
    Context context;

    public SettingDialogFragment(Context context){
        this.context = context;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Settings");
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View view = inflater.inflate(R.layout.settings_layout, null);
        builder.setView(view);
        final EditText scanTime = (EditText)view.findViewById(R.id.scanning_time);
        final EditText rssi_value = (EditText)view.findViewById(R.id.rssi_value);
        final Switch vibration = (Switch)view.findViewById(R.id.vibration);
        final Switch warning = (Switch)view.findViewById(R.id.warning);
        final Switch driving = (Switch)view.findViewById(R.id.driving);
        final Switch data = (Switch)view.findViewById(R.id.data_collection);
        scanTime.setText(String.valueOf(Settings.scan_Time));
        rssi_value.setText(String.valueOf(Settings.rssi_value));
        vibration.setChecked(Settings.vibration);
        warning.setChecked(Settings.alarm);
        driving.setChecked(Settings.enable_calls);
        data.setChecked(Settings.data_collection);
/*        driving.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && ((ScanningActivity) context).speedDetectionServiceRunning())
                    context.stopService(new Intent(context, SpeedDetectionService.class));
                else if (!((ScanningActivity) context).speedDetectionServiceRunning())
                    context.startService(new Intent(context, SpeedDetectionService.class));
                Settings.enable_calls = driving.isChecked();
            }
        });*/
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int user_rssi = Integer.parseInt(rssi_value.getText().toString());
                if (user_rssi < 20) {
                    Toast.makeText(context, "RSSI Threshold set to max value (-20)", Toast.LENGTH_SHORT).show();
                    Settings.rssi_value = 20;
                } else if (user_rssi > 128) {
                    Toast.makeText(context, "RSSI Threshold set to min value (-128)", Toast.LENGTH_SHORT).show();
                    Settings.rssi_value = 128;
                } else {
                    Settings.rssi_value = user_rssi;
                }
                Settings.scan_Time = Integer.parseInt(scanTime.getText().toString());
                Settings.vibration = vibration.isChecked();
                Settings.alarm = warning.isChecked();
                Settings.enable_calls = driving.isChecked();
                Settings.data_collection = data.isChecked();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        return builder.create();
    }
}

