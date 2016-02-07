 package mfulton.drivedata;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import java.io.File;
import java.io.OutputStream;


 public class CaptureActivity extends FragmentActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
     /*
        OBJECT VARIABLES
            These variables hold information required by the capture activity.
      */

     private GoogleApiClient googleApiClient; //The connection client for the Google API.

     //Variables related to the log.
     private String logName; //The name of the current capture.
     private long timestamp; //The current time.
     private boolean isCapturing; //Whether or not the capture is currently taking place.

     //Variables related to file i/o
     private File directory, locationFile, imageDir; // The directory, log file for location, and the directory for images.
     private OutputStream outLocation; //

     //Variables related to camera use.
     private Camera cam; //The camera object.
     private Preview preview; // The camera preview used to display the current view.
     private boolean cameraSafe; //A boolean representing whether or not the camera is safe to use.

     private boolean resolvingError; //Is the application currently resolving an error.

     //Constants
     private static final int REQUEST_RESULT_ERROR = 1001; //Request code for errors.
     private static final int REQUEST_CHECK_SETTINGS = 0x1; //Check settings requet.
     private static final String DIALOG_ERROR = "dialog_error"; //Tag for the error dialog fragment.
     private static final String STATE_RESOLVING_ERROR = "resolving_error"; // Tag for the state resolving state.
     private static final String STATE_CRITERIA_MET = "criteria_met"; //Tag for the criteria met state.

     @Override
    protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_capture);

         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
         logName = prefs.getString("log_name", "");

         if (googleApiClient == null) {
             googleApiClient = new GoogleApiClient.Builder(this)
                     .addApi(LocationServices.API)
                     .addConnectionCallbacks(this)
                     .addOnConnectionFailedListener(this)
                     .build();
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
    @Override
    public void onBackPressed(){

    }

    public void onClick(View view) {
        TextView recorder = (TextView)findViewById(R.id.recording_indicator);

        if (isCapturing && (recorder.getText().equals("REC"))) {
            recorder.setText("Capture Complete");
            //myCapture.end();

            Log.i("CaptureActivity", "Stopping CaptureActivity");

        }else if(!isCapturing) {
            recorder.setText("REC");
            //myCapture.begin();
            Log.i("CaptureActivity", "Starting CaptureActivity");
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
     }

     @Override
     public void onConnectionSuspended(int cause) {
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
         public Dialog onCreateDialog(Bundle savedInstanceState) {
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