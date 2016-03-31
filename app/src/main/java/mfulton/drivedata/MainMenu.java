package mfulton.drivedata;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.SimpleDateFormat;

public class MainMenu extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    /*
        OBJECT VARIABLES
            These values hold information required by the activity.
    */
    private GoogleApiClient googleApiClient; //The connection client for the Google API.
    private boolean criteriaPassed; //The status of testing of the Google Location API.

    private boolean resolvingError;//Is the Activity currently resolving an error?

    //Constants
    private static final int REQUEST_RESULT_ERROR = 1001; //Request code for errors.
    private static final int REQUEST_CHECK_SETTINGS = 0x1; //Check settings requet.
    private static final String DIALOG_ERROR = "dialog_error"; //Tag for the error dialog fragment.
    private static final String STATE_RESOLVING_ERROR = "resolving_error"; // Tag for the state resolving state.
    private static final String STATE_CRITERIA_MET = "criteria_met"; //Tag for the criteria met state.

    /*
       ACTIVITY LIFECYCLE FUNCTIONS
           These overriden functions are crucial to maintaining the proper lifecycle of the Activity.
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        //Connect Google API Client.
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        //Get all saved instance variables.
        criteriaPassed = savedInstanceState != null && savedInstanceState.getBoolean(STATE_CRITERIA_MET, false);
        resolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

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
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, resolvingError);
        outState.putBoolean(STATE_CRITERIA_MET, criteriaPassed);
    }
/*
    HANDLERS FOR UI INTERACTIONS
        The following functions handle button presses and other interaction.
 */

    // Handler for the button to start the capture.
    public void startCapture(View view) {
        String logString= "CAPTURE--";
        logString = logString + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());

        Context context = getApplicationContext();
        int toast_duration = Toast.LENGTH_LONG;

        if (!criteriaPassed){
            CharSequence text = "Google Location Services is still configuring, please wait.";
            Toast toast = Toast.makeText(context, text, toast_duration);
            toast.show();
            return;
        }

        //Start the capture activity.
        Log.i("MainMenu", "Log Name is " + logString);
        Intent intent = new Intent(this, CaptureActivity.class);
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString("log_name", logString);
        edit.commit();
        startActivity(intent);
    }

    public void captureManager(View view){
        Intent intent = new Intent(this, CaptureManager.class);
        startActivity(intent);
    }

    public void settingsStart(View  view){
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    //Test the location services to see if they are adequate.
    public void checkCriteria(){
        //Set up test location reuest.
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000);

        //Set up the SettingsRequest framework.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.
                checkLocationSettings(googleApiClient, builder.build());

        //Run the actual test.
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates states = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //All location settings are good!
                        criteriaPassed = true;
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //There are some issues.
                        try {
                            status.startResolutionForResult(
                                    MainMenu.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("DriveData", e.toString());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Location settings are wrong, but cannot be resolved.
                        criteriaPassed = false;
                        break;
                }
            }
        });
    }

/*
    CONNECTION HANDLERS FOR GOOGLE API
 */

    @Override
    public void onConnected(Bundle connectionHint) {
        checkCriteria();
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    //Deal with connection failure cases.
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
            if (resultCode == RESULT_OK){
                if(!criteriaPassed){
                    checkCriteria();
                }
                if (!googleApiClient.isConnecting() &&
                        !googleApiClient.isConnected()) {
                    googleApiClient.connect();
                }
            }
        }
    }
}