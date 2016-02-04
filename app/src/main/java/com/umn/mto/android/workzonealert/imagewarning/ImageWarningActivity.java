package com.umn.mto.android.workzonealert.imagewarning;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.umn.mto.android.workzonealert.LogUtils;
import com.umn.mto.android.workzonealert.R;

/**
 * Created by Sandeep on 1/3/2016.
 */
public class ImageWarningActivity extends Activity {
    static ImageWarningActivity _instance = null;
    AlertDialog dialog;

    int drawableID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.log("inside image warning activity: ");
        drawableID = getIntent().getIntExtra("drawable", 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning");
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.image_warning, null);
        ImageView v = (ImageView) view.findViewById(R.id.image_dialog);
        Picasso.with(this).load(drawableID).into(v);
        // v.setBackground(mContext.getDrawable(drawableID));
        builder.setView(view);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
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
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        finish();
    }

    public static ImageWarningActivity getInstance() {
        return _instance;
    }
}
