package com.umn.mto.android.constructionidentification.com.umn.mto.android.constructionidentification.dto;

/**
 * Created by Sandeep on 10/2/2015.
 */
public class WorkZonePoint {
    private int id;
    private int pointId;
    private double lat;
    private double lon;

    public WorkZonePoint(int id, int pointId, double lat, double lon) {
        this.id = id;
        this.pointId = pointId;
        this.lat = lat;
        this.lon = lon;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPointId() {
        return pointId;
    }

    public void setPointId(int pointId) {
        this.pointId = pointId;
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
    public String toString(){
        return "id: " +id +" pointID: " +pointId +" lat: " +lat +" lon: " +lon;
    }
}
