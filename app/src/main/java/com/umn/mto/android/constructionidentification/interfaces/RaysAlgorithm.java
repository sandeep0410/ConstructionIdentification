package com.umn.mto.android.constructionidentification.interfaces;

import android.location.Location;

import java.util.ArrayList;

/**
 * Created by Sandeep on 8/23/2015.
 */
public interface RaysAlgorithm {
    boolean isPointInPolygon(ArrayList<Location> polygonPoints, Location current);
}
