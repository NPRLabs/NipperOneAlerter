package org.nprlabs.nipperone.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.prss.nprlabs.nipperonealerter.R;

public class HelpFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
            Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
     // Inflate the layout for this fragment
        return inflater.inflate(R.layout.help_fragment_activity, container, false);
        
        
        // Load the preferences from an XML resource
        //addPreferencesFromResource(R.xml.preferences);
    }
    
    @Override
    public void onPause(){
        super.onPause();
        Log.d("HelpFragment","Pausing");
    }
    
}
