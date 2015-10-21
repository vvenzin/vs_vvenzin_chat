package ch.ethz.inf.vs.vsvvenzinchat;

import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.net.Inet4Address;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener{

    // Constants
    private final String LOGTAG = "## VV-SettingsActvty ##";

    private Inet4Address mServer;
    private int mPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
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
        // TODO: all
        return false;
    }

    // Save values to shared preferences
    private void save()
    {
        Log.d(LOGTAG, "Saving server data to shared preferences.\n IP:"
                + mServer.toString() + " PORT:" + Integer.toString(mPort));
        // TODO: all
    }

    // Tell user that he should enter valid data
    private void errorMessage()
    {
        Log.d(LOGTAG, "Cannot save invalid data");
        // TODO: all
    }
}
