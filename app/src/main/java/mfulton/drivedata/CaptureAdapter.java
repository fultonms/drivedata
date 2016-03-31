package mfulton.drivedata;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

/**
 * Created by michael on 3/31/2016.
 */
public class CaptureAdapter extends BaseAdapter {
    private final Context context;
    private final File[] values;

    public CaptureAdapter(Context context,File[] values ){
        super();
        this.context = context;
        this.values = values;
    }

    public int getCount(){
        return values.length;
    }

    public View getView(int position, View convertView, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.view_capture, parent, false);

        TextView name = (TextView) rowView.findViewById(R.id.captureName);
        TextView description = (TextView) rowView.findViewById(R.id.captureDescription);

        name.setText(values[position].getName());
        description.setText(Long.toString(values[position].length()));

        return rowView;
    }

    public long getItemId(int position){
        return position;
    }

    public Object getItem(int position){
        return values[position];
    }
}
