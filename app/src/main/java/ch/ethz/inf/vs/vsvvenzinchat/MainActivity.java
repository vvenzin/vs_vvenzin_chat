package ch.ethz.inf.vs.vsvvenzinchat;

import android.content.Context;
import android.content.Intent;
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

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Constants
    private final String LOGTAG = "## VV-MainActivity ##";


    private String mName;
    private int mPort;
    private InetAddress mIp;

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
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(textField.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    mName = textField.getText().toString();
                    Log.d(LOGTAG, "User entered new name " + mName);

                    // Store name in sharedPrefs
                    if (mName != null && !mName.equals(""))
                    {
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
        if(mPort < 1000 || mIp == null || mName == null || mName.equals("")) loadPrefs();

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
        Log.d(LOGTAG, "onConfigrationChanged()");

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

    public void onClick(View b)
    {
        switch (b.getId()) {
            case R.id.join_btn:
                register();
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

    private void register()
    {

    }

    // Returns true if found false if none stored yed
    private boolean loadPrefs()
    {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), getApplicationContext().MODE_PRIVATE);
        String port = sharedPref.getString(getString(R.string.saved_port), "");
        String ip = sharedPref.getString(getString(R.string.saved_server), "");

        if (ip.equals("") || port.equals("")) return false;
        try {
            mPort = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            Log.d(LOGTAG,"Error retrieving port");
            return false;
        }
        try {
            mIp = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            Log.d(LOGTAG,"Error creating inet address");
            return false;
        }
        Log.d(LOGTAG,"Fetched ip " + ip + " and port " + port);

        // Load name
        mName = sharedPref.getString(getString(R.string.saved_name),"");
        EditText textField = (EditText) findViewById(R.id.name_text_field);
        textField.setText(mName);
        return mName.equals("");
    }
}
