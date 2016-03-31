package mfulton.drivedata;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by michael on 3/31/2016.
 */
public class CaptureAdapter extends BaseAdapter {
    private final Context context;
    private final File[] values;

    public CaptureAdapter(Context context, File[] values) {
        super();
        this.context = context;
        this.values = values;
    }

    public int getCount() {
        return values.length;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.view_capture, parent, false);

        TextView name = (TextView) rowView.findViewById(R.id.captureName);
        TextView description = (TextView) rowView.findViewById(R.id.captureDescription);

        //Bitmap image = BitmapFactory.decodeFile(new File(values[position].getPath() + "/images/").listFiles()[0].getPath());
        //ImageView icon = (ImageView) rowView.findViewById(R.id.icon);
        //icon.setImageBitmap(image);

        name.setText(values[position].getName());
        long size = getDirectorySize(values[position]);

        if (size/(1000*1000) < 1) {
            description.setText(Long.toString(size/1000) + " KB");
        }
        else if(size/(1000*1000*1000) < 1) {
            description.setText(Long.toString(size / (1000 * 1000)) + " MB");
        }
        else
            description.setText(Long.toString(size /(1000 * 1000 * 1000)) + " GB");

        return rowView;
    }

    public long getItemId(int position) {
        return position;
    }

    public Object getItem(int position) {
        return values[position];
    }

    static long getDirectorySize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                // System.out.println(file.getName() + " " + file.length());
                size += file.length();
            } else
                size += getDirectorySize(file);
        }
        return size;
    }
}