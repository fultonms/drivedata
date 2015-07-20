package mfulton.drivedata;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.FrameLayout;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by michael on 7/20/15.
 */
public class Capture {

    private boolean capturing;
    private Context myContext;
    private String logName;
    private File directory, logFile, imageDir;
    private OutputStream outLog;

    private SensorManager mySensors;
    private Sensor myAccel;

    private float values[];
    private float gravity[];

    private Camera cam;
    private Preview preview;

    public Capture(Context context, String name, FrameLayout frame){
        myContext = context;
        logName = name;

        mySensors = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        myAccel = mySensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mySensors.registerListener(listener, myAccel, SensorManager.SENSOR_DELAY_NORMAL);

        directory = filePrep(null, logName, true);
        imageDir = filePrep(directory, "images", true);
        logFile = filePrep(directory, (logName + ".log"), false);

        values = new float[3];
        gravity = new float[3];

        try{
            cam = Camera.open();
            preview = new Preview(myContext, cam);
            FrameLayout camView = frame;
            camView.addView(preview);
            //cam.startPreview();

        }catch(Exception e){
            Log.e("Capture", e.toString());
        }

        try {
            outLog = new FileOutputStream(logFile);
        }catch (Exception e){
            Log.e("Capture", e.toString());
        }
    }

    public void begin(){
        capturing = true;
        Log.i("Capture", "Starting the capture.");

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                        //CaptureActivity image
                            cam.takePicture(null, null, myPicture);
                            cam.startPreview();
                            cam.startPreview();

                            //CaptureActivity acceleration
                            String message;
                            message = Float.toString(values[0]) + " , " + Float.toString(values[1]) + " , "
                                    + Float.toString(values[2]) + " ; " + "\n";
                            outLog.write(message.getBytes());

                            //CaptureActivity location

                            //Write File

                            } catch (Exception e) {
                                Log.e("Capture", e.toString());
                            }

                        if (capturing) {
                            handler.postDelayed(this, 100);
                        }
                    }
                }, 100);

        //Start Camera CaptureActivity.
    }

    public void end(){
        capturing = false;
        cam.release();
        try {
            outLog.close();
        }catch (Exception e){
            Log.e("Capture", e.toString());
        }

    }

    public boolean isCapturing(){
        return capturing;
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
                    Context context = myContext;
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

    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                values[0] = event.values[0];
                values[1] = event.values[1];
                values[2] = event.values[2];
            }

            else {
                final float alpha = (float) 0.9;
                gravity[0] = alpha * gravity[0] + (1-alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1-alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1-alpha) * event.values[2];

                values[0] = event.values[0] - gravity[0];
                values[1] = event.values[1] - gravity[1];
                values[2] = event.values[2] - gravity[2];
            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private Camera.PictureCallback myPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile =  new File(imageDir.getPath() +File.separator + "IMG_" + SystemClock.elapsedRealtime() + ".jpg");
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

}
