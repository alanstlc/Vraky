package com.example.vraky;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

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
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Move the camera to Vrsovice
        LatLng vrsovice = new LatLng(50.069175, 14.453255);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vrsovice, 18));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

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

                        String brand_selected = brandsSpinner.getSelectedItem().toString();
                        String colour_selected = coloursSpinner.getSelectedItem().toString();
                        String condition_selected = conditionSpinner.getSelectedItem().toString();

                        markerName.setTitle(colour_selected.concat(" auto značky ").concat(brand_selected));
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
}
