package com.umn.mto.android.workzonealert;

import android.location.Location;

import com.umn.mto.android.workzonealert.interfaces.RaysAlgorithm;

import java.util.List;

/**
 * Created by Sandeep on 8/23/2015.
 */
public class LocationValidator implements RaysAlgorithm {
    @Override
    public boolean isPointInPolygon(List<Location> polygonPoints, Location current) {
        int length = polygonPoints.size();
        int count = 0;
        for (int i = 1; i < length - 1; i++) {
            if (checkIntersect(polygonPoints.get(i - 1), polygonPoints.get(i), current)) {
                count++;
            }
        }
        return ((count % 2) == 1);
    }

    private boolean checkIntersect(Location locA, Location locB, Location cur) {
        double aY = locA.getLatitude();
        double bY = locB.getLatitude();
        double aX = locA.getLongitude();
        double bX = locB.getLongitude();
        double pY = cur.getLatitude();
        double pX = cur.getLongitude();

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
            return false;
        }
        double m = (aY - bY) / (aX - bX);
        double b = (-aX) * m + aY;
        double x = (pY - b) / m;
        return x > pX;
    }


}
