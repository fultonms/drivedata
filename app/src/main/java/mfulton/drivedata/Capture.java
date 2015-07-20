package mfulton.drivedata;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by michael on 7/20/15.
 */
public class Capture {


    private Context myContext;
    private String logName;
    private File directory, logFile, imageDir;

    private SensorManager mySensors;
    private Sensor myAccel;

    private Camera cam;
    private Preview preview;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = new File(imageDir.getPath(), "image");
            if (pictureFile == null){
                Log.d("CaptureActivity", "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                Bitmap image;
                BitmapFactory.Options options = new BitmapFactory.Options();

                Log.i("CaptureActivity pictureCallback", "Attempting byte-array to bitmap convert");
                image = BitmapFactory.decodeByteArray(data, 0, data.length,options );

                Log.i("CaptureActivity pictureCallback", "Attempting to compress image");
                image.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("CaptureActivity", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("CaptureActivity", "Error accessing file: " + e.getMessage());
            }

        }
    };

    public Capture(Context context, String name){
        myContext = context;
        logName = name;

        mySensors = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        myAccel = mySensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        directory = filePrep(null, logName, true);
        imageDir = filePrep(directory, "images", true);
        logFile = filePrep(directory, (logName + ".log"), false);


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


}
