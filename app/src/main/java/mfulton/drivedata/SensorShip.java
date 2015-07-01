package mfulton.drivedata;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SensorShip extends Service {
    boolean accel, location;

    @Override
    public void int onStartCommand(Intent intent, int flags,int startId ){
        accel = intent.getBooleanExtra("accel", false);
        location = intent.getBooleanExtra("location",false);

        return START_STICKY

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
