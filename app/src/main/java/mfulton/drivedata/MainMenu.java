package mfulton.drivedata;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

public class MainMenu extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
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


        Intent intent= new Intent(this, CaptureActivity.class);
        intent.putExtra("logName", logString);
        startActivity(intent);
    }


}
