package mfulton.drivedata;

import android.animation.FloatEvaluator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CaptureManager extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_manager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ListView listView = (ListView) findViewById(R.id.list);
        final ArrayList<File> list = new ArrayList<File>();
        String path;
        File captureDir;
        File [] captures;

        path = Environment.getExternalStorageDirectory().getPath();
        path = path + "/DriveDataCaptures";
        captureDir = new File(path);
        captures = captureDir.listFiles();


        for (int i=0; i< captures.length; i++){
            list.add(captures[i]);
        }

        path = getApplicationContext().getFilesDir().getPath();
        captureDir = new File(path);
        captures = captureDir.listFiles();

        for (int i=0; i< captures.length; i++){
            list.add(captures[i]);
        }

        captures = list.toArray(captures);
        final CaptureAdapter adaptor = new CaptureAdapter(this, captures);
        listView.setAdapter(adaptor);

    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

}