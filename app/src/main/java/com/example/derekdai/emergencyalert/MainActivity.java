package com.example.derekdai.emergencyalert;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.View;
import android.widget.Button;
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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
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
    private Hashtable<Marker, String> markerTable;
    private final String url = "http://cloudserver.carma-cam.com:9001";
    private final String urlReadAll = "/readAll";
    private final String urlReadByRaidus = "/readByRadius";
    private final String urlDownload = "/downloadFile/";
    //TODO needs to be changed to emergencyALERTS
    private final String collectionBad = "baddriverreports";
    private final String collectionEmergency = "emergencyalerts";
    private RequestQueue requestQueue;

    private Intent goToVideo;
    private final String key = "record";
    private final int videoCode = 2;

    private Intent goToSetting;
    private final String settingKey = "setting";
    private final int settingCode = 1;

    //declare variables for timer
    private Handler handler;
    private int elapsedTime = 10; //minutes

    private LocationManager locationManager;
    private Criteria criteria;
    private final double mile2M = 1.60934 * 1000;
    private int radius = 5; //miles

    private Location location;
    private CircleOptions circleOptions;
    private Circle circle;

    private SharedPreferences sharedPref;

    private JSONArray emergencyAlerts = null, badDriverReports = null;

    private JSONObject actions;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            emergencyAlerts = null; badDriverReports = null;
            sendBadDriverReportRequest();
            sendEmergencyAlertsRequest();
            //clear up shared preference if it is updated 30 minutes ago
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Long currentTime = System.currentTimeMillis();
            Iterator<String> iterator = actions.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                try {
                    String timeStamp = actions.getJSONObject(key).getString("timeStamp");
                    Long timeStampMiliSec = timeFormat.parse(timeStamp).getTime();
                    if(currentTime - timeStampMiliSec > 1800000){
                        iterator.remove();
                    }
                } catch (Exception e) {
                    // Something went wrong!
                }
            }
            //set the delay again to implement the timer
            handler.postDelayed(this, elapsedTime * 60 * 1000);
        }
    };

    //declare variables to play sound when update marker
    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instantiate the request queue
        requestQueue = Volley.newRequestQueue(this);

        //set the sharedPref to the shared preference of this app
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        //initialize the elapsedTime to 5 minutes and radius to 10 miles
        elapsedTime = sharedPref.getInt("elapsedTime", 5);
        radius = sharedPref.getInt("radius", 10);
        try {
            actions = new JSONObject(sharedPref.getString("actions", "{}"));
        } catch(JSONException e){
            System.out.println(e.getMessage());
        }

        //initialize url and hashtable which stores mapping between _id and JSONOBject
        markerTable = new Hashtable<>();
        goToVideo = new Intent(MainActivity.this, VideoActivity.class);

        goToSetting = new Intent(MainActivity.this, SettingActivity.class);
        //initialize timer (handler), 5 minus
        handler = new Handler();
        handler.postDelayed(runnable, elapsedTime * 60 * 1000);

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
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                try {
                    location = locationManager.getLastKnownLocation(locationManager
                            .getBestProvider(criteria, false));
                    sendBadDriverReportRequest();
                    sendEmergencyAlertsRequest();
                    //draw the circle to show the radius in default value
                    circleOptions = new CircleOptions().center(new LatLng(location.getLatitude(),
                            location.getLongitude())).radius((int) (radius * mile2M)).strokeColor(0x220000FF).fillColor(0x220000FF);
                    circle = mMap.addCircle(circleOptions);
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
                    mMap.moveCamera(cameraUpdate);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            circleOptions.getCenter(), getZoomLevel(circle)));
                } catch (SecurityException e) {

                }
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {
                // TODO Auto-generated method stub
                //simulate the long click on marker
                Toast.makeText(MainActivity.this, "long click!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // TODO Auto-generated method stub

            }
        });


        mMap.setOnMarkerClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mUiSettings = mMap.getUiSettings();
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
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                circleOptions.getCenter(), getZoomLevel(circle)));
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
    public boolean onMarkerClick (final Marker marker){
        marker.showInfoWindow();
        return true;
    }

    @Override
    public void onInfoWindowClick(final Marker marker){
        try {
            JSONObject correspondObject = new JSONObject(markerTable.get(marker));
            String id = correspondObject.getString("_id");
            if(actions.has(id)){
                correspondObject.put("action", actions.get(id));
            }
            Bundle bundle = new Bundle();
            bundle.putString(key, correspondObject.toString());
            goToVideo.putExtras(bundle);
            startActivityForResult(goToVideo, videoCode);
        } catch (JSONException e){
            System.out.println(e.getMessage());
        }
    }

    private void drawMarkers () throws JSONException, ParseException{
        /*boolean hasNew = false;
        Hashtable<Marker, String> newMarkerTable = new Hashtable<>();
        //add new markers
        for(int index = 0; index < badDriverReports.length(); index++){
            JSONObject tempJSONObject = badDriverReports.getJSONObject(index);
            String[] coordinates = tempJSONObject.getString("location").split(",");
            if(coordinates[0].equals("null") || coordinates[1].equals("null")){
                continue;
            }
            if(!markerTable.containsValue(tempJSONObject.toString())){
                hasNew = true;
            }
            LatLng tempPoint = new LatLng(Double.parseDouble(coordinates[0]),
                    Double.parseDouble(coordinates[1]));
            Marker tempMarker = mMap.addMarker(new MarkerOptions().position(tempPoint)
                    .title(tempJSONObject.getString("time")));
            tempMarker.setSnippet("Number of respondings: ● ● ●");
            newMarkerTable.put(tempMarker, tempJSONObject.toString());
        }
        if(hasNew) mp.start();
        //delete expired markers
        for(Hashtable.Entry<Marker, String> entry : markerTable.entrySet()){
            entry.getKey().remove();
        }
        markerTable = newMarkerTable;*/
        boolean hasNew = false;
        Hashtable<Marker, String> newMarkerTable = new Hashtable<>();
        Hashtable<String, JSONObject> emergencySet = new Hashtable<>();
        //get all emergency ids
        for(int index = 0; index < emergencyAlerts.length(); index++){
            JSONObject tempJSONObject = emergencyAlerts.getJSONObject(index);
            emergencySet.put(tempJSONObject.getString("report"), tempJSONObject);
        }
        //add new markers
        for(int index = 0; index < badDriverReports.length(); index++){
            JSONObject tempJSONObject = badDriverReports.getJSONObject(index);
            String id = tempJSONObject.getString("_id");
            if(!emergencySet.containsKey(id)){
                continue;
            }
            JSONObject jsonObject = emergencySet.get(id);
            jsonObject.put("videoClip", tempJSONObject.getString("videoClip"));
            JSONArray coordinates = jsonObject.getJSONObject("location").
                    getJSONArray("coordinates");
            if(coordinates.getString(0).equals("null") ||
                    coordinates.getString(1).equals("null")){
                continue;
            }
            if(!markerTable.containsValue(jsonObject.toString())){
                hasNew = true;
            }
            LatLng tempPoint = new LatLng(Double.parseDouble(coordinates.getString(1)),
                    Double.parseDouble(coordinates.getString(0)));
            Marker tempMarker = mMap.addMarker(new MarkerOptions().draggable(true).position(tempPoint));
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = inputFormat.parse(jsonObject.getString("reportedAt"));
            String formattedTime = outputFormat.format(date);
            tempMarker.setTitle(formattedTime);
            if(jsonObject.has("reject") && jsonObject.getInt("reject") == 1){
                tempMarker.setSnippet("Case is rejected");
            }
            else if(jsonObject.has("resolved") && jsonObject.getInt("resolved") == 1){
                tempMarker.setSnippet("Case is resolved");
            }
            else if(jsonObject.has("respond") && jsonObject.getInt("respond") >= 1){
                StringBuilder sb = new StringBuilder();
                for(int i = 1; i <= jsonObject.getInt("respond"); i++){
                    sb.append("● ");
                }
                tempMarker.setSnippet("Number of responding: " + sb.toString());
            }
            else{
                tempMarker.setSnippet("Needs action!");
            }

            newMarkerTable.put(tempMarker, jsonObject.toString());
        }

        if(hasNew) mp.start();
        //delete expired markers
        for(Hashtable.Entry<Marker, String> entry : markerTable.entrySet()){
            entry.getKey().remove();
        }
        markerTable = newMarkerTable;
    }

    private void sendBadDriverReportRequest(){
        //set both
        //send out the post request to back-end API for all emergency alert reports
        final StringRequest badDriverReportRequest = new StringRequest(Request.Method.POST,
                url + urlReadAll,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            badDriverReports = new JSONArray(response);
                            if(badDriverReports != null && emergencyAlerts != null)
                                drawMarkers();
                        }
                        catch(Exception e){

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
                params.put("collection", collectionBad);
                /*JSONObject tempJSON = new JSONObject();
                JSONObject locationJSON = new JSONObject();
                try {
                    tempJSON.put("radius", radius);
                    locationJSON.put("lon", location.getLongitude());
                    locationJSON.put("lat", location.getLatitude());
                    tempJSON.put("location", locationJSON);
                } catch(JSONException e){

                }
                params.put("data", tempJSON.toString());*/
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        requestQueue.add(badDriverReportRequest);
    }

    private void sendEmergencyAlertsRequest(){
        //set both
        //send out the post request to back-end API for all emergency alert reports
        final StringRequest emergencyAlertsReportRequest = new StringRequest(Request.Method.POST,
                url + urlReadByRaidus,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            emergencyAlerts = new JSONArray(response);
                            if(badDriverReports != null && emergencyAlerts != null)
                                drawMarkers();
                        }
                        catch(Exception e){

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
                params.put("collection", collectionEmergency);
                JSONObject tempJSON = new JSONObject();
                JSONObject locationJSON = new JSONObject();
                try {
                    tempJSON.put("radius", radius);
                    locationJSON.put("lon", location.getLongitude());
                    locationJSON.put("lat", location.getLatitude());
                    tempJSON.put("location", locationJSON);
                } catch(JSONException e){

                }
                params.put("data", tempJSON.toString());
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

    public void handleSettingClick(View view) throws JSONException {
        //create bundle to put necessary information to set up the setting page
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("radius", radius);
        jsonObject.put("elapsedTime", elapsedTime);
        String data = jsonObject.toString();
        Bundle bundle = new Bundle();
        bundle.putString(settingKey, data);
        goToSetting.putExtras(bundle);
        //noinspection RestrictedApi
        startActivityForResult(goToSetting, settingCode);
    }

    //handle updates from setting page
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (settingCode) : {
                if (resultCode == Activity.RESULT_OK) {
                    String returnValue = data.getStringExtra(settingKey);
                    try {
                        JSONObject returnJSON = new JSONObject(returnValue);
                        updateSettings(returnJSON.getInt("elapsedTime"),
                                returnJSON.getInt("radius"));
                    } catch (JSONException e){
                        System.out.print(e.getMessage());
                    }
                }
                break;
            }
            case(videoCode):{
                if (resultCode == Activity.RESULT_OK) {
                    String returnValue = data.getStringExtra(key);
                    try {
                        JSONObject returnJSON = new JSONObject(returnValue);
                        JSONObject value = new JSONObject();
                        value.put("action", returnJSON.getString("action"));
                        value.put("timeStamp", returnJSON.getString("reportedAt"));
                        actions.put(returnJSON.getString("id"), value);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("action", actions.toString());
                        editor.commit();
                    } catch (JSONException e){
                        System.out.print(e.getMessage());
                    }
                }
                else if(resultCode == Activity.RESULT_CANCELED){
                    //TODO currently we do nothing when user did not react to the case
                }
                    sendBadDriverReportRequest();
                    sendEmergencyAlertsRequest();

            }
        }
    }

    //helper function, update the frequency and radius, redraw circle
    private void updateSettings(int updatedFreq, int updatedRadius){
        circle.setRadius(updatedRadius * mile2M);
        //update the shared preference
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("elapsedTime", updatedFreq);
        editor.putInt("radius", updatedRadius);
        editor.commit();
        elapsedTime = updatedFreq;
        radius = updatedRadius;

        sendBadDriverReportRequest();
        sendEmergencyAlertsRequest();

    }
    
}
