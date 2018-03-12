package com.example.derekdai.emergencyalert;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingActivity extends AppCompatActivity {

    private final String settingKey = "setting";
    private JSONObject jsonObject;
    private TextView radiusText;
    private TextView freqText;
    private SeekBar radiusBar;
    private SeekBar freqBar;
    private final int maxRadius = 20;//miles
    private final int maxUpdateFrequency = 30;//minutes
    private SeekBar.OnSeekBarChangeListener radiusSeekBarChangeListener;
    private SeekBar.OnSeekBarChangeListener freqSeekBarChangeListener;
    private Button saveButton;
    private Button cancelButton;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(!bundle.isEmpty() && bundle.containsKey(settingKey)){
            try {
                jsonObject = new JSONObject(bundle.getString(settingKey));
            } catch (JSONException e){
                jsonObject = null;
            }
        }
        //declare TextView and SeekBar
        radiusText = findViewById(R.id.radiusTextView);
        radiusBar = findViewById(R.id.radiusSeekBar);
        freqText = findViewById(R.id.frequencyTextView);
        freqBar  = findViewById(R.id.frequencySeekBar);
        try {
            radiusText.setText("Radius: " + jsonObject.getString("radius") + " miles");
            radiusBar.setMax(maxRadius);
            radiusBar.setProgress(jsonObject.getInt("radius"));
            freqText.setText("Update Frequency: " + jsonObject.getString("elapsedTime") + " minutes");
            freqBar.setMax(maxUpdateFrequency);
            freqBar.setProgress(jsonObject.getInt("elapsedTime"));
        } catch (JSONException e){
            System.out.print(e.getMessage());
        }
        //initialize the listeners for SeekBars
        radiusSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // updated continuously as the user slides the thumb
                radiusText.setText("Radius: " + progress + " miles");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // called when the user first touches the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // called after the user finishes moving the SeekBar
            }
        };
        freqSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // updated continuously as the user slides the thumb
                freqText.setText("Update Frequency: " + progress + " minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // called when the user first touches the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // called after the user finishes moving the SeekBar
            }
        };
        radiusBar.setOnSeekBarChangeListener(radiusSeekBarChangeListener);
        freqBar.setOnSeekBarChangeListener(freqSeekBarChangeListener);

        //initialize button varaibles
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    public void handleSaveClick(View view) throws JSONException{
        JSONObject resultJSON = new JSONObject();
        resultJSON.put("radius", String.valueOf(this.radiusBar.getProgress()));
        resultJSON.put("elapsedTime", String.valueOf(this.freqBar.getProgress()));
        Intent resultIntent = new Intent();
        resultIntent.putExtra(settingKey, resultJSON.toString());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    public void handleCancelClick(View view) throws JSONException{
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, resultIntent);
        finish();
    }


}
