package com.example.vraky;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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

import static com.example.vraky.GetParams.getBoundariesParameters;
import static com.example.vraky.GetParams.getDeletePointParameters;
import static com.example.vraky.GetParams.getDeleteUserParameters;
import static com.example.vraky.GetParams.getInsertPointParameters;
import static com.example.vraky.GetParams.getInsertUserParameters;
import static com.example.vraky.GetParams.isCSSR;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private GoogleMap mMap;
    final Context context = this;
    private static final String[] INITIAL_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS
    };
    private static final String[] LOCATION_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int INITIAL_REQUEST = 1337;
    private static final int LOCATION_REQUEST = INITIAL_REQUEST + 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!canAccessLocation()) {
            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }
        setContentView(R.layout.activity_maps);
        // obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // curl in main thread needs this
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        mMap.setMyLocationEnabled(true);

        // move the camera to Vrsovice if no network_provider found
        LatLng startLocation = new LatLng(50.069175, 14.453255);

        if (canAccessLocation()) {
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean network_enabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (network_enabled) {
                Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                startLocation = new LatLng(location.getLatitude(), location.getLongitude());
            }
        } else {
            requestPermissions(LOCATION_PERMS, LOCATION_REQUEST);
        }

        Button infoButton = getWindow().getDecorView().findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                AlertDialog alertDialog = alertDialogBuilder.create();
                LayoutInflater inflater = alertDialog.getLayoutInflater();
                final View infoView = inflater.inflate(R.layout.info_button_layout, null);
                alertDialogBuilder.setView(infoView);
                alertDialogBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
        // TODO: Uncomment when deployed to PROD!
        //infoButton.performClick();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 18));
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                AlertDialog alertDialog = alertDialogBuilder.create();
                LayoutInflater inflater = alertDialog.getLayoutInflater();

                // check if the point is in Czech Or Slovak Republic
                if (isCSSR(latLng)) {
                    // check if zoom isn't too low
                    if (mMap.getCameraPosition().zoom > 16) {

                        // check whether any marker was clicked
                        JSONObject clickedMarker = proximityCheck(mMap, latLng);
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
                                    ImageView carWreck = tableView.findViewById(R.id.carWreckConfirm);
                                    carWreck.setColorFilter(Color.parseColor(getColourCode(colourTW.getText().toString())));
                                } catch (Exception e) {
                                    System.out.println(e.fillInStackTrace());
                                }
                                // count number of raters
                                int[] ratings = ratingsWithPoint(markerLatLng);
                                TextView headcountTW = tableView.findViewById(R.id.headcount_text);
                                headcountTW.setVisibility(View.VISIBLE);
                                int positive_users = 0;
                                int negative_users = 0;
                                for (int i = 0; i < ratings.length; i++) {
                                    if (ratings[i] == 1)
                                        positive_users++;
                                    else
                                        negative_users++;
                                }
                                headcountTW.setText("Počet uživatelů, kteří nahlásili vrak: " + Integer.toString(positive_users));
                                if (negative_users > 0) {
                                    TextView fakeHeadcountTW = tableView.findViewById(R.id.fake_headcount_text);
                                    fakeHeadcountTW.setVisibility(View.VISIBLE);
                                    fakeHeadcountTW.setText("Počet uživatelů, kteří nahlásili, že tu nic není: " + Integer.toString(negative_users));
                                }
                                // check rating of the user
                                final Boolean userPointRating = userRatedPoint(markerLatLng, getUID());
                                TextView confirmationTW = tableView.findViewById(R.id.confirmation_text);
                                confirmationTW.setVisibility(View.VISIBLE);
                                if (userPointRating != null) {
                                    if (userPointRating == true) {
                                        confirmationTW.setText("Označil jsem, že tu vrak je");
                                    } else {
                                        confirmationTW.setText("Označil jsem, že tu vrak není");
                                    }
                                }

                                if (userPointRating == null || userPointRating == false) {
                                    alertDialogBuilder.setPositiveButton("Vrak tu je", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (userPointRating != null) {
                                                // delete user's negative rating
                                                String urlParameters = getDeleteUserParameters(markerLatLng, getUID());
                                                try {
                                                    URL url = new URL(context.getResources().getString(R.string.delete_user));
                                                    getResponseFromHttpUrl(url, urlParameters);
                                                } catch (Exception e) {
                                                    System.out.println(e.fillInStackTrace());
                                                }
                                            }
                                            // insert user's positive rating
                                            String urlParameters = getInsertUserParameters(markerLatLng, getUID(), 1);
                                            try {
                                                URL url = new URL(context.getResources().getString(R.string.insert_user));
                                                getResponseFromHttpUrl(url, urlParameters);
                                            } catch (Exception e) {
                                                System.out.println(e.fillInStackTrace());
                                            }
                                        }
                                    });
                                }
                                // insert user to users table
                                if (userPointRating == null || userPointRating == true) {
                                    alertDialogBuilder.setNeutralButton("Vrak tu není", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // if this is the last user of this point, delete the point and user
                                            String[] pointRaters = usersWithPoint(markerLatLng);
                                            if (pointRaters.length == 1 && pointRaters[0].equals(getUID())) {
                                                // delete user
                                                String urlParameters = getDeleteUserParameters(markerLatLng, getUID());
                                                try {
                                                    URL url = new URL(context.getResources().getString(R.string.delete_user));
                                                    getResponseFromHttpUrl(url, urlParameters);
                                                } catch (Exception e) {
                                                    System.out.println(e.fillInStackTrace());
                                                }
                                                // delete point
                                                urlParameters = getDeletePointParameters(markerLatLng);
                                                try {
                                                    URL url = new URL(context.getResources().getString(R.string.delete_point));
                                                    getResponseFromHttpUrl(url, urlParameters);
                                                } catch (Exception e) {
                                                    System.out.println(e.fillInStackTrace());
                                                }
                                                // delete point from map
                                                mMap.clear();
                                                addMarkers(mMap, getMarkers(mMap));

                                            } else {
                                                // delete user's positive rating
                                                String urlParameters = getDeleteUserParameters(markerLatLng, getUID());
                                                try {
                                                    URL url = new URL(context.getResources().getString(R.string.delete_user));
                                                    getResponseFromHttpUrl(url, urlParameters);
                                                } catch (Exception e) {
                                                    System.out.println(e.fillInStackTrace());
                                                }
                                                // insert user's negative rating
                                                urlParameters = getInsertUserParameters(markerLatLng, getUID(), 0);
                                                try {
                                                    URL url = new URL(context.getResources().getString(R.string.insert_user));
                                                    getResponseFromHttpUrl(url, urlParameters);
                                                } catch (Exception e) {
                                                    System.out.println(e.fillInStackTrace());
                                                }
                                            }
                                        }
                                    });
                                }
                                alertDialogBuilder.setNegativeButton("Zrušit", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                });

                            } catch (Exception e) {
                                System.out.println(e.fillInStackTrace());
                            }

                        } else {
                            // no marker exit on the point as of yet, create one
                            try {
                                final Marker markerName = mMap.addMarker(new MarkerOptions().position(latLng).icon(
                                        getIcon("Černé")));


                                final View tableView = inflater.inflate(R.layout.alert_dialog_layout, null);
                                final Spinner coloursSpinner = tableView.findViewById(R.id.colours_spinner);

                                coloursSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                                        ImageView carWreck = tableView.findViewById(R.id.carWreckNew);
                                        String colour_selected = coloursSpinner.getSelectedItem().toString();
                                        carWreck.setColorFilter(Color.parseColor(getColourCode(colour_selected)));
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parentView) {
                                        System.out.println("Madre mia...");
                                    }
                                });

                                alertDialogBuilder.setView(tableView);
                                alertDialogBuilder.setPositiveButton("Potvrdit", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        Spinner brandsSpinner = tableView.findViewById(R.id.brands_spinner);
                                        Spinner coloursSpinner = tableView.findViewById(R.id.colours_spinner);

                                        String brand_selected = brandsSpinner.getSelectedItem().toString();
                                        String colour_selected = coloursSpinner.getSelectedItem().toString();

                                        markerName.setTitle(colour_selected.concat(" auto značky ").concat(brand_selected));
                                        markerName.setIcon(getIcon(colour_selected));
                                        String urlParameters = getInsertPointParameters(latLng, brand_selected, colour_selected);
                                        // insert point to points table
                                        try {
                                            URL url = new URL(context.getResources().getString(R.string.insert_point));
                                            getResponseFromHttpUrl(url, urlParameters);
                                        } catch (Exception e) {
                                            System.out.println(e.fillInStackTrace());
                                        }

                                        // insert user's rating to table
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
                            } catch (Exception e) {
                                System.out.println(e.fillInStackTrace());
                            }
                        }
                    } else {
                        alertDialogBuilder.setMessage("Zaměřte prosím mapu větším zoomem pro lepší přesnost");
                        alertDialogBuilder.setNegativeButton("Dobrá", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                    }

                } else {
                    alertDialogBuilder.setMessage("Zkuste se prosím držet pouze České a Slovenské republiky");
                    alertDialogBuilder.setNegativeButton("Zkusím to", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                }
                alertDialog = alertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
        });
    }

    // load markers from db
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

    // show markers on the map
    public void addMarkers(GoogleMap mMap, String[] json_data_array) {
        for (int i = 0; i < json_data_array.length; i++) {
            try {
                JSONObject jsonObj = new JSONObject(json_data_array[i]);
                LatLng latLng = new LatLng(jsonObj.getDouble("latitude"), jsonObj.getDouble("longitude"));
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).icon(getIcon(jsonObj.getString("colour")));
                Marker markerName = mMap.addMarker(markerOptions);
                markerName.setTitle(jsonObj.getString("colour").concat(" auto značky ").concat(jsonObj.getString("brand")));
            } catch (JSONException e) {
                System.out.println(e.fillInStackTrace());
            }
        }
    }

    public BitmapDescriptor getIcon(String colour) {
        Drawable carWreck = context.getResources().getDrawable(R.drawable.ic_directions_car_black_24dp);
        carWreck.setColorFilter(Color.parseColor(getColourCode(colour)), PorterDuff.Mode.SRC_IN);
        Bitmap bitmap = drawableToBitmap(carWreck);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
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

    // when camera is still, load markers
    @Override
    public void onCameraIdle() {
        addMarkers(mMap, getMarkers(mMap));
    }

    public String getUID() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    // check for markers within radius
    public JSONObject proximityCheck(GoogleMap mMap, LatLng latLng) {
        String[] json_data_array = getMarkers(mMap);
        for (int i = 0; i < json_data_array.length; i++) {
            try {
                JSONObject jsonObj = new JSONObject(json_data_array[i]);

                float[] results = new float[1];
                double zoom = mMap.getCameraPosition().zoom;
                Location.distanceBetween(jsonObj.getDouble("latitude"), jsonObj.getDouble("longitude"), latLng.latitude, latLng.longitude, results);
                if (results[0] < 2000000 / Math.pow(2, zoom - 1)) {
                    return jsonObj;
                }
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }
        return null;
    }

    // check if user has already rated the point
    public Boolean userRatedPoint(LatLng latLng, String user_id) {
        StringBuilder sb = new StringBuilder();
        sb.append("latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        sb.append("&user_id=").append(user_id);
        String urlParameters = sb.toString();
        try {
            URL url = new URL(context.getResources().getString(R.string.select_user));
            String json_data = getResponseFromHttpUrl(url, urlParameters);
            if (!json_data.equals("0")) {
                json_data = json_data.substring(0, json_data.length() - 1);
                String[] json_data_array = json_data.split("\\|");
                JSONObject jsonObj = new JSONObject(json_data_array[0]);
                if (jsonObj.getInt("status") == 1) return true;
                else return false;
            }
        } catch (Exception e) {
            System.out.println(e.fillInStackTrace());
        }
        return null;
    }

    // return array of users with this point
    public String[] usersWithPoint(LatLng latLng) {
        StringBuilder sb = new StringBuilder();
        sb.append("latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        String urlParameters = sb.toString();
        try {
            URL url = new URL(context.getResources().getString(R.string.select_users));
            String json_data = getResponseFromHttpUrl(url, urlParameters);
            if (!json_data.equals("0")) {
                json_data = json_data.substring(0, json_data.length() - 1);
                String[] json_data_array = json_data.split("\\|");
                String[] users_array = new String[json_data_array.length];
                for (int i = 0; i < json_data_array.length; i++) {
                    try {
                        JSONObject jsonObj = new JSONObject(json_data_array[i]);
                        users_array[i] = jsonObj.getString("user_id");
                    } catch (Exception e) {
                        e.fillInStackTrace();
                    }
                }
                return users_array;
            }
        } catch (Exception e) {
            System.out.println(e.fillInStackTrace());
        }
        return null;
    }

    // return array of ratings with this point
    public int[] ratingsWithPoint(LatLng latLng) {
        StringBuilder sb = new StringBuilder();
        sb.append("latitude=").append(latLng.latitude).append("&longitude=").append(latLng.longitude);
        String urlParameters = sb.toString();
        try {
            URL url = new URL(context.getResources().getString(R.string.select_users));
            String json_data = getResponseFromHttpUrl(url, urlParameters);
            if (!json_data.equals("0")) {
                json_data = json_data.substring(0, json_data.length() - 1);
                String[] json_data_array = json_data.split("\\|");
                int[] ratings_array = new int[json_data_array.length];
                for (int i = 0; i < json_data_array.length; i++) {
                    try {
                        JSONObject jsonObj = new JSONObject(json_data_array[i]);
                        ratings_array[i] = jsonObj.getInt("status");
                    } catch (Exception e) {
                        e.fillInStackTrace();
                    }
                }
                return ratings_array;
            }
        } catch (Exception e) {
            System.out.println(e.fillInStackTrace());
        }
        return null;
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

    private boolean canAccessLocation() {
        return (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    }

    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == checkSelfPermission(perm));
    }

    private String getColourCode(String colour) {
        String[] colours = context.getResources().getStringArray(R.array.colours);
        String[] colour_codes = context.getResources().getStringArray(R.array.colour_codes);
        for (int i = 0; i < colours.length; i++) {
            if (colour.equals(colours[i])) {
                return colour_codes[i];
            }
        }
        return null;
    }

}
