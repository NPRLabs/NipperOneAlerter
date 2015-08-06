package org.nprlabs.nipperone.framework;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.nprlabs.nipperone.framework.NipperConstants;
import org.nprlabs.nipperone.main.AlertImpl;
import org.nprlabs.nipperone.main.NipperActivity;
import org.prss.nprlabs.nipperonealerter.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom adapter for displaying the alert archive.
 * Created by kbrudos on 7/30/2015.
 */
public class ListAdapter extends ArrayAdapter<AlertImpl> {

    private String TAG = "ListAdapter";
    private Activity activity;
    private List<AlertImpl> list;
    private Resources resources;

    public ListAdapter(Context context, ArrayList<AlertImpl> alerts){
        super(context, 0, alerts);

    }


    @Override
    public View getView(int position, View view, ViewGroup parent){

        if(view==null){
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            view = inflater.inflate(R.layout.list_item, parent, false);
        }

        TextView firstLine = (TextView)view.findViewById(R.id.firstLine);
        TextView secondLine = (TextView)view.findViewById(R.id.secondLine);

        AlertImpl alert = getItem(position);
        firstLine.setText(alert.getEventString());
        secondLine.setText(alert.getMsgCategory());

        return view;
    }

}
