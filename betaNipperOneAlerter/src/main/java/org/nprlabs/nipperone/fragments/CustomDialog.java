package org.nprlabs.nipperone.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.nprlabs.nipperone.framework.DatabaseHandler;
import org.nprlabs.nipperone.main.AlertImpl;
import org.prss.nprlabs.nipperonealerter.R;

/**
 * This is a custom dialog class. To be used to display a new message. Will stay until the user
 * clicks the ok button.
 *
 * Created by kbrudos on 7/1/2015.
 */
public class CustomDialog extends Dialog implements View.OnClickListener {

    public Activity myA;
    public Dialog d;
    public Button ok, fullAlert, alertHelp;
    public TextView category, certainty, severity, urgency, action, banner;

    private DatabaseHandler dbHandler;

    public CustomDialog(Activity a){
        super(a);
        this.myA = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog);

        dbHandler = DatabaseHandler.getInstance(getContext());

        ok = (Button) findViewById(R.id.btn_ok);
        fullAlert = (Button) findViewById(R.id.btn_view_full_alert);
        alertHelp = (Button) findViewById(R.id.btn_alert_help);

        category = (TextView) findViewById(R.id.txt_category);
        certainty = (TextView) findViewById(R.id.txt_certainty);
        severity = (TextView) findViewById(R.id.txt_severity);
        urgency = (TextView) findViewById(R.id.txt_urgency);
        action = (TextView) findViewById(R.id.txt_action);
        banner = (TextView) findViewById(R.id.txt_banner);

        displayMessage();

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        dismiss();
            }
        });
    }


    @Override
    public void onClick(View v){



    }

    private void displayMessage(){

        AlertImpl msg = dbHandler.getMessage(dbHandler.getMessageCount());
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
