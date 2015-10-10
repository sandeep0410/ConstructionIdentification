package com.umn.mto.android.constructionidentification.com.umn.mto.android.constructionidentification.dto;

/**
 * Created by Sandeep on 10/3/2015.
 */
public class BLETag {
    int workzoneID;
    String bleMac;
    double lat;
    double lon;
    int speedLimit;
    String message;
    int flag;
    String fileName;

    public BLETag(int workzoneID, String bleMac, double lat, double lon, int speedLimit, String message, int flag, String fileName) {
        this.workzoneID = workzoneID;
        this.bleMac = bleMac;
        this.lat = lat;
        this.lon = lon;
        this.speedLimit = speedLimit;
        this.message = message;
        this.flag = flag;
        this.fileName = fileName;
    }

    public int getWorkzoneID() {
        return workzoneID;
    }

    public void setWorkzoneID(int workzoneID) {
        this.workzoneID = workzoneID;
    }

    public String getBleMac() {
        return bleMac;
    }

    public void setBleMac(String bleMac) {
        this.bleMac = bleMac;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(int speedLimit) {
        this.speedLimit = speedLimit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String toString() {
        return "workzoneID: " + workzoneID + " bleMac: " + bleMac + " lat: "
                + lat + " lon: " + lon + " speedLimit: " + speedLimit
                + " message: " + message + " flag: " + flag + " fileName: "
                + fileName;
    }
}
