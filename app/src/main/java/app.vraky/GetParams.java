package app.vraky;

import com.google.android.gms.maps.GoogleMap;
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

    // create String with urlParameters
    public static String getBoundariesParameters(GoogleMap mMap) {
        LatLng position = mMap.getCameraPosition().target;
        double zoom = mMap.getCameraPosition().zoom;
        double northBound = position.latitude + 1 / Math.pow(2, zoom - 1) / 0.005;
        double southBound = position.latitude - 1 / Math.pow(2, zoom - 1) / 0.005;
        double westBound = position.longitude - 1 / Math.pow(2, zoom - 1) / 0.005;
        double eastBound = position.longitude + 1 / Math.pow(2, zoom - 1) / 0.005;

        StringBuilder sb = new StringBuilder();
        sb.append("northBound=").append(northBound).append("&southBound=").append(southBound);
        sb.append("&westBound=").append(westBound).append("&eastBound=").append(eastBound);
        return sb.toString();
    }

    public static Boolean isCSSR(LatLng latLng) {
        if (47.5 < latLng.latitude && latLng.latitude < 51 && 12 < latLng.longitude && latLng.longitude < 22.5)
            return true;
        else return false;
    }
}
