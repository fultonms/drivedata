package mfulton.drivedata;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Log;
import android.util.StateSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by michael on 7/20/15.
 */
public class Capture {

    private Context myContext;
    private Activity myActivity;

    private boolean capturing;
    private String logName;
    private File directory, accelFile, locationFile, imageDir;
    private OutputStream outAccel, outLocation;

    private SensorManager mySensors;
    private Sensor myAccel;
    private LocationManager local;
    private Sensor myOrient;

    private float accelValues[];
    private float gravity[];
    private double locationValues[];
    private float orientationValues[];
    private long timestamp;

    private Camera cam;
    private Preview preview;
    private boolean cameraSafe;

    public Capture(Activity activity, Context context, String name, FrameLayout frame){
        myActivity = activity;
        myContext = context;
        logName = name;

        mySensors = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        myAccel = mySensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        myOrient = mySensors.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mySensors.registerListener(mySensorlistener, myAccel, SensorManager.SENSOR_DELAY_FASTEST);
        mySensors.registerListener(OrientListener, myOrient, SensorManager.SENSOR_DELAY_FASTEST);

        local = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        local.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);

        directory = filePrep(null, logName, true);
        imageDir = filePrep(directory, "images", true);
        accelFile = filePrep(directory, (logName + "_ACCEL.log"), false);
        locationFile = filePrep(directory, (logName + "_LOCATION.log"), false);

        accelValues = new float[3];
        gravity = new float[3];
        locationValues = new double[3];
        orientationValues = new float[3];

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
            outAccel = new FileOutputStream(accelFile);
            outLocation = new FileOutputStream(locationFile);
        }catch (Exception e){
            Log.e("Capture", e.toString());
        }
    }

    public void begin(){
        capturing = true;
        cameraSafe = true;
        Log.i("Capture", "Starting the capture.");

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                        //CaptureActivity image
                            if(cameraSafe) {
                                cam.takePicture(null, null, myPicture);
                                cameraSafe = false;
                                cam.startPreview();

                                //CaptureActivity acceleration
                                String message;
                                message = Long.toString(timestamp) + " , " + Float.toString(accelValues[0]) + " , " + Float.toString(accelValues[1]) + " , "
                                        + Float.toString(accelValues[2]) + "\n";
                                outAccel.write(message.getBytes());

                                //CaptureActivity location
                                message = Long.toString(timestamp) + " , " + Double.toString(locationValues[0]) + " , " + Double.toString(locationValues[1]) + " , "
                                        + Double.toString(locationValues[2]) + " , " + Float.toString(orientationValues[0]) +
                                        "\n";
                                outLocation.write(message.getBytes());


                                message="( " + Float.toString(accelValues[0]) + ", " + Float.toString(accelValues[1]) + ", "
                                        + Float.toString(accelValues[2]) + " )";
                                ((TextView) myActivity.findViewById(R.id.accel_indicator)).setText(message);

                                StatFs stats = new StatFs(directory.getPath());
                                long bytesPossible = (long) stats.getBlockSize() * (long) stats.getBlockCount();
                                long bytesAvailable = (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
                                long megsAvailable = bytesAvailable/ 1048576 ;
                                long megsPossible = bytesPossible/ 1048576;

                                message = Long.toString(megsAvailable) + " / " + Long.toString(megsPossible) + " Free";

                                ((TextView) myActivity.findViewById(R.id.space_remaining)).setText(message);

                            }

                        } catch (Exception e) {
                                Log.e("Capture Loop", e.toString());
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
            outAccel.close();
            outLocation.close();
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
                Log.e("Capture", "Using External Storage");
                //If no parent is provided, find the documents directory.
                if (parent == null) {
                    //path = Environment.getExternalStorageDirectory().getPath();
                    path = System.getenv("SECONDARY_STORAGE");
                    path = path + "/DriveDataCaptures";
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
                Log.e("Capture", "Using Internal Storage");
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

    private SensorEventListener mySensorlistener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                accelValues[0] = event.values[0];
                accelValues[1] = event.values[1];
                accelValues[2] = event.values[2];
            }

            else {
                final float alpha = (float) 0.9;
                gravity[0] = alpha * gravity[0] + (1-alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1-alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1-alpha) * event.values[2];

                accelValues[0] = event.values[0] - gravity[0];
                accelValues[1] = event.values[1] - gravity[1];
                accelValues[2] = event.values[2] - gravity[2];
            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    private SensorEventListener OrientListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            orientationValues[0] = event.values[0];
            orientationValues[1] = event.values[1];
            orientationValues[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private LocationListener myLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            locationValues[0] = location.getLatitude();
            locationValues[1] = location.getLongitude();
            locationValues[2] = location.getAltitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e("Capture", "LocationProvider is available");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e("Capture", "LocationProvider is NOT available");
        }
    };

    private Camera.PictureCallback myPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            timestamp = SystemClock.elapsedRealtime();
            File pictureFile =  new File(imageDir.getPath() +File.separator + logName + "_IMG_" + timestamp + ".jpg");
            if (pictureFile == null){
                cameraSafe = true;
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
            } catch (Exception e){
                Log.e("Capture", e.toString());
            }

            cameraSafe= true;

        }
    };

}
