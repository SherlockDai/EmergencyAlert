package com.example.derekdai.emergencyalert;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoActivity extends AppCompatActivity {
    private final String key = "record";
    private final String url = "http://cloudserver.carma-cam.com:9001/downloadFile/";
    JSONObject record;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        VideoView videoView = (VideoView)findViewById(R.id.EmergencyVideo);
        Intent intentExtras = getIntent();
        Bundle extrasBundle = intentExtras.getExtras();
        if(!extrasBundle.isEmpty() && extrasBundle.containsKey(key)){
            try {
                record = new JSONObject(extrasBundle.getString(key));
            }catch(JSONException e){

            }
        }
        else{
            record = null;
        }
        try {
            String videoAddress = url + record.getString("videoClip");
            Uri uri = Uri.parse(videoAddress);
            videoView.setVideoURI(uri);
            videoView.setMediaController(new MediaController(this));
            videoView.start();
        } catch(JSONException e){

        }
    }


}
