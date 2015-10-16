package com.umn.mto.android.workzonealert.interfaces;

import android.location.Location;

import java.util.List;

/**
 * Created by Sandeep on 8/23/2015.
 */
public interface RaysAlgorithm {
    boolean isPointInPolygon(List<Location> polygonPoints, Location current);
}
