package com.example.derekdai.emergencyalert;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private TextView phoneTextView;
    private TextView passwordTextView;
    private CardView loginButton;
    private TextView registerButton;

    private RequestQueue requestQueue;

    private Intent goToMain;

    private String authURL = "https://carma-cam-test-backend.yj83leetest.space/9010/getPoliceAccountByPhone";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        phoneTextView = findViewById(R.id.phoneText);
        passwordTextView = findViewById(R.id.passwordText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        requestQueue = Volley.newRequestQueue(this);

        goToMain = new Intent(LoginActivity.this, MainActivity.class);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginClick(v);
            }
        });

    }

    private void onLoginClick(View v){
        Drawable error_icon = ContextCompat.getDrawable(this, R.drawable.error_icon);
        //first check if phone number is input correctly
        if(phoneTextView.getText().length() == 0){
            phoneTextView.setError("Please input your phone number", error_icon);
            phoneTextView.requestFocus();
            return;
        }
        if(!validatePhoneNumber(phoneTextView.getText().toString())){
            phoneTextView.setError("Please input valid phone number", error_icon);
            phoneTextView.requestFocus();
            return;
        }
        if(passwordTextView.getText().length() == 0){
            passwordTextView.setError("Please input your password", error_icon);
            passwordTextView.requestFocus();
            return;
        }
        //all right, lets check if the user can get into our main activity or not
        sendAuthRequest(phoneTextView.getText().toString(), passwordTextView.getText().toString());

    }

    private boolean validatePhoneNumber(String phoneNumber){
        //matches 9999999999, 1-999-999-9999 and 999-999-9999
        Pattern p = Pattern.compile("^[0-9]*$" );
        // Pattern class contains matcher() method
        // to find matching between given number
        // and regular expression
        Matcher m = p.matcher(phoneNumber);
        return (m.find() && m.group().equals(phoneNumber));
    }

    private void sendAuthRequest(final String phone, final String password){
        //set both
        //send out the post request to back-end API for all emergency alert reports
        final StringRequest updateRequest = new StringRequest(Request.Method.POST,
                authURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject respJSON = new JSONObject(response);
                            if(respJSON.has("error")){
                                Toast.makeText(LoginActivity.this, respJSON.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            else{
                                String data = respJSON.toString();
                                Bundle bundle = new Bundle();
                                bundle.putString("user", data);
                                goToMain.putExtras(bundle);
                                startActivity(goToMain);
                            }
                        } catch(JSONException e){
                            e.printStackTrace();
                        }
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
                params.put("phone", phone);
                params.put("password", password);
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
}
