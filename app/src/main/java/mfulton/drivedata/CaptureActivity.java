 package mfulton.drivedata;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CaptureActivity extends Activity {

    private Intent intent;
    private String logName;

    private Capture myCapture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Recieve intent and grab the extras
        intent = getIntent();
        logName = intent.getStringExtra("logName");

        setContentView(R.layout.activity_capture);

        myCapture = new Capture(this, getApplicationContext(), logName, ((FrameLayout)findViewById(R.id.camPreview)));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        Log.i("CaptureActivity", "is terminating nicely.");
        return;
    }

    @Override
    public void onBackPressed(){
        if(myCapture.isCapturing()){
            myCapture.end();
            Log.i("CaptureActivity", "Stopping CaptureActivity");
        }

        Intent intent = new Intent(this, MainMenu.class);
        startActivity(intent);

    }


    public void onClick(View view) {
        TextView recorder = (TextView)findViewById(R.id.recording_indicator);

        if (myCapture.isCapturing() && (recorder.getText().equals("REC"))) {
            recorder.setText("Capture Complete");
            myCapture.end();

            Log.i("CaptureActivity", "Stopping CaptureActivity");

        }else if(!myCapture.isCapturing()) {
            recorder.setText("REC");
            myCapture.begin();
            Log.i("CaptureActivity", "Starting CaptureActivity");
        }

        else{
            return;
        }
    }

}