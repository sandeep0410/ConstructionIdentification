package com.umn.mto.android.workzonealert.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.umn.mto.android.workzonealert.R;
import com.umn.mto.android.workzonealert.ScanningActivity;

/**
 * Created by Sandeep on 9/16/2015.
 */
public class ImageNotificationDialogFragment extends DialogFragment {

    Context mContext;
    int drawableID;

    public ImageNotificationDialogFragment() {
        mContext = ScanningActivity.getInstance();
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        drawableID = args.getInt("drawable");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Warning");
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.image_warning, null);
        ImageView v = (ImageView)view.findViewById(R.id.image_dialog);
        Picasso.with(mContext).load(drawableID).into(v);
       // v.setBackground(mContext.getDrawable(drawableID));
        builder.setView(view);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;

    }
}
