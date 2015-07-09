package mfulton.drivedata;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
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


public class Capture extends ActionBarActivity {
    private boolean accel, location, capturing;
    Intent intent;
    static final String ACTION_CAPTURE = "com.mfulton.drivedata.action.CAPTURE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        capturing = false;
        super.onCreate(savedInstanceState);
        intent = getIntent();
        accel = intent.getBooleanExtra("accel", false);
        location = intent.getBooleanExtra("location", false);

        setContentView(R.layout.activity_capture);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroy(){}


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
            Log.i("Capture", "Stopping Capture");;
            capturing=false;
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
                            Log.i("Handler", "Work Step: "+ SystemClock.elapsedRealtime());
                            if(capturing){handler.postDelayed(this, 100);}
                        }
                    }, 100);

            mine.setText("Stop Capture");
            //Start Camera Capture.
        }
    }
}
