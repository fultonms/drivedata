package mfulton.drivedata;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by michael on 7/12/15.
 */
public class Preview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    private Camera cam;

    public Preview(Context context, Camera camera){
        super(context);
        cam = camera;

        holder = getHolder();
        holder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder sHolder){
        try{
            cam.setPreviewDisplay(sHolder);
            cam.startPreview();
        }catch(Exception e){
            Log.e("Preview surfaceCreated", e.toString());
        }
    }

    public void surfaceDestroyed(SurfaceHolder sHolder){
        return;
    }

    public void surfaceChanged(SurfaceHolder sHolder, int format, int w, int h){

    }

}
