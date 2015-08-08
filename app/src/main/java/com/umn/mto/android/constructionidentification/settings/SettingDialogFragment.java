package com.umn.mto.android.constructionidentification.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import com.umn.mto.android.constructionidentification.R;

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
        final Switch vibration = (Switch)view.findViewById(R.id.vibration);
        final Switch warning = (Switch)view.findViewById(R.id.warning);
        scanTime.setText(String.valueOf(Settings.scan_Time));
        vibration.setChecked(Settings.vibration);
        warning.setChecked(Settings.alarm);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.scan_Time = Integer.parseInt(scanTime.getText().toString());
                Settings.vibration = vibration.isChecked();
                Settings.alarm = warning.isChecked();
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

