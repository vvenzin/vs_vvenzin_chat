package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import ch.ethz.inf.vs.helperclasses.EnhancedActivity;

public class MainActivity extends EnhancedActivity implements View.OnClickListener, ChatServiceManagerListener {

    // Constants
    private final String LOGTAG = "## VV-MainActivity ##";


    private String mName;
    private int mPort;
    private String mIp;
    private ChatServiceManager mClientManager;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOGTAG, "onCreate()");

        Button b = (Button) findViewById(R.id.join_btn);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.serttings_btn);
        b.setOnClickListener(this);

        // Install handlet to save name in mName when user clicks done
        final EditText textField = (EditText) findViewById(R.id.name_text_field);
        textField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        textField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    // Clear focus
                    textField.clearFocus();
                    RelativeLayout parent = (RelativeLayout) findViewById(R.id.parent_edittext);
                    parent.requestFocus();

                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(textField.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                    mName = textField.getText().toString();
                    Log.d(LOGTAG, "User entered new name " + mName);

                    if (infoReady()) mClientManager.setIpPortName(mIp,mPort,mName); // Tell service about ip and port
                    else mClientManager.invalidateIpPortName();


                    // Store name in sharedPrefs
                    if (mName != null) {
                        SharedPreferences sharedPref = getSharedPreferences(
                                getString(R.string.preference_file_key), getApplicationContext().MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(getString(R.string.saved_name), mName);
                        editor.commit();
                    }
                    handled = true;
                }
                return handled;
            }
        });


        // Bind to service
        mClientManager = (ChatServiceManager) ServiceManagerSingleton.getInstance(getApplication(), ChatServiceManager.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClientManager.start();
        mClientManager.registerListener(this);
    }

    @Override
    protected void onEnterBackground() {
        super.onEnterBackground();

        // Stop service
        mClientManager.stop();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Check if should check for ip and port
        loadPrefs();
        if (infoReady()) mClientManager.setIpPortName(mIp,mPort,mName); // Tell service about ip and port
        else mClientManager.invalidateIpPortName();


        // Make buttons appear next to each other when in landscape
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.button_layout);
        // Checks the orientation of the screen
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            //  portrait
            linearLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration conf)
    {
        super.onConfigurationChanged(conf);

        // Make buttons appear next to each other when in landscape
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.button_layout);
        // Checks the orientation of the screen
        if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else if (conf.orientation == Configuration.ORIENTATION_PORTRAIT){
            //  portrait
            linearLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mClientManager.unregisterListener(this);
    }

    public void onClick(View b)
    {
        switch (b.getId()) {
            case R.id.join_btn:
                // Check if should check for ip and port
                if (!infoReady()) {
                    Log.d(LOGTAG, "Could not register. ip:" + mIp + " port: " + mPort + " name: " + mName);
                    errorMessage("Please enter your name, ip address and port.");
                } else  mClientManager.register(true,mIp,mPort,mName);
                break;
            case R.id.serttings_btn:
                Intent i1 = new Intent(this, SettingsActivity.class);
                this.startActivity(i1);
                break;
        }
    }

    /**
     *
     * App logic
     *
     */

    // Returns true if found false if none stored yed
    private void loadPrefs()
    {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), getApplicationContext().MODE_PRIVATE);
        String port = sharedPref.getString(getString(R.string.saved_port), "");
        mIp = sharedPref.getString(getString(R.string.saved_server), "");

        try {
            mPort = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            mPort = -1;
            Log.d(LOGTAG,"Error retrieving port");
        }
        Log.d(LOGTAG,"Fetched ip " + mIp + " and port " + port);

        // Load name
        mName = sharedPref.getString(getString(R.string.saved_name),"");
        EditText textField = (EditText) findViewById(R.id.name_text_field);
        textField.setText(mName);
    }

    private boolean infoReady()
    {
        return (mName != null && mIp != null && !mName.equals("") && !mIp.equals("") && mPort > 999 && mPort < 10000);
    }

    // Display error with msg as message
    private void errorMessage(String msg)
    {
        // Display error
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(msg);
        dlgAlert.setTitle("ERROR");
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
        dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
    }


    /**
     *
     * ClientManagerCallbacks
     *
     */

    @Override
    public void onRegister(boolean register)
    {
        if (register) {
            Log.d(LOGTAG,"Successfully registered to server");

            // Change to ChatActivity
            Intent i = new Intent(this, ChatActivity.class);
            this.startActivity(i);
        }
    }

    @Override
    public void onError()
    {
        Log.d(LOGTAG,"Error registering to server");
        errorMessage(getString(R.string.server_error));
    }

    @Override
    public void onReceivedChatLog(List<String> messages) {} // Do nothing here
}
