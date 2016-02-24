package mfulton.drivedata;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;

public class CaptureActivity extends FragmentActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
     /*
        OBJECT VARIABLES
            These variables hold information required by the capture activity.
      */

    //
    private static String appVersion;

    private GoogleApiClient googleApiClient; //The connection client for the Google API.
    private boolean clientReady; // Is the client ready for capture?
    private Handler handler; //The Handler which handles the message que.

    //Variables related to the log.
    private String logName; //The name of the current capture.
    private long timestamp; //The current time.
    private boolean isCapturing; //Whether or not the capture is currently taking place

    //Variables related to file i/o
    private File directory, locationFile, accelFile, imageDir; // The directory, log file for location, and the directory for images.
    private OutputStream outLocation, outAccel; //

    //Variables for sensor output and sensor managers.
    private Location newestLocation;
    private boolean locationReady; // Whether or not the location API is ready.

    //Variables for handling sensor management.
    private SensorManager sManager;
    private Sensor lAccel;
    private SensorEvent newestAccel;
    private boolean accelReady; // Whether or not the acceleration sensors are ready.


    //Variables related to camera use.
    private Camera cam; //The camera object.
    private Preview preview; // The camera preview used to display the current view.
    private boolean cameraSafe; //A boolean representing whether or not the camera is safe to use.

    private boolean resolvingError; //Is the application currently resolving an error.

    //Constants
    private static final int REQUEST_RESULT_ERROR = 1001; //Request code for errors.
    private static final String DIALOG_ERROR = "dialog_error"; //Tag for the error dialog fragment.
    private static final String STATE_RESOLVING_ERROR = "resolving_error"; // Tag for the state resolving state.
    private static final String STATE_CRITERIA_MET = "criteria_met"; //Tag for the criteria met state.
