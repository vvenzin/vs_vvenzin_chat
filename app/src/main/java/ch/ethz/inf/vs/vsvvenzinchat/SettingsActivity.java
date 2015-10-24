package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


public class SettingsActivity extends EnhancedActivity implements View.OnClickListener{

    // Constants
    private final String LOGTAG = "## VV-SettingsActvty ##";

    private String mServerString;
    private String mPortString;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Log.d(LOGTAG,"onCreate()");

        Button b = (Button) findViewById(R.id.save_btn);
        b.setOnClickListener(this);

        // Install handlet to save name in mName when user clicks done
        final EditText serverTxtField = (EditText) findViewById(R.id.server_text_field);
        final EditText portTxtField = (EditText) findViewById(R.id.port_text_field);
        serverTxtField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        portTxtField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        serverTxtField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    // Clear focus
                    serverTxtField.clearFocus();
                    LinearLayout parent = (LinearLayout) findViewById(R.id.parent_layout);
                    parent.requestFocus();

                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(serverTxtField.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    mServerString = serverTxtField.getText().toString();
                    Log.d(LOGTAG, "User entered ip " + mServerString);
                    handled = true;
                }
                return handled;
            }
        });
        portTxtField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    // Clear focus
                    portTxtField.clearFocus();
                    LinearLayout parent = (LinearLayout) findViewById(R.id.parent_layout);
                    parent.requestFocus();

                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(portTxtField.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    mPortString = portTxtField.getText().toString();
                    Log.d(LOGTAG, "User entered port " + mPortString);
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    protected void onEnterBackground()
    {
        super.onEnterBackground();
        Log.d(LOGTAG,"onEnterBackground()");


    }


    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(LOGTAG, "onPause()");

    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(LOGTAG, "onResume()");

        // Check if should check for ip and port
        if (mPortString == null || mServerString == null || mPortString.equals("") || mServerString.equals("")) {
            getIpAndPort();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {super.onConfigurationChanged(conf);}


    public void onClick(View b)
    {
        switch (b.getId()) {
            case R.id.save_btn:
                if(validate()) save();
                else errorMessage();
                break;
        }
    }


    // Validate if mServer and mPort are valid
    boolean validate()
    {
        boolean ipV = mServerString.equals("");
        boolean portV = mPortString.equals("");

        // Port
        try {
            Integer port = Integer.parseInt(mPortString);
            portV = !(port < 1000 || port > 9999);
        } catch (NumberFormatException e) {}

        if (!ipV) {


            // IP
            try {
                String[] parts = mServerString.split("\\.");
                if (parts.length != 4) ipV = false;
                for (String s : parts) {
                    int i = Integer.parseInt(s);
                    if ((i < 0) || (i > 255)) ipV = false;
                }

                if (mServerString.endsWith(".")) ipV = false;
                ipV = true;
            } catch (NumberFormatException nfe) {
            }
        }
        return ipV && portV;
    }

    // Save values to shared preferences - gets only called when values validated
    private void save()
    {
        Log.d(LOGTAG, "Saving server data to shared preferences. IP:"
                + mServerString.toString() + " PORT:" + mPortString);
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), getApplicationContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.saved_port), mPortString);
        editor.putString(getString(R.string.saved_server), mServerString);
        editor.commit();
    }

    // Tell user that he should enter valid data
    private void errorMessage()
    {
        Log.d(LOGTAG, "Cannot save invalid data");

        // Display error
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage("Please enter valid address and port. Thank you.");
        dlgAlert.setTitle("ERROR");
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
        dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        save();

    }

    // Returns true if found false if none stored yed
    private boolean getIpAndPort()
    {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), getApplicationContext().MODE_PRIVATE);
        mPortString  = sharedPref.getString(getString(R.string.saved_port), "");
        mServerString = sharedPref.getString(getString(R.string.saved_server), "");

        // Fill text fields
        EditText textField = (EditText) findViewById(R.id.server_text_field);
        textField.setText(mServerString);
        textField = (EditText) findViewById(R.id.port_text_field);
        textField.setText(mPortString);

        return (!mPortString.equals("") && !mServerString.equals(""));
    }
}
