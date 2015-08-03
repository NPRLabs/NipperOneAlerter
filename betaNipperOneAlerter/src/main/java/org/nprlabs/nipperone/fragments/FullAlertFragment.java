package org.nprlabs.nipperone.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.nprlabs.nipperone.framework.NipperConstants;
import org.nprlabs.nipperone.main.AlertImpl;
import org.prss.nprlabs.nipperonealerter.R;

/**
 *
 * Created by kbrudos on 7/29/2015.
 */
public class FullAlertFragment extends Fragment{

    private String TAG = "Full Alert Fragment";
    public Button ok, fullAlert, alertHelp;
    public TextView category, certainty, severity, urgency, action, banner;
    private int alertId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        super.onCreateView(inflater, container, savedInstanceState);
        alertId = getArguments().getInt("AlertId");

        return inflater.inflate(R.layout.custom_dialog, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        ok = (Button)  getView().findViewById(R.id.btn_ok);
        fullAlert = (Button) getView().findViewById(R.id.btn_view_full_alert);
        alertHelp = (Button) getView().findViewById(R.id.btn_alert_help);

        category = (TextView) getView().findViewById(R.id.txt_category);
        certainty = (TextView) getView().findViewById(R.id.txt_certainty);
        severity = (TextView) getView().findViewById(R.id.txt_severity);
        urgency = (TextView) getView().findViewById(R.id.txt_urgency);
        action = (TextView) getView().findViewById(R.id.txt_action);
        banner = (TextView) getView().findViewById(R.id.txt_banner);
        displayMessage(alertId);
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "------ onPause ------");
    }

    public void displayMessage(int alertId){

        AlertImpl msg = NipperConstants.dbHandler.getMessage(alertId);
        category.setText(msg.getMsgCategory());
        certainty.setText(msg.getMsgCertainty());
        severity.setText(msg.getMsgSeverity());
        urgency.setText(msg.getMsgUrgency());
        action.setText(msg.getMsgAction());
        banner.setText(msg.getEventString());

        //TODO add functionality logic for the action response connector word
        //TODO add logic for the sentence connector.

    }
}