/*
    FUNCTIONS  */

     @Override
    protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_capture);

         isCapturing = clientReady = locationReady = accelReady = false;

         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
         logName = prefs.getString("log_name", "");

         handler = new Handler(Looper.getMainLooper());

         if (googleApiClient == null) {
             googleApiClient = new GoogleApiClient.Builder(this)
                     .addApi(LocationServices.API)
                     .addConnectionCallbacks(this)
                     .addOnConnectionFailedListener(this)
                     .build();
         }

         directory = filePrep(null, logName, true);
         imageDir = filePrep(directory, "images", true);
         locationFile = filePrep(directory, (logName + "_LOCATION.log"), false);
         accelFile = filePrep(directory, (logName + "_ACCEL.log"), false);

         try{
             DateFormat[] formats = new DateFormat[]{
                DateFormat.getDateInstance(),
                DateFormat.getDateTimeInstance(),
                DateFormat.getTimeInstance(),
             };

             outLocation = new FileOutputStream(locationFile);
             outLocation.write((formats[1].format(new Date(0))).getBytes());
             outLocation.write(("\nTimestamp || Latitude || Longitude || Bearing || Altitude || Acuracy || Real Time\n").getBytes());

             outAccel = new FileOutputStream(accelFile);
             outAccel.write((formats[1].format(new Date(0))).getBytes());
             outAccel.write(("\nTimestamp || X || Y || Z || Acuracy || Real Time\n").getBytes());
         }catch( Exception e){
             Log.e("Capture", e.toString());
         }

         sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
         if (sManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null){
             lAccel = sManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
             sManager.registerListener(accelListener, lAccel, SensorManager.SENSOR_DELAY_FASTEST);
             accelReady = true;
         }
         else{
             lAccel = null;
             accelReady = false;
         }
         lAccel = sManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

         try{
             cam = Camera.open();
             preview = new Preview(getApplicationContext(), cam);
             FrameLayout camView = (FrameLayout) findViewById(R.id.camPreview);
             camView.addView(preview);
             cam.startPreview();
         }catch(Exception e){
             Log.e("Capture", e.toString());
         }
     }

     @Override
     protected void onDestroy() {
         super.onDestroy();
     }

     @Override
     protected void onStart() {
         googleApiClient.connect();
         super.onStart();
     }

     @Override
     protected void onStop() {
         if (isCapturing){endCapture();}
         googleApiClient.disconnect();
         super.onStop();
     }

    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause(){
         super.onPause();
     }

     /*
    UI LISTENERS

*/
    public void onClick(View view) {
        TextView recorder = (TextView)findViewById(R.id.recording_indicator);

        if (isCapturing && (recorder.getText().equals("REC"))) {
            recorder.setText("Capture Complete");
            recorder.setTextColor(getResources().getColor(R.color.green));
            endCapture();

            Log.i("CaptureActivity", "Stopping CaptureActivity");

        }else if(!isCapturing) {
            if(clientReady && locationReady && accelReady) {
                recorder.setText("REC");
                beginCapture();
                Log.i("CaptureActivity", "Starting CaptureActivity");
            }
            else{
                Context context = getApplicationContext();
                int toast_duration = Toast.LENGTH_LONG;
                CharSequence text = "An unkown error is preventing capture beginning.";
                Toast toast;

                if (!clientReady)
                    text = "Google Play Services not ready, please wait a moment";
                else if (!locationReady)
                    text = "Google Location Services not ready, please wait a moment";
                else if (!accelReady)
                    text = "Acceleration sensors calibrating, please wait.";

                toast = Toast.makeText(context, text, toast_duration);
                toast.show();

            }
        }

        else{
            return;
        }
    }

     /*
    CONNECTION HANDLERS FOR GOOGLE API
 */

     @Override
     public void onConnected(Bundle connectionHint) {
         Log.i("CaptureActivity", "onConnected called");
         clientReady = true;

         //Use a new thread to request location updates for the worker thread.
         //This new thread waits until the result of the request for updates is complete.
         Runnable r = new Runnable(){
             @Override
             public void run() {
                 LocationRequest request = new LocationRequest();
                 request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5000);

                 LocationCallback locationCallback = new LocationCallback() {
                     @Override
                     public void onLocationAvailability(LocationAvailability availability) {
                         if (!availability.isLocationAvailable()) {
                             locationReady = false;
                         }
                     }

                     public void onLocationResult(LocationResult result) {
                         newestLocation = result.getLastLocation();
                     }
                 };

                 PendingResult<Status> requestResult = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, locationCallback, handler.getLooper());
                 Status requestStatus = requestResult.await();
                 locationReady = true;
                 if(requestStatus.isSuccess()) {
                     locationReady = true;
                     Log.i("CaptureActivity", "Location update request result success!");
                 }else {
                     Log.i("CaptureActivity", "Location update request result: failure!");
                 }
             }
         };
         new Thread(r).start();
     }

     @Override
     public void onConnectionSuspended(int cause) {
         isCapturing = false;
         endCapture();
     }

     @Override
     public void onConnectionFailed(@NonNull ConnectionResult result) {
         if (resolvingError) {
             return; //An error is being resolved.  We need to wait.
         } else if (result.hasResolution()) { //There is a resolution available.
             try {
                 resolvingError = true;
                 result.startResolutionForResult(this, REQUEST_RESULT_ERROR);
             } catch (IntentSender.SendIntentException e) {
                 //There was an error attempting to resolve.  Let's try again.
                 googleApiClient.connect();
             }
         } else {
             //Show error dialog
             resolvingError = true;
             showErrorDialog(result.getErrorCode());

         }
     }
 /*
     FUNCTIONS THAT ACTUALLY DO THE WORK
      */

     public void beginCapture(){
         isCapturing = true;
         cameraSafe = true;

         Log.i("Capture", "Starting the capture.");

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

                                 String logEntry;
                                 if(newestLocation != null) {
                                     //Writing Location updates
                                     logEntry = Long.toString(timestamp) + " , "
                                             + Double.toString(newestLocation.getLatitude()) + " , "
                                             + Double.toString(newestLocation.getLongitude()) + " , ";

                                     if (newestLocation.hasBearing())
                                         logEntry = logEntry + Double.toString(newestLocation.getBearing())
                                                 + " , ";
                                     else
                                         logEntry = logEntry + "NA" + " , ";

                                     if (newestLocation.hasAltitude())
                                         logEntry = logEntry + Double.toString(newestLocation.getAltitude())
                                                 + " , ";
                                     else
                                         logEntry = logEntry + "NA" + " , ";
                                     if (newestLocation.hasAccuracy())
                                         logEntry = logEntry + Double.toString(newestLocation.getAccuracy())
                                                 + " , ";
                                     else
                                         logEntry = logEntry + "NA" + " , ";

                                     logEntry = logEntry + Long.toString(newestLocation.getTime());
                                     logEntry = logEntry + "\n";
                                     outLocation.write(logEntry.getBytes());
                                 }
                                 else{
                                     outLocation.write(("LOCATION NULL").getBytes());
                                 }

                                 if(newestAccel != null) {
                                     //Writing Accel Updates
                                     logEntry = "";
                                     logEntry = Long.toString(timestamp) + " , "
                                             + Double.toString(newestAccel.values[0]) + " , "
                                             + Double.toString(newestAccel.values[1]) + " , "
                                             + Double.toString(newestAccel.values[2]) + " , "
                                             + Integer.toString(newestAccel.accuracy) + " , "
                                             + Long.toString(newestAccel.timestamp) + "\n";
                                     outAccel.write(logEntry.getBytes());
                                 }
                                 else{
                                     outAccel.write(("ACCELERATION NULL").getBytes());
                                 }
                             }
                         } catch (Exception e) {
                             Log.e("Capture Loop", e.toString());
                         }
                         if (isCapturing && locationReady) {
                             handler.postDelayed(this, 100);
                         }
                     }
                 }, 100);

         //Start Camera CaptureActivity.
     }

     public void endCapture(){
         isCapturing = false;

         cam.release();
         try {
             outLocation.close();
         }catch (Exception e){
             Log.e("Capture", e.toString());
         }

     }

     //Sets up a file with the appropriate path and creates it if ineescary.
 public File filePrep(File parent, String filename, boolean isDirectory){
     File result = null;
     String path;

     try {
         //Cheack to see if external storage is available.  If it is, use it.
         if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
             Log.i("Capture", "Using External Storage");
             //If no parent is provided, find the documents directory.
             if (parent == null) {
                 path = Environment.getExternalStorageDirectory().getPath();
//                 path = System.getenv("SECONDARY_STORAGE");
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
             Log.i("Capture", "Using Internal Storage");
             //If no parent is provided, get the app file directory.
             if (parent == null) {
                 path = getApplicationContext().getFilesDir().getPath();
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
                 Log.i("Capture filePrep", "Directory "
                         + filename + " already existed at: " + path);
             else
                 Log.i("Capture filePrep", "Directory "
                         + filename + " has been created at: " + path);
         }

         //If we need a .log file.
         else {
             if (!(result.createNewFile()))
                 Log.i("Capture filePrep", "File "
                         + filename + " already existed at: " + path);
             else
                 Log.i("Capture filePrep", "File "
                         + filename + " has been created at: " + path);

         }

         //Set file permissions
         if(!(result.setReadable(true, true) &&result.setWritable(true, true))) {
             throw new IOException(("Access denied to:" + result.getName()));
         }

         }catch(Exception e){
         //If anything happen, just dump the contents of the exception.
         Log.e("Capture filePrep", "encountered a problem");
         Log.e("Capture filePrep", e.toString());
     }

     //Return the File, tied to a directory of .log file at the correct path..
     return result;
 }

    //SensorEvent Listener for linear acceleration.
    private SensorEventListener accelListener = new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent event){
            newestAccel = event;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy){
            return;
        }

    };

    // Callback for capturing image.
     private Camera.PictureCallback myPicture = new Camera.PictureCallback() {

         @Override
         public void onPictureTaken(byte[] data, Camera camera) {

             timestamp = SystemClock.elapsedRealtime();
             File pictureFile =  new File(imageDir.getPath()
                     + File.separator + logName + "_IMG_" + timestamp + ".jpg");
             cameraSafe = true;
             try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
             } catch (FileNotFoundException e) {
                Log.d("Capture", "File not found: " + e.getMessage());
             } catch (IOException e) {
                Log.d("Capture", "Error accessing file: " + e.getMessage());
             } catch (Exception e) {
                Log.e("Capture", e.toString());
             }
             cameraSafe= true;

         }
     };

 /*
     ERROR HANDLING FUNCTIONS
 */

     //The following functions and inline classes are defined to help resolve connection issues.
     public void showErrorDialog(int errorCode) {
         //Create a new fragment.
         ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
         //Pass in the error that must be displayed.
         Bundle args = new Bundle();
         args.putInt(DIALOG_ERROR, errorCode);
         dialogFragment.setArguments(args);
         dialogFragment.show(getSupportFragmentManager(), "errordialog");
     }

     // Called once the ErrorDialogFragment is done.
     public void onDialogDismissed() {
         resolvingError = false;
     }

     //Class used to select the correct dialog
     public static class ErrorDialogFragment extends DialogFragment {
         public ErrorDialogFragment() {
         }

         @Override
         public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
             //Get the error code
             int errorCode = this.getArguments().getInt(DIALOG_ERROR);
             return GoogleApiAvailability.getInstance().getErrorDialog(
                     this.getActivity(), errorCode, REQUEST_RESULT_ERROR
             );
         }

         @Override
         public void onDismiss(DialogInterface dialog) {
             ((MainMenu) getActivity()).onDialogDismissed();
         }
     }

     //Once the error resolution completes, this callback is needed
     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         if (requestCode == REQUEST_RESULT_ERROR) {
             resolvingError = false;
             if (resultCode == RESULT_OK) {
                 if (!googleApiClient.isConnecting() &&
                         !googleApiClient.isConnected()) {
                     googleApiClient.connect();
                 }
             }
         }
     }

}