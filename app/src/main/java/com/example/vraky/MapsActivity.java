package com.example.vraky;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

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
    private static double latitude_constant = 0.0005;
    private static double longitude_constant = 0.0035;

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

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                AlertDialog alertDialog = alertDialogBuilder.create();
                LayoutInflater inflater = alertDialog.getLayoutInflater();

                // Check whether any marker was clicked
                JSONObject clickedMarker = proximityCheck(mMap, latLng, 18);
                if (clickedMarker != null) {

                    final View tableView = inflater.inflate(R.layout.confirm_dialog_layout, null);
                    alertDialogBuilder.setView(tableView);

                    try {
                        final LatLng markerLatLng = new LatLng(clickedMarker.getDouble("latitude"), clickedMarker.getDouble("longitude"));

                        try {
                            TextView brandTW = tableView.findViewById(R.id.brand_selected);
                            brandTW.setText(clickedMarker.getString("brand"));
                            TextView colourTW = tableView.findViewById(R.id.colour_selected);
                            colourTW.setText(clickedMarker.getString("colour"));
                        } catch (Exception e) {
                            System.out.println(e.fillInStackTrace());
                        }

                        alertDialogBuilder.setPositiveButton("Potvrdit vrak", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // insert user to users table
                                String urlParameters = getInsertUserParameters(markerLatLng, getUID(), 1);
                                try {
                                    URL url = new URL(context.getResources().getString(R.string.insert_user));
                                    getResponseFromHttpUrl(url, urlParameters);
                                } catch (Exception e) {
                                    System.out.println(e.fillInStackTrace());
                                }
                            }
                        });
                        alertDialogBuilder.setNeutralButton("Vrak tu není", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // insert user to users table
                                String urlParameters = getInsertUserParameters(markerLatLng, getUID(), 0);
                                try {
                                    URL url = new URL(context.getResources().getString(R.string.insert_user));
                                    getResponseFromHttpUrl(url, urlParameters);
                                } catch (Exception e) {
                                    System.out.println(e.fillInStackTrace());
                                }
                            }
                        });
                        alertDialogBuilder.setNegativeButton("Zrušit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });

                    } catch (Exception e) {
                        System.out.println(e.fillInStackTrace());
                    }
                } else {

                    final Marker markerName = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

                    final View tableView = inflater.inflate(R.layout.alert_dialog_layout, null);
                    alertDialogBuilder.setView(tableView);
                    alertDialogBuilder.setPositiveButton("Potvrdit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            Spinner brandsSpinner = tableView.findViewById(R.id.brands_spinner);
                            Spinner coloursSpinner = tableView.findViewById(R.id.colours_spinner);
                            Spinner conditionSpinner = tableView.findViewById(R.id.condition_spinner);

                            String brand_selected = brandsSpinner.getSelectedItem().toString();
                            String colour_selected = coloursSpinner.getSelectedItem().toString();
                            String condition_selected = conditionSpinner.getSelectedItem().toString();

                            if (condition_selected.equals("Nepojízdné")) {
                                condition_selected = "0";
                            } else {
                                condition_selected = "1";
                            }

                            markerName.setTitle(colour_selected.concat(" auto značky ").concat(brand_selected));
                            String urlParameters = getInsertPointParameters(latLng, brand_selected, colour_selected);
                            // insert point to points table
                            try {
                                URL url = new URL(context.getResources().getString(R.string.insert_point));
                                getResponseFromHttpUrl(url, urlParameters);
                            } catch (Exception e) {
                                System.out.println(e.fillInStackTrace());
                            }

                            // insert user to users table

                            urlParameters = getInsertUserParameters(latLng, getUID(), 1);
                            try {
                                URL url = new URL(context.getResources().getString(R.string.insert_user));
                                getResponseFromHttpUrl(url, urlParameters);
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
                }
                alertDialog = alertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
        });
    }

    // get markers from db
    public String[] getMarkers(GoogleMap mMap) {
        String urlParameters = getBoundariesParameters(mMap);
        String json_data = "";

        try {
            URL url = new URL(context.getResources().getString(R.string.select_points));
            json_data = getResponseFromHttpUrl(url, urlParameters);
            json_data = json_data.substring(0, json_data.length() - 1);
        } catch (Exception e) {
            System.out.println(e.fillInStackTrace());
        }
        return json_data.split("\\|");
    }

    public static void addMarkers(GoogleMap mMap, String[] json_data_array) {
        for (int i = 0; i < json_data_array.length; i++) {
            try {
                JSONObject jsonObj = new JSONObject(json_data_array[i]);
                LatLng latLng = new LatLng(jsonObj.getDouble("latitude"), jsonObj.getDouble("longitude"));

                MarkerOptions markerOptions = new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_CYAN));

                Marker markerName = mMap.addMarker(markerOptions);
                markerName.setTitle(jsonObj.getString("colour").concat(" auto značky ").concat(jsonObj.getString("brand")));
            } catch (JSONException e) {
                System.out.println(e.fillInStackTrace());
            }
        }
    }

    // communication with DB
    public String getResponseFromHttpUrl(URL url, String urlParameters) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        urlParameters = getConnectionParameters().concat("&").concat(urlParameters);

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
        addMarkers(mMap, getMarkers(mMap));
        System.out.println("Moving!");
    }

    public String getUID() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    // create String with urlParameters
    public static String getBoundariesParameters(GoogleMap mMap) {
        LatLng position = mMap.getCameraPosition().target;
        double zoom = mMap.getCameraPosition().zoom;
        double northBound = position.latitude + latitude_constant * position.latitude / zoom;
        double southBound = position.latitude - latitude_constant * position.latitude / zoom;
        double westBound = position.longitude - longitude_constant * position.longitude / zoom;
        double eastBound = position.longitude + longitude_constant * position.longitude / zoom;

        StringBuilder sb = new StringBuilder();
        sb.append("northBound=").append(northBound).append("&southBound=").append(southBound);
        sb.append("&westBound=").append(westBound).append("&eastBound=").append(eastBound);
        return sb.toString();
    }

    // create String with connection urlParameters
    public String getConnectionParameters() {
        StringBuilder sb = new StringBuilder();
        sb.append("servername=").append(context.getResources().getString(R.string.servername));
        sb.append("&dbname=").append(context.getResources().getString(R.string.dbname));
        sb.append("&username=").append(context.getResources().getString(R.string.username));
        sb.append("&password=").append(context.getResources().getString(R.string.password));
        return sb.toString();
    }

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

    // check for markers within radius
    public JSONObject proximityCheck(GoogleMap mMap, LatLng latLng, int radius) {
        String[] json_data_array = getMarkers(mMap);
        for (int i = 0; i < json_data_array.length; i++) {
            try {
                JSONObject jsonObj = new JSONObject(json_data_array[i]);

                float[] results = new float[1];
                Location.distanceBetween(jsonObj.getDouble("latitude"), jsonObj.getDouble("longitude"), latLng.latitude, latLng.longitude, results);
                if (results[0] < radius) {
                    return jsonObj;
                }
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }
        return null;
    }
}
