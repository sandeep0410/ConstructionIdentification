package com.umn.mto.android.workzonealert;

import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Sandeep on 12/24/2015.
 */
public class LogUtils {
    static boolean DEBUG = true;
    static String TAG = "sandeep";

    public static void log(String message) {
        if (DEBUG)
            Log.d(TAG, message);
    }

    public static void writeToFile(String status) {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + ScanningActivity.DIR_NAME + File.separator);
        File temp = new File(f.getAbsolutePath() + File.separator + "scan_time_data.csv");
        LogUtils.log(f.getAbsolutePath() + " " + temp.getAbsolutePath());
        LogUtils.log("" + (f.exists()) + " " + temp.exists());
        CSVWriter writer = null;
        if (!temp.exists()) {
            if (!f.exists())
                f.mkdir();
            temp = new File(f, "scan_time_data.csv");
            try {
                writer = new CSVWriter(new FileWriter(temp, true));
                String[] entries = "Time#Status#Speed#Latitude#Longitude".split("#"); // array of your values
                writer.writeNext(entries);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            writer = new CSVWriter(new FileWriter(temp, true));
            Calendar c = Calendar.getInstance();
            String[] entries = {
                    (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DAY_OF_MONTH)
                            + "/" + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "." + c.get(Calendar.MILLISECOND) + " " + (c.get(Calendar.AM_PM) == 0 ? "AM" : "PM"),
                    status,
                    "" + SpeedDetectionService.mSpeed,
                    "" + SpeedDetectionService.updateLat,
                    "" + SpeedDetectionService.updateLon
            };
            writer.writeNext(entries);
            writer.close();
        } catch (IOException e) {
            //error
        }


        LogUtils.log("" + f.exists());
    }
}
