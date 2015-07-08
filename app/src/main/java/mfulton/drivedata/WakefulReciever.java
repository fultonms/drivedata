package mfulton.drivedata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class WakefulReciever extends WakefulBroadcastReceiver {
    public WakefulReciever() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equalsIgnoreCase("com.mfulton.drivedata.action.CAPTURE"));
            Log.i("WakefulReciever", "Message recieved");

        Intent service = new Intent(context, CaptureService.class);
        Log.i("WakefulReciever", "Starting service @"+ SystemClock.elapsedRealtime());
        startWakefulService(context, service);
    }
}
