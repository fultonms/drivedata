package mfulton.drivedata;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
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
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Integer;

/**
 * Created by michael on 7/20/15.
 */
public class Capture {

    private Context myContext;
    private Activity myActivity;

    private boolean capturing;
    private String logName;
    private File directory, locationFile, imageDir;
    private OutputStream outLocation;

    private Camera cam;
    private Preview preview;
    private boolean cameraSafe;

    public Capture(Activity activity, Context context, String name, FrameLayout frame){
        myActivity = activity;
        myContext = context;
        logName = name;

        directory = filePrep(null, logName, true);
        imageDir = filePrep(directory, "images", true);
        locationFile = filePrep(directory, (logName + "_LOCATION.log"), false);

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

        }catch(Exception e){
            //If anything happen, just dump the contents of the exception.
            Log.e("Capture filePrep", "encountered a problem");
            Log.e("Capture filePrep", e.toString());
        }

        //Return the File, tied to a directory of .log file at the correct path..
        return result;
    }

    private Camera.PictureCallback myPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            timestamp = SystemClock.elapsedRealtime();
            File pictureFile =  new File(imageDir.getPath()
                    + File.separator + logName + "_IMG_" + timestamp + ".jpg");
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