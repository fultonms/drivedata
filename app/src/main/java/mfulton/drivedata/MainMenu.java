package mfulton.drivedata;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

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
        View parent = view.getRootView();

        TextView logEntry = (TextView) parent.findViewById(R.id.logNameEntry);


        CharSequence logName = logEntry.getText();
        String logString = logName.toString();

        if(logString.isEmpty()){
            logEntry.setError("This field cannot be empty");
            return;
        }

        Log.i("MainMenu", "Log Name is " + logString);


        Intent intent= new Intent(this, Capture.class);
        intent.putExtra("accel", accel);
        intent.putExtra("location", location);
        intent.putExtra("logName", logString);
        startActivity(intent);
    }

    public void accelButtonCheck(View view){
        accel = ((Switch) view).isChecked();
    }

    public void locationButtonCheck(View view){
        location = ((Switch) view).isChecked();
    }

}
