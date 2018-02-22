package com.example.derekdai.emergencyalert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback{

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;
    private UiSettings mUiSettings;
    private Hashtable<Marker, JSONObject> markerTable;
    private final String url = "http://cloudserver.carma-cam.com:9001";
    private final String urlReadAll = "/readAll";
    private final String urlDownload = "/downloadFile/";
    //TODO needs to be changed to emergencyALERTS
    private final String collection = "baddriverreports";
    private RequestQueue requestQueue;
    private Intent goToVideo;
    private final String key = "record";
    //declare variables for timer
    private Handler handler;
    private int elapsedTime = 1000 * 60 * 5;

    private LocationManager locationManager;
    private Criteria criteria;
    private final double mile2M = 1.60934 * 1000;
    private int defaultRadius = 10; //miles

    private Location location;
    private CircleOptions circleOptions;
    private Circle circle;

    //declare variables to play sound when update marker
    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instantiate the request queue
        requestQueue = Volley.newRequestQueue(this);

        //initialize url and hashtable which stores mapping between _id and JSONOBject
        markerTable = new Hashtable<>();
        goToVideo = new Intent(MainActivity.this, VideoActivity.class);
        sendEmergencyAlertReportRequest();

        //initialize timer (handler), 5 minus
        handler = new Handler();
        handler.postDelayed(runnable, elapsedTime);

        //initialize MediaPlayer
        mp = MediaPlayer.create(getApplicationContext(), R.raw.notification4update);

        //initialize location manager
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mUiSettings = mMap.getUiSettings();

        // Keep the UI Settings state in sync with the checkboxes.
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);

        enableMyLocation();

    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        try {
            if(circle == null) {
                location = locationManager.getLastKnownLocation(locationManager
                        .getBestProvider(criteria, false));
                //draw the circle to show the radius in default value
                circleOptions = new CircleOptions().center(new LatLng(location.getLatitude(),
                        location.getLongitude())).radius((int) (defaultRadius * mile2M)).strokeColor(0x220000FF).fillColor(0x220000FF);
                circle = mMap.addCircle(circleOptions);
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    circleOptions.getCenter(), getZoomLevel(circle)));
        }
        catch(SecurityException e){

        }
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return true;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker){
        JSONObject correspondObject = markerTable.get(marker);
        Bundle bundle = new Bundle();
        bundle.putString(key, correspondObject.toString());
        goToVideo.putExtras(bundle);
        startActivity(goToVideo);
        return true;
    }

    private void drawMarkers (JSONArray jsonArray) throws JSONException{
        mp.start();
        mMap.clear();
        markerTable.clear();
        for(int index = 0; index < jsonArray.length(); index++){
            JSONObject tempJSONObject = jsonArray.getJSONObject(index);
            String[] coordinates = tempJSONObject.getString("location").split(",");
            if(coordinates[0].equals("null") || coordinates[1].equals("null")){
                continue;
            }
            LatLng tempPoint = new LatLng(Double.parseDouble(coordinates[0]),
                    Double.parseDouble(coordinates[1]));
            Marker tempMarker = mMap.addMarker(new MarkerOptions().position(tempPoint)
                    .title(tempJSONObject.getString("time")));
            markerTable.put(tempMarker, tempJSONObject);

        }

    }

    private void sendEmergencyAlertReportRequest(){
        //send out the post request to back-end API for all emergency alert reports
        StringRequest emergencyAlertsReportRequest = new StringRequest(Request.Method.POST,
                url + urlReadAll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONArray jsonResponse = new JSONArray(response);
                            drawMarkers(jsonResponse);
                        }
                        catch(JSONException e){

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        ){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String>  params = new HashMap<>();
                // the POST parameters:
                params.put("collection", collection);
                params.put("network", "tutsplus");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        requestQueue.add(emergencyAlertsReportRequest);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            sendEmergencyAlertReportRequest();
            //set the delay again to implement the timer
            handler.postDelayed(this, elapsedTime);
        }
    };

    //helper function calculate zoom level based on circle size
    public int getZoomLevel(Circle circle) {
        int zoomLevel = 11;
        if (circle != null) {
            double radius = circle.getRadius() + circle.getRadius() / 2;
            double scale = radius / 500;
            zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel;
    }
}
