package mfulton.drivedata;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.Calendar;


public class Capture extends Activity {

    private Intent intent;
    private boolean accel, location, capturing;
    private String logName;

    private PowerManager.WakeLock wakelock;
    private File directory, logFile, imageDir;

    Camera cam;
    Preview preview;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = new File(imageDir.getPath(), "image");
            if (pictureFile == null){
                Log.d("Capture", "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("Capture", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("Capture", "Error accessing file: " + e.getMessage());
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Recieve intent and grab the extras
        intent = getIntent();
        accel = intent.getBooleanExtra("accel", false);
        location = intent.getBooleanExtra("location", false);
        logName = intent.getStringExtra("logName");

        //Acquires a wake lock of this activity, released when the capture is compelte
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeTag");
        wakelock.acquire();
        if(wakelock.isHeld()){Log.i("Capture", "Aquired wakeLock @" + SystemClock.elapsedRealtime());}


        //File prep for writing.  Creates the follow directory structure either on internal storage
        //or in the documents directory.
        // logName (directory)
        //  |--> images (directory)
        //  |--> logName.log (text file)
        // See filePrep function for further work.

        directory = filePrep(null, logName, true);
        imageDir = filePrep(directory, "images", true);
        logFile = filePrep(directory, (logName + ".log"), false);

        setContentView(R.layout.activity_capture);
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i("Capture onResume", "Gettting Camera and Preview");
        cam = getCamera();
        preview = new Preview(this, cam);
        FrameLayout frame = (FrameLayout) findViewById(R.id.camPreview);
        frame.addView(preview);
    }

    @Override
    public void onDestroy(){
        cam.release();
        Log.i("Capture", "is terminating nicely.");
        return;
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
                            cam.takePicture(null, null, mPicture);
                            Log.i("Hanlder Worker Thread", "Taking Picture");

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

    private static Camera getCamera(){
        Camera c = null;
        try{

            c = Camera.open();
        }catch(Exception e){
            Log.e("getCamera", e.toString());
        }

        return c;
    }

    //Sets up a file with the appropriate path and creates it if ineescary.
    public File filePrep(File parent, String filename, boolean isDirectory){
        File result = null;
        String path;

        try {
            //Cheack to see if external storage is available.  If it is, use it.
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                //If no parent is provided, find the documents directory.
                if (parent == null) {
                    path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath();
                }
                //Use the provided parent.
                else {
                    path = parent.getPath();
                }
                //Create a new file.
                result = new File(path, filename);

            }
            //Use the internal storage.
            else {
                //If no parent is provided, get the app file directory.
                if (parent == null) {
                    Context context = getApplicationContext();
                    path = context.getFilesDir().getPath();
                }
                //Otherwise just use the parent
                else {
                    path = parent.getPath();
                }

                //Create a new file.
                result = new File(path, filename);
            }

            //Now we're going to check to see if directories and files with these paths exist, and
            //create them if nessecary.

            //If we need a directory.
            if (isDirectory) {
                if (!(result.mkdirs()))
                    Log.i("Capture filePrep", "Directory " + filename + " already existed at: " + path);
                else
                    Log.i("Capture filePrep", "Directory " + filename + " has been created at: " + path);

            }

            //If we need a .log file.
            else {
                if (!(result.createNewFile()))
                    Log.i("Capture filePrep", "File " + filename + " already existed at: " + path);
                else
                    Log.i("Capture filePrep", "File " + filename + " has been created at: " + path);

            }

        }catch(Exception e){
            //If anything happen, just dump the contents of the exception.
            Log.e("Capture filePrep", "encountered a problem");
            Log.e("Capture filePrep", e.toString());
        }

        //Return the File, tied to a directory of .log file at the correct path..
        return result;
    }

}
