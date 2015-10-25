package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import ch.ethz.inf.vs.helperclasses.EnhancedActivity;

public class ChatActivity extends EnhancedActivity implements View.OnClickListener, ChatServiceManagerListener {

    // Constants
    private final String LOGTAG = "## VV-ChatActivity ##";


    private ChatServiceManager mClientManager;
    private TextView mChatTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Log.d(LOGTAG, "onCreate");

        Button b = (Button) findViewById(R.id.chat_log_btn);
        b.setOnClickListener(this);

        mChatTextView = (TextView) findViewById(R.id.chat_text_view);

        // Bind to service
        mClientManager = (ChatServiceManager) ServiceManagerSingleton.getInstance(getApplication(), ChatServiceManager.class);
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        Log.d(LOGTAG, "onStart()");
        mClientManager.start();
        mClientManager.registerListener(this);

        // If not registered anymore change back to homescreen
        if(!mClientManager.isRegistered()){
            Intent i1 = new Intent(this, MainActivity.class);
            this.startActivity(i1);
        }
    }

    @Override
    protected void onEnterForeground()
    {
        super.onEnterForeground();
        Log.d(LOGTAG, "onEnterForeground()");
    }

    @Override
    protected void onEnterBackground()
    {
        super.onEnterBackground();
        Log.d(LOGTAG, "onEnterBackground()");

        mClientManager.stop();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.d(LOGTAG, "onStop()");

        mClientManager.register(false,null,-1,null); // Unregister from server -> Doesnt handle unregister failure yet
        mClientManager.unregisterListener(this); // Unregister self from service callback
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {super.onConfigurationChanged(conf);}


    public void onClick(View b)
    {
        switch (b.getId()) {
            case R.id.chat_log_btn:
                mChatTextView.setText("");
                mClientManager.getChatLog();
                break;
        }
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

    @Override
    public void onError()
    {
        Log.d(LOGTAG, "Error getting chat log");
        errorMessage(getString(R.string.chatlog_error));
    }

    @Override
    public void onRegister(boolean register) {}

    @Override
    public void onReceivedChatLog(List<String> messages)
    {
        Log.d(LOGTAG,"Received chat log");

        String display = "";
        for (String m : messages) {
            display += getContent(m) + "   Timestamp: " + getTimeStamp(m) + "\n";
        }
        mChatTextView.setText(display);

        // TODO: Task 3
    }

    // Get timestamp from json message
    private String getTimeStamp(String message)
    {
        // TODO: Extract timestamp and do something useful
        try {
            JSONObject jpkt = new JSONObject(message);
            JSONObject header = jpkt.getJSONObject(getString(R.string.json_header));

            // Should probably be handled as JSON too
            String timestamp = header.getString(getString(R.string.json_timestamp));
            return timestamp;

        } catch (JSONException e) {e.printStackTrace();}
        return "ERROR while getting timestamp";
    }

    // Get message's content
    private String getContent(String message)
    {
        try {
            JSONObject jpkt = new JSONObject(message);
            JSONObject body = jpkt.getJSONObject(getString(R.string.json_body));
            return body.getString(getString(R.string.json_content));
        } catch (JSONException e) {e.printStackTrace();}
        return "ERROR while getting content";
    }
}
