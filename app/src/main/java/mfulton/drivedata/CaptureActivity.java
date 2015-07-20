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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CaptureActivity extends Activity {

    private Intent intent;
    private boolean accel, location, capturing;
    private String logName;

    private PowerManager.WakeLock wakelock;

    private Capture myCapture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Recieve intent and grab the extras
        intent = getIntent();
        accel = intent.getBooleanExtra("accel", false);
        location = intent.getBooleanExtra("location", false);
        logName = intent.getStringExtra("logName");

        myCapture = new Capture(getApplicationContext(), logName);

        //Acquires a wake lock of this activity, released when the capture is compelte
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeTag");
        wakelock.acquire();
        if (wakelock.isHeld()) {
            Log.i("CaptureActivity", "Aquired wakeLock @" + SystemClock.elapsedRealtime());
        }

        setContentView(R.layout.activity_capture);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("CaptureActivity onResume", "Gettting Camera and Preview");
    }

    @Override
    public void onDestroy() {
        Log.i("CaptureActivity", "is terminating nicely.");
        return;
    }


    public void onButton(View view) {
        Button mine = (Button) view.findViewById(R.id.stopCapture);

        if (capturing) {
            capturing = false;
            wakelock.release();

            Log.i("CaptureActivity", "Sto   pping CaptureActivity");
            if (!wakelock.isHeld()) {
                Log.i("CaptureActivity", "Released wakeLock @" + SystemClock.elapsedRealtime());
            }

            mine.setText("Go Back");
        } else if (!capturing && mine.getText().equals("Go Back")) {
            Intent Sintent = new Intent(this, MainMenu.class);
            Sintent.putExtra("accel", accel);
            Sintent.putExtra("location", location);
            startActivity(Sintent);
        } else {
            capturing = true;
            Log.i("CaptureActivity", "Starting CaptureActivity.");

            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            //CaptureActivity image
                            //cam.takePicture(null, null, mPicture);
                            Log.i("Hanlder Worker Thread", "Taking Picture");

                            //CaptureActivity acceleration

                            //CaptureActivity location

                            //Write File

                            if (capturing) {
                                handler.postDelayed(this, 100);
                            }
                        }
                    }, 100);

            mine.setText("Stop CaptureActivity");
            //Start Camera CaptureActivity.
        }
    }

    private static Camera getCamera() {
        Camera c = null;
        try {

            c = Camera.open();
        } catch (Exception e) {
            Log.e("getCamera", e.toString());
        }

        return c;
    }

}