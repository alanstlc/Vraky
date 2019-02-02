package com.example.vraky;

import com.google.android.gms.maps.model.LatLng;

public class GetParams {

    // create String with urlParameters for points table
    public static String getInsertPointParameters(LatLng latLng, String brand_selected, String colour_selected) {
        StringBuilder sb = new StringBuilder();
        sb.append("&latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        sb.append("&brand=").append(brand_selected).append("&colour=").append(colour_selected);
        sb.append("&status=1");
        return sb.toString();
    }

    // create String with urlParameters for users table
    public static String getInsertUserParameters(LatLng latLng, String UID, int status) {
        StringBuilder sb = new StringBuilder();
        sb.append("user_id=").append(UID).append("&status=").append(status);
        sb.append("&latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        return sb.toString();
    }

    // create String with urlParameters for users table
    public static String getDeletePointParameters(LatLng latLng) {
        StringBuilder sb = new StringBuilder();
        sb.append("latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        return sb.toString();
    }

    // create String with urlParameters for users table
    public static String getDeleteUserParameters(LatLng latLng, String UID) {
        StringBuilder sb = new StringBuilder();
        sb.append("user_id=").append(UID);
        sb.append("&latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        return sb.toString();
    }
}
