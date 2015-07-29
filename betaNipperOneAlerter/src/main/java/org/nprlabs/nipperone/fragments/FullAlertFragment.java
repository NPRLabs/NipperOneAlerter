package org.nprlabs.nipperone.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.prss.nprlabs.nipperonealerter.R;

/**
 *
 * Created by kbrudos on 7/29/2015.
 */
public class FullAlertFragment extends Fragment{

    private String TAG = "Full Alert Fragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.custom_dialog, container, false);
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "------ onPause ------");
    }
}
