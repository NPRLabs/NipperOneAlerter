package org.nprlabs.nipperone.activities;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.nprlabs.nipperone.framework.NipperConstants;
import org.nprlabs.nipperone.main.AlertImpl;

import java.util.List;

/**
 *
 * Created by kbrudos on 7/29/2015.
 */
public class MyListView extends ListFragment {

    //This is the adapter being used to display the list's data
    SimpleCursorAdapter mAdapter;

    List<AlertImpl> messageList = NipperConstants.dbHandler.getAllMessages();
    String[] values = new String[]{ "Alert 1", "Alert 2", "Alert 3"};


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_expandable_list_item_1, values);
        setListAdapter(myAdapter);

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id){
        //Going to open the alert once clicked
        String item = (String) getListAdapter().getItem(position);
        Toast.makeText(getActivity().getApplication(), item + "selected", Toast.LENGTH_LONG).show();
    }

}
