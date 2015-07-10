package mfulton.drivedata;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;


public class MainMenu extends Activity {
    boolean accel, location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        accel= true;
        location = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroy(){
        Log.i("MainMenu", "is terminating nicely.");
        return;
    }


    public void startCapture(View view){
        Intent intent= new Intent(this, Capture.class);
        intent.putExtra("accel", accel);
        intent.putExtra("location", location);
        startActivity(intent);
    }

    public void accelButtonCheck(View view){
        accel = ((ToggleButton) view).isChecked();
    }

    public void locationButtonCheck(View view){
        location = ((ToggleButton) view).isChecked();
    }

}
