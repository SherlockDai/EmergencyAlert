package com.example.derekdai.emergencyalert;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class VideoActivity extends AppCompatActivity {
    private final String key = "record";
    private final String url = "https://cloudserver.carma-cam.com/downloadFile/";
    private final String updateUrl = "https://cloudserver.carma-cam.com/update";

    //declare all action buttons
    private Button respondButton, rejectButton, resolveButton;

    private RequestQueue requestQueue;

    private Intent intentExtras;

    private Bundle extrasBundle;

    private JSONObject record;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        VideoView videoView = (VideoView)findViewById(R.id.EmergencyVideo);
        resolveButton = findViewById(R.id.resolveBtn);
        rejectButton = findViewById(R.id.rejectBtn);
        respondButton = findViewById(R.id.respondBtn);
        //initialize request queue for udpate request
        requestQueue = Volley.newRequestQueue(this);

        //add listener to the buttons
        resolveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v, resolveButton);
            }
        });
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v, rejectButton);
            }
        });
        respondButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v, respondButton);
            }
        });
        intentExtras = getIntent();
        extrasBundle = intentExtras.getExtras();
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
            if(record.has("action")){
                if(record.getJSONObject("action").getString("action").equals("respond")){
                    respondButton.setEnabled(false);
                }
                else if(record.getJSONObject("action").getString("action").equals("resolve")){
                    resolveButton.setEnabled(false);
                }
                else if(record.getJSONObject("action").getString("action").equals("reject")){
                    rejectButton.setEnabled(false);
                }
            }
            else{
                if(record.has("reject") && record.getInt("reject") == 1){
                    respondButton.setEnabled(false);
                    rejectButton.setEnabled(false);
                    resolveButton.setEnabled(false);
                }
                else if(record.has("resolve") && record.getInt("resolve") == 1){
                    respondButton.setEnabled(false);
                    rejectButton.setEnabled(false);
                    resolveButton.setEnabled(false);
                }
            }
        } catch(JSONException e){
            System.out.println(e.getMessage());
        }
    }

    public void onButtonClick(View view, final Button btn){
        resolveButton.setEnabled(true);
        rejectButton.setEnabled(true);
        respondButton.setEnabled(true);
        btn.setEnabled(false);
        new AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog)
                .setTitle("Confirm Action!")
                .setMessage("Do you really want to " + btn.getText() +" ?")
                .setIcon(android.R.drawable.ic_dialog_alert)

                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast toast = Toast.makeText(VideoActivity.this, "Action Saved!", Toast.LENGTH_SHORT);
                        View view = toast.getView();
                        view.setBackgroundResource(R.color.colorPrimaryDark);
                        TextView text = (TextView) view.findViewById(android.R.id.message);
                        text.setTextColor(Color.WHITE);
                        toast.show();
                        //send update message to the back end
                        sendUpdateRequest(btn);
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void sendUpdateRequest(final Button btn){
        //set both
        //send out the post request to back-end API for all emergency alert reports
        final StringRequest updateRequest = new StringRequest(Request.Method.POST,
                updateUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.print(response);
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
                params.put("collection", "emergencyalerts");
                String btnText = btn.getText().toString();
                try {
                    JSONObject newData = new JSONObject();
                    switch (btnText) {
                        case "Respond": {
                            //first find out if some one has responded already
                            if (!record.has("respond")) {
                                newData.put("respond", 1);
                            } else {
                                int num = Integer.parseInt(record.getString("respond")) + 1;
                                newData.put("respond", num);
                            }
                            newData.put("reject", 0);
                            newData.put("resolved", 0);
                            break;
                        }
                        case "Reject":{
                            newData.put("reject", 1);
                            newData.put("resolved", 0);
                            break;
                        }
                        case "Resolved":{
                            newData.put("resolved", 1);
                            newData.put("reject", 0);
                            break;
                        }
                    }

                    params.put("_id", record.getString("_id"));
                    params.put("newData", newData.toString());
                } catch(JSONException e){

                }

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        requestQueue.add(updateRequest);
    }

    @Override
    public void onBackPressed(){
        Intent resultIntent = new Intent();
        JSONObject resultObject = new JSONObject();
        try {
            if(!respondButton.isEnabled() && !resolveButton.isEnabled() && !rejectButton.isEnabled()){
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
            }
            if (!respondButton.isEnabled()) {
                resultObject.put("id", record.getString("_id"));
                resultObject.put("action", "respond");
                resultObject.put("reportedAt", record.getString("reportedAt"));
            }
            else if(!resolveButton.isEnabled()){
                resultObject.put("id", record.getString("_id"));
                resultObject.put("action", "resolve");
                resultObject.put("reportedAt", record.getString("reportedAt"));
            }
            else if(!rejectButton.isEnabled()){
                resultObject.put("id", record.getString("_id"));
                resultObject.put("action", "reject");
                resultObject.put("reportedAt", record.getString("reportedAt"));
            }
            else{
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
            }
        } catch(JSONException e){
            System.out.println(e.getMessage());
        }
        resultIntent.putExtra(key, resultObject.toString());
        setResult(Activity.RESULT_OK, resultIntent);

        super.onBackPressed();
        finish();
    }

}
