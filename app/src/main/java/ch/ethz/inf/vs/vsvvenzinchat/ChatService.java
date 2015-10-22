package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.util.ArrayList;

public class ChatService extends Service {

    // Constants
    private final String LOGTAG = "## VV-ChatService ##";


    private static boolean isRunning = false;

    /**
     *
     * Message handling
     *
     */

    // Define Messages as static final int

    // Target we publish for clients to send messages to IncomingHandler.
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                // Switch over Messagetypes

                default:
                    super.handleMessage(msg);
            }
        }
    }

    public ChatService()
    {
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(LOGTAG, "onCreate()");
        isRunning = true;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(LOGTAG,"onBind");
        return mMessenger.getBinder(); // Pass 'Mailbox' to client activity
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOGTAG,"onStartCommand()");


        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(LOGTAG,"onDestroy()");

    }

    public static boolean isRunning() {return isRunning;}

}
