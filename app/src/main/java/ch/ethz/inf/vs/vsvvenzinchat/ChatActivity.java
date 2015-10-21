package ch.ethz.inf.vs.vsvvenzinchat;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    // Constants
    private final String LOGTAG = "## VV-ChatActivity ##";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Log.d(LOGTAG, "onCreate");

        Button b = (Button) findViewById(R.id.chat_log_btn);
        b.setOnClickListener(this);
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
            case R.id.chat_log_btn:

                // TODO: Stuff from task 3

                break;
        }
    }

}
