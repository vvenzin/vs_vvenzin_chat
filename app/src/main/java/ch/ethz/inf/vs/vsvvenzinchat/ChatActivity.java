package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

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
    }

    @Override
    protected void onEnterForeground()
    {
        super.onEnterForeground();
        Log.d(LOGTAG,"onEnterForeground()");
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

        mClientManager.register(false); // Unregister from server
        mClientManager.unregisterListener(this); // Unregister self from service callback
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {super.onConfigurationChanged(conf);}


    public void onClick(View b)
    {
        switch (b.getId()) {
            case R.id.chat_log_btn:
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
    public void onRegisterError()
    {
        Log.d(LOGTAG,"Error registering to server");
        errorMessage(getString(R.string.register_error));
    }

    @Override
    public void onRegister(boolean register)
    {
        if (!register) {
            Log.d(LOGTAG,"Deregister from server");
        }
    }

    @Override
    public void onReceivedChatLog(List<String> messages)
    {
        Log.d(LOGTAG,"Received chat log");

        String display = "";
        for (String m : messages) display += m + "\n";
        mChatTextView.setText(display);

        // TODO: Task 3
    }
}
