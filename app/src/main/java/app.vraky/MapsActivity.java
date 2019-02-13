package app.vraky;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;
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
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import static app.vraky.GetParams.getBoundariesParameters;
import static app.vraky.GetParams.getDeletePointParameters;
import static app.vraky.GetParams.getDeleteUserParameters;
import static app.vraky.GetParams.getInsertPointParameters;
import static app.vraky.GetParams.getInsertUserParameters;
import static app.vraky.GetParams.isCSSR;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private GoogleMap mMap;
    final Context context = this;
    private static final String[] INITIAL_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final String[] LOCATION_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int INITIAL_REQUEST = 1337;
    private static final int LOCATION_REQUEST = INITIAL_REQUEST + 3;

    // move the camera to Vrsovice if no network_provider found
    private LatLng startLocation = new LatLng(50.069175, 14.453255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!canAccessLocation()) {
            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }

        setContentView(R.layout.activity_maps);
        View locationButton = ((View) findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
// position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        rlp.setMargins(0, 120, 180, 0);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setTitleTextAppearance(this, R.style.TitleFont);
        myToolbar.setTitleMarginStart(50);
        myToolbar.setTitleTextColor(Color.WHITE);
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        AlertDialog alertDialog = alertDialogBuilder.create();
        LayoutInflater inflater = alertDialog.getLayoutInflater();
        if (id == R.id.oAplikaci) {
            final View infoView = inflater.inflate(R.layout.oaplikaci, null);
            // set version at info window
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = pInfo.versionName;
                TextView versionTV = infoView.findViewById(R.id.version);
                versionTV.setText("Verze ".concat(version));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            alertDialogBuilder.setView(infoView);
            alertDialogBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return true;
        }

        if (id == R.id.ovladani) {
            final View infoView = inflater.inflate(R.layout.ovladani, null);
            alertDialogBuilder.setView(infoView);
            alertDialogBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return true;
        }

        if (id == R.id.adresa) {
            final View infoView = inflater.inflate(R.layout.adresa, null);
            alertDialogBuilder.setView(infoView);
            alertDialogBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    EditText editText = infoView.findViewById(R.id.addressText);
                    Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
                    try {
                        List<Address> addresses = geoCoder.getFromLocationName(editText.getText().toString(), 5);
                        if (addresses.size() > 0) {
                            Double lat = (double) (addresses.get(0).getLatitude());
                            Double lon = (double) (addresses.get(0).getLongitude());
                            LatLng selectedLocation = new LatLng(lat, lon);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 18));
                        }
                        else {
                            Toast.makeText(MapsActivity.this, "Adresa nenalezena!",
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return true;
        }

        if (id == R.id.statistiky) {
            final View infoView = inflater.inflate(R.layout.statistiky, null);
            // get info about total count of carwrecks
            try {
                URL url = new URL(context.getResources().getString(R.string.point_count));
                String json_data = getResponseFromHttpUrl(url, "");
                json_data = json_data.substring(0, json_data.length() - 1);
                json_data.split("\\|");
                JSONObject jsonObject = new JSONObject(json_data);
                String count = jsonObject.getString("count");
                TextView countTotalTV = infoView.findViewById(R.id.countTotalTV);
                countTotalTV.setText("Počet vraků v databázi: ".concat(count));
                url = new URL(context.getResources().getString(R.string.user_count));
                json_data = getResponseFromHttpUrl(url, "user_id=".concat(getUID()));
                json_data = json_data.substring(0, json_data.length() - 1);
                json_data.split("\\|");
                jsonObject = new JSONObject(json_data);
                count = jsonObject.getString("count");
                TextView countMineTV = infoView.findViewById(R.id.countMineTV);
                countMineTV.setText("Počet mnou nahlášených vraků: ".concat(count));
            } catch (Exception e) {
                e.printStackTrace();
            }
            alertDialogBuilder.setView(infoView);
            alertDialogBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);

        if (canAccessLocation()) {
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean network_enabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (network_enabled) {
                mMap.setMyLocationEnabled(true);
                Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                startLocation = new LatLng(location.getLatitude(), location.getLongitude());
            }
        } else {
            requestPermissions(LOCATION_PERMS, LOCATION_REQUEST);
        }

        final SharedPreferences pref = MapsActivity.this.getSharedPreferences("AutovrakyPreferences", 0);
        Button infoButton = getWindow().getDecorView().findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                AlertDialog alertDialog = alertDialogBuilder.create();
                LayoutInflater inflater = alertDialog.getLayoutInflater();
                final View infoView = inflater.inflate(R.layout.oaplikaci, null);
                // set version at info window
                try {
                    PackageInfo pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
                    String version = pInfo.versionName;
                    TextView versionTV = infoView.findViewById(R.id.version);
                    versionTV.setText("Verze ".concat(version));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                // get info about total count of carwrecks
                try {
                    URL url = new URL(context.getResources().getString(R.string.point_count));
                    String json_data = getResponseFromHttpUrl(url, "");
                    json_data = json_data.substring(0, json_data.length() - 1);
                    json_data.split("\\|");
                    JSONObject jsonObject = new JSONObject(json_data);
                    String count = jsonObject.getString("count");
                    TextView countTV = infoView.findViewById(R.id.countTV);
                    countTV.setText("Počet vraků v databázi: ".concat(count));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                alertDialogBuilder.setView(infoView);
                alertDialogBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (pref.getBoolean("firstRun", true)) {
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("firstRun", false);
                            editor.commit();
                            if (canAccessLocation()) {
                                LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                boolean network_enabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                                if (network_enabled) {
                                    mMap.setMyLocationEnabled(true);
                                    Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                    startLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 18));
                                }
                            } else {
                                requestPermissions(LOCATION_PERMS, LOCATION_REQUEST);
                            }
                        }
                    }
                });
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        if (pref.getBoolean("firstRun", true)) {
            infoButton.callOnClick();
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 18));
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                AlertDialog alertDialog = alertDialogBuilder.create();
                LayoutInflater inflater = alertDialog.getLayoutInflater();

                // check if the point is in Czech Or Slovak Republic
                if (isCSSR(latLng)) {

                    // check whether any marker was clicked
                    JSONObject clickedMarker = proximityCheck(mMap, latLng);
                    if (clickedMarker != null) {
                        try {
                            alreadyClickedMarker(clickedMarker);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        // check if zoom isn't too low
                        if (mMap.getCameraPosition().zoom > 16) {
                            // no marker exit on the point as of yet, create one
                            try {
                                final Marker markerName = mMap.addMarker(new MarkerOptions().position(latLng).icon(
                                        getIcon("Černé")));
                                final View tableView = inflater.inflate(R.layout.alert_dialog_layout, null);

                                TextView sv_text = tableView.findViewById(R.id.sv_link_alert);
                                sv_text.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        openStreetView(latLng);
                                    }
                                });

                                final Spinner coloursSpinner = tableView.findViewById(R.id.colours_spinner);
                                coloursSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                                        ImageView carWreck = tableView.findViewById(R.id.carWreckNew);
                                        String colour_selected = coloursSpinner.getSelectedItem().toString();
                                        carWreck.setImageBitmap(getIconBitmap(colour_selected));
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parentView) {
                                    }
                                });

                                alertDialogBuilder.setView(tableView);
                                alertDialogBuilder.setPositiveButton("Potvrdit", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        Spinner brandsSpinner = tableView.findViewById(R.id.brands_spinner);
                                        Spinner coloursSpinner = tableView.findViewById(R.id.colours_spinner);

                                        String brand_selected = brandsSpinner.getSelectedItem().toString();
                                        String colour_selected = coloursSpinner.getSelectedItem().toString();
                                        if (colour_selected.equals("Jiné")) {
                                            markerName.setTitle("Auto značky ".concat(brand_selected));
                                            if (brand_selected.equals("Jiné")) {
                                                markerName.setTitle("Auto neznámé značky a barvy");
                                            }
                                        } else {
                                            if (brand_selected.equals("Jiné")) {
                                                markerName.setTitle(colour_selected.concat(" auto neznámé značky"));
                                            } else {
                                                markerName.setTitle(colour_selected.concat(" auto značky ").concat(brand_selected));
                                            }
                                        }
                                        markerName.setIcon(getIcon(colour_selected));
                                        String urlParameters = getInsertPointParameters(latLng, brand_selected, colour_selected);
                                        // insert point to points table
                                        try {
                                            URL url = new URL(context.getResources().getString(R.string.insert_point));
                                            getResponseFromHttpUrl(url, urlParameters);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        // insert user's rating to table
                                        urlParameters = getInsertUserParameters(latLng, getUID(), 1);
                                        try {
                                            URL url = new URL(context.getResources().getString(R.string.insert_user));
                                            getResponseFromHttpUrl(url, urlParameters);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });


                                alertDialogBuilder.setNegativeButton("Zrušit", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        markerName.remove();
                                    }
                                });
                                alertDialog = alertDialogBuilder.create();
                                alertDialog.setCanceledOnTouchOutside(false);
                                alertDialog.show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            alertDialogBuilder.setMessage("Zaměřte prosím mapu větším zoomem pro lepší přesnost");
                            alertDialogBuilder.setNegativeButton("Dobrá", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                            alertDialog = alertDialogBuilder.create();
                            alertDialog.setCanceledOnTouchOutside(false);
                            alertDialog.show();
                        }
                    }

                } else {
                    alertDialogBuilder.setMessage("Zkuste se prosím držet pouze České a Slovenské republiky");
                    alertDialogBuilder.setNegativeButton("Zkusím to", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    alertDialog = alertDialogBuilder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();
                }
            }
        });
    }


    // load markers from db
    public String[] getMarkers(GoogleMap mMap) {
        // prevention against too mmuch data
        if (mMap.getCameraPosition().zoom > 12) {
            String urlParameters = getBoundariesParameters(mMap);
            String json_data = "";
            try {
                URL url = new URL(context.getResources().getString(R.string.select_points));
                json_data = getResponseFromHttpUrl(url, urlParameters);
                json_data = json_data.substring(0, json_data.length() - 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return json_data.split("\\|");
        }
        return null;
    }

    // show markers on the map
    public void addMarkers(GoogleMap mMap, String[] json_data_array) {
        if (json_data_array != null) {
            for (int i = 0; i < json_data_array.length; i++) {
                try {
                    JSONObject jsonObj = new JSONObject(json_data_array[i]);
                    LatLng latLng = new LatLng(jsonObj.getDouble("latitude"), jsonObj.getDouble("longitude"));
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).icon(getIcon(jsonObj.getString("colour")));
                    Marker markerName = mMap.addMarker(markerOptions);
                    String brand_selected = jsonObj.getString("brand");
                    String colour_selected = jsonObj.getString("colour");
                    if (colour_selected.equals("Jiné")) {
                        markerName.setTitle("Auto značky ".concat(brand_selected));
                        if (brand_selected.equals("Jiné")) {
                            markerName.setTitle("Auto neznámé značky a barvy");
                        }
                    } else {
                        if (brand_selected.equals("Jiné")) {
                            markerName.setTitle(colour_selected.concat(" auto neznámé značky"));
                        } else {
                            markerName.setTitle(colour_selected.concat(" auto značky ").concat(brand_selected));
                        }
                    }
                    markerName.setIcon(getIcon(colour_selected));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public BitmapDescriptor getIcon(String colour) {
        Drawable carWreck = context.getResources().getDrawable(R.drawable.ic_directions_car_black_24dp);
        carWreck.setColorFilter(Color.parseColor(getColourCode(colour)), PorterDuff.Mode.SRC_IN);
        Bitmap bitmap = drawableToBitmap(carWreck);
        Bitmap icon = addShadow(bitmap, bitmap.getHeight(), bitmap.getWidth(), Color.BLACK, 1, 1, 1);
        return BitmapDescriptorFactory.fromBitmap(icon);
    }

    public Bitmap getIconBitmap(String colour) {
        Drawable carWreck = context.getResources().getDrawable(R.drawable.ic_directions_car_black_24dp);
        carWreck.setColorFilter(Color.parseColor(getColourCode(colour)), PorterDuff.Mode.SRC_IN);
        Bitmap bitmap = drawableToBitmap(carWreck);
        return addShadow(bitmap, bitmap.getHeight(), bitmap.getWidth(), Color.BLACK, 1, 1, 1);
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

    public Bitmap addShadow(final Bitmap bm, final int dstHeight, final int dstWidth, int color, int size, float dx, float dy) {
        final Bitmap mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8);

        final Matrix scaleToFit = new Matrix();
        final RectF src = new RectF(0, 0, bm.getWidth(), bm.getHeight());
        final RectF dst = new RectF(0, 0, dstWidth - dx, dstHeight - dy);
        scaleToFit.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);

        final Matrix dropShadow = new Matrix(scaleToFit);
        dropShadow.postTranslate(dx, dy);

        final Canvas maskCanvas = new Canvas(mask);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskCanvas.drawBitmap(bm, scaleToFit, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        maskCanvas.drawBitmap(bm, dropShadow, paint);

        final BlurMaskFilter filter = new BlurMaskFilter(size, BlurMaskFilter.Blur.NORMAL);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setMaskFilter(filter);
        paint.setFilterBitmap(true);

        final Bitmap ret = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        final Canvas retCanvas = new Canvas(ret);
        retCanvas.drawBitmap(mask, 0, 0, paint);
        retCanvas.drawBitmap(bm, scaleToFit, null);
        mask.recycle();
        return ret;
    }

    // communication with DB
    public String getResponseFromHttpUrl(URL url, String urlParameters) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (urlParameters.equals("")) {
            urlParameters = getConnectionParameters();
        } else {
            urlParameters = getConnectionParameters().concat("&").concat(urlParameters);
        }
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
        if (!isNetworkAvailable()) {
            AlertDialog alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setTitle("Chybí připojení k internetu!");
            alertDialog.setMessage("Připojte se prosím k internetu a znovu spusťte aplikaci, bez internetu je aplikace zatím bohužel nepoužitelná.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Zavřít aplikaci",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    });
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
        addMarkers(mMap, getMarkers(mMap));
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                LatLng markerLatLng = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
                try {
                    URL url = new URL(context.getResources().getString(R.string.select_point));
                    String urlParameters = getDeletePointParameters(markerLatLng);
                    String json_data = getResponseFromHttpUrl(url, urlParameters);
                    if (!json_data.equals("0")) {
                        json_data = json_data.substring(0, json_data.length() - 1);
                        String[] json_data_array = json_data.split("\\|");
                        JSONObject jsonObj = new JSONObject(json_data_array[0]);
                        alreadyClickedMarker(jsonObj);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return new int[0];
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void openStreetView(LatLng latLng) {
        Uri gmmIntentUri = Uri.parse("google.streetview:cbll=" + latLng.latitude + "," + latLng.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public boolean contains(final int[] array, final int key) {
        return ArrayUtils.contains(array, key);
    }

    public void alreadyClickedMarker(JSONObject clickedMarker) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        AlertDialog alertDialog = alertDialogBuilder.create();
        LayoutInflater inflater = alertDialog.getLayoutInflater();
        final View tableView = inflater.inflate(R.layout.confirm_dialog_layout, null);
        alertDialogBuilder.setView(tableView);
        LatLng latLng = null;
        try {
            latLng = new LatLng(clickedMarker.getDouble("latitude"), clickedMarker.getDouble("longitude"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        final LatLng markerLatLng = latLng;

        try {
            TextView brandTW = tableView.findViewById(R.id.brand_selected);
            brandTW.setText(clickedMarker.getString("brand"));
            TextView colourTW = tableView.findViewById(R.id.colour_selected);
            colourTW.setText(clickedMarker.getString("colour"));
            ImageView carWreck = tableView.findViewById(R.id.carWreckConfirm);
            carWreck.setImageBitmap(getIconBitmap(clickedMarker.getString("colour")));
            TextView sv_text = tableView.findViewById(R.id.sv_link_confirm);
            sv_text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openStreetView(markerLatLng);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
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
        if (userPointRating != null) {
            confirmationTW.setVisibility(View.VISIBLE);
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
                            e.printStackTrace();
                        }
                    }
                    // insert user's positive rating
                    String urlParameters = getInsertUserParameters(markerLatLng, getUID(), 1);
                    try {
                        URL url = new URL(context.getResources().getString(R.string.insert_user));
                        getResponseFromHttpUrl(url, urlParameters);
                    } catch (Exception e) {
                        e.printStackTrace();
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
                            e.printStackTrace();
                        }
                        // delete point
                        urlParameters = getDeletePointParameters(markerLatLng);
                        try {
                            URL url = new URL(context.getResources().getString(R.string.delete_point));
                            getResponseFromHttpUrl(url, urlParameters);
                        } catch (Exception e) {
                            e.printStackTrace();
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
                            e.printStackTrace();
                        }

                        if (contains(ratingsWithPoint(markerLatLng), 1)) {
                            // insert user's negative rating
                            urlParameters = getInsertUserParameters(markerLatLng, getUID(), 0);
                            try {
                                URL url = new URL(context.getResources().getString(R.string.insert_user));
                                getResponseFromHttpUrl(url, urlParameters);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else { // last positive rating, delete all negative ratings
                            urlParameters = getDeletePointParameters(markerLatLng);
                            try {
                                URL url = new URL(context.getResources().getString(R.string.delete_users));
                                getResponseFromHttpUrl(url, urlParameters);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // delete point from map
                            urlParameters = getDeletePointParameters(markerLatLng);
                            try {
                                URL url = new URL(context.getResources().getString(R.string.delete_point));
                                getResponseFromHttpUrl(url, urlParameters);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mMap.clear();
                            addMarkers(mMap, getMarkers(mMap));
                        }
                    }
                }
            });
        }
        alertDialogBuilder.setNegativeButton("Zrušit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }
}
