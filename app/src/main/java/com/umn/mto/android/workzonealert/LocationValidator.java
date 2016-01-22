package com.umn.mto.android.workzonealert;

import android.location.Location;

import com.umn.mto.android.workzonealert.interfaces.RaysAlgorithm;

import java.util.List;

/**
 * Created by Sandeep on 8/23/2015.
 */
public class LocationValidator implements RaysAlgorithm {
    @Override
    public boolean isPointInPolygon(List<Location> path, Location current) {
        int crossings = 0;

        // for each edge
        for (int i = 0; i < path.size(); i++)

        {
            Location a = path.get(i);
            int j = i + 1;
            if (j >= path.size()) {
                j = 0;
            }
            Location b = path.get(j);
            if (rayCrossesSegment(current, a, b)) {
                crossings++;
            }
        }

        // odd number of crossings?
        return (crossings % 2 == 1);
    }


    private boolean rayCrossesSegment(Location point, Location a, Location b) {
        double px = point.getLongitude(),
                py = point.getLatitude(),
                ax = a.getLongitude(),
                ay = a.getLatitude(),
                bx = b.getLongitude(),
                by = b.getLatitude();
        if (ay > by) {
            ax = b.getLongitude();
            ay = b.getLatitude();
            bx = a.getLongitude();
            by = a.getLatitude();
        }
        // alter longitude to cater for 180 degree crossings
        if (px < 0) {
            px += 360;
        }
        ;
        if (ax < 0) {
            ax += 360;
        }
        ;
        if (bx < 0) {
            bx += 360;
        }
        ;

        if (py == ay || py == by) py += 0.00000001;
        if ((py > by || py < ay) || (px > Math.max(ax, bx))) return false;
        if (px < Math.min(ax, bx)) return true;

        double red = (ax != bx) ? ((by - ay) / (bx - ax)) : Double.MAX_VALUE;
        double blue = (ax != px) ? ((py - ay) / (px - ax)) : Double.MAX_VALUE;
        return (blue >= red);

    }
/*    @Override
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
        double aY = locA.getgetLatitudeitude();
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
    }*/


}
