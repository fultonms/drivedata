package mfulton.drivedata;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.IOException;


public class Capture extends Activity {
    private boolean accel, location, capturing;
    private Intent intent;
    private static final String ACTION_CAPTURE = "com.mfulton.drivedata.action.CAPTURE";
    private PowerManager.WakeLock wakelock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        capturing = false;
        super.onCreate(savedInstanceState);
        intent = getIntent();
        accel = intent.getBooleanExtra("accel", false);
        location = intent.getBooleanExtra("location", false);

        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeTag");
        wakelock.acquire();
        if(wakelock.isHeld()){Log.i("Capture", "Aquired wakeLock @" + SystemClock.elapsedRealtime());}

        setContentView(R.layout.activity_capture);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroy(){
        Log.i("Capture", "is terminating nicely.");
        return;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_capture, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onButton(View view){
        Button mine = (Button) view.findViewById(R.id.stopCapture);

        if(capturing){
            capturing=false;
            wakelock.release();

            Log.i("Capture", "Stopping Capture");
            if(!wakelock.isHeld()){Log.i("Capture", "Released wakeLock @" + SystemClock.elapsedRealtime());}

            mine.setText("Go Back");
        }
        else if(!capturing && mine.getText().equals("Go Back")){
            Intent Sintent = new Intent(this, MainMenu.class);
            Sintent.putExtra("accel", accel);
            Sintent.putExtra("location", location);
            startActivity(Sintent);
        }
        else{
            capturing = true;
            Log.i("CaptureActivity", "Starting Capture.");

            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            //Capture image

                            //Capture acceleration

                            //Capture location

                            //Write File

                            if(capturing){handler.postDelayed(this, 100);}
                        }
                    }, 100);

            mine.setText("Stop Capture");
            //Start Camera Capture.
        }
    }
}
