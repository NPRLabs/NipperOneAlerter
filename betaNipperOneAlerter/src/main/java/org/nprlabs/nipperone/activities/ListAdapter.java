package org.nprlabs.nipperone.activities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.nprlabs.nipperone.framework.NipperConstants;
import org.nprlabs.nipperone.main.AlertImpl;
import org.nprlabs.nipperone.main.NipperActivity;
import org.prss.nprlabs.nipperonealerter.R;

import java.util.List;

/**
 * A custom adapter for displaying the alert archive.
 * Created by kbrudos on 7/30/2015.
 */
public class ListAdapter extends BaseAdapter implements View.OnClickListener{

    private String TAG = "ListAdapter";
    private Activity activity;
    private List<AlertImpl> list;
    private Resources resources;

    private List<AlertImpl> mData = NipperConstants.dbHandler.getAllMessages();

    public ListAdapter(Activity a, List<AlertImpl> l, Resources r ){
        this.activity = a;
        this.list = l;
        this.resources = r;
    }

    @Override
    public int getCount(){

        return mData.size();
    }

    @Override
    public long getItemId(int position){
        return position;
    }


    @Override
    public Object getItem(int position){
        return mData.get(position);
    }


    @Override
    public View getView(int position, View arg1, ViewGroup parent){

        if(arg1==null){
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            arg1 = inflater.inflate(R.layout.list_item, parent, false);
        }

        TextView firstLine = (TextView)arg1.findViewById(R.id.firstLine);
        TextView secondLine = (TextView)arg1.findViewById(R.id.secondLine);

        AlertImpl alert = mData.get(position);
        firstLine.setText(alert.getEventString());
        secondLine.setText(alert.getMsgAction());

        return arg1;
    }
    public void updateListAdapter(){
        mData = NipperConstants.dbHandler.getAllMessagesReverse();
    }


    @Override
    public void onClick(View view){
        Log.d(TAG, "====== List item clicked ======");
    }

    private class OnItemClickListener implements View.OnClickListener{
        int mPosition;

        OnItemClickListener(int position){
            mPosition = position;
        }

        @Override
        public void onClick(View view){

            NipperActivity nA = (NipperActivity) activity;
            nA.onItemClick(mPosition);
        }
    }

}
