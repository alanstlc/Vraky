package com.example.vraky;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private GoogleMap mMap;
    final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Curl in main thread needs this
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);

        // Move the camera to Vrsovice
        LatLng vrsovice = new LatLng(50.069175, 14.453255);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vrsovice, 18));

        System.out.println("Loading points on the map");
        loadPoints(mMap);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {

                System.out.println("Map long clicked");

                final Marker markerName = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder.setTitle("");
                AlertDialog alertDialog = alertDialogBuilder.create();
                LayoutInflater inflater = alertDialog.getLayoutInflater();
                final View tableView = inflater.inflate(R.layout.alert_dialog_layout, null);

                alertDialogBuilder.setView(tableView);
                alertDialogBuilder.setPositiveButton("Potvrdit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Spinner brandsSpinner = tableView.findViewById(R.id.brands_spinner);
                        Spinner coloursSpinner = tableView.findViewById(R.id.colours_spinner);
                        Spinner conditionSpinner = tableView.findViewById(R.id.condition_spinner);
                        Spinner lengthSpinner = tableView.findViewById(R.id.length_spinner);

                        String brand_selected = brandsSpinner.getSelectedItem().toString();
                        String colour_selected = coloursSpinner.getSelectedItem().toString();
                        String condition_selected = conditionSpinner.getSelectedItem().toString();
                        String length_selected = Integer.toString(lengthSpinner.getSelectedItemPosition());

                        if (condition_selected.equals("Nepojízdné")) {
                            condition_selected = "0";
                        } else {
                            condition_selected = "1";
                        }

                        markerName.setTitle(colour_selected.concat(" auto značky ").concat(brand_selected));

                        String urlParameters = getInsertParameters(latLng, getUID(), brand_selected, colour_selected, length_selected, condition_selected);
                        try {
                            URL url = new URL("http://www.stolc.net/insert.php");
                            System.out.println(getResponseFromHttpUrl(url, urlParameters));
                        } catch (Exception e) {
                            System.out.println(e.fillInStackTrace());
                        }
                    }
                });

                alertDialogBuilder.setNegativeButton("Zrušit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        markerName.remove();
                    }
                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
    }

    public static void loadPoints(GoogleMap mMap) {
        String urlParameters = getBoundariesParameters(mMap);
        String json_data = "";

        try {
            URL url = new URL("http://www.stolc.net/select.php");
            json_data = getResponseFromHttpUrl(url, urlParameters);
            json_data = json_data.substring(0, json_data.length() - 1);
        } catch (Exception e) {
            System.out.println(e.fillInStackTrace());
        }

        System.out.println(json_data);

        String[] json_data_array = json_data.split("\\|");

        for (int i = 0; i < json_data_array.length; i++) {

            System.out.println(json_data_array[i]);

            try {
                JSONObject jsonObj = new JSONObject(json_data_array[i]);
                System.out.println(jsonObj.getString("user_id"));
                System.out.println(jsonObj.getString("colour"));

                LatLng latLng = new LatLng(jsonObj.getDouble("latitude"), jsonObj.getDouble("longitude"));

                Marker markerName = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                markerName.setTitle(jsonObj.getString("colour").concat(" auto značky ").concat(jsonObj.getString("brand")));

            } catch (JSONException e) {
                System.out.println(e.fillInStackTrace());
            }
        }
    }

    public static String getResponseFromHttpUrl(URL url, String urlParameters) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf8_czech_ci");
        connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        connection.setUseCaches(false);
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.write(postData);
        }

        try {
            InputStream in = connection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void onCameraIdle() {
        loadPoints(mMap);
        System.out.println("Moving!");
    }

    public String getUID() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public static String getBoundariesParameters(GoogleMap mMap) {
        LatLng position = mMap.getCameraPosition().target;
        double zoom = mMap.getCameraPosition().zoom;
        double northBound = position.latitude + 0.0005 * position.latitude / zoom;
        double southBound = position.latitude - 0.0005 * position.latitude / zoom;
        double westBound = position.longitude - 0.0035 * position.longitude / zoom;
        double eastBound = position.longitude + 0.0035 * position.longitude / zoom;
        System.out.println(northBound);
        System.out.println(southBound);
        System.out.println(westBound);
        System.out.println(eastBound);

        StringBuilder sb = new StringBuilder();
        sb.append("northBound=").append(northBound).append("&southBound=").append(southBound);
        sb.append("&westBound=").append(westBound).append("&eastBound=").append(eastBound);
        return sb.toString();
    }

    public static String getInsertParameters(LatLng latLng, String UID, String brand_selected, String colour_selected, String length_selected, String condition_selected) {
        // create String with urlParameters
        StringBuilder sb = new StringBuilder();
        sb.append("user_id=").append(UID);
        sb.append("&latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        sb.append("&brand=").append(brand_selected).append("&colour=").append(colour_selected);
        sb.append("&status=").append(condition_selected).append("&length=").append(length_selected);
        return sb.toString();
    }
}
