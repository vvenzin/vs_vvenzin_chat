package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valentin on 23/10/15.
 *
 * This class shall be used as additional layer of indirection between the service and the application.
 * The ServiceManager creates and binds a service to the application. Activites may register themselves
 * as listener to the ServiceManager. If multiple activities want to use the same service, they can get
 * a singleton instance of the ServiceManager through ServiceManagerSigleton.
 *
 * To implement specific behavior for the specific service; subclass from ServiceManager and create an
 * appropriate ServiceManagerListener interface. You may then implement your own callbacks for the activities.
 *
 * Note that you must implement the low-level message passing in the service yourself. Take an example
 * from this class since it is its symmetric counterpart anyway.
 */


public abstract class ServiceManager {

    // Constants
    private final String LOGTAG = "## VV-ServiceManager ##";

    protected List<ChatServiceManagerListener> mListener;
    private ServiceConnection mConnection;
    Messenger mService = null;
    boolean mIsBound = false;
    private Application mApp;
    private List<Message> mMessageBuffer;

    /**
     *
     * Message passing
     *
     */

    static final int MSG_REGISTER_CLIENT = 0;
    static final int MSG_UNREGISTER_CLIENT = 1;
    static final int MSG_START = 2;
    static final int MSG_STOP = 3;
    static final int MSG_DEFAULT = 4;
    // To define and handle in subclass
    static final int MSG_1 = 5; // Only ints
    static final int MSG_2 = 6; // Only ints
    static final int MSG_3 = 7; // Ints and String
    static final int MSG_4 = 8; // Ints and String



    // Target we publish for clients to send messages to IncomingHandler.
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_REGISTER_CLIENT:
                    break;
                case MSG_UNREGISTER_CLIENT:
                    break;
                case MSG_DEFAULT:
                    break;
                case MSG_1:
                case MSG_2:
                case MSG_3:
                case MSG_4:
                    handleCustomMessage(msg);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    // Force sublcass to implement message handling for specific messages
    abstract void handleCustomMessage(Message msg);

    ServiceManager(Application app)
    {
        mListener = new ArrayList<>();
        mMessageBuffer = new ArrayList<>();
        mApp = app;
        initConnection(); // Start service
    }


    // Register/unregister observer
    public void registerListener(ChatServiceManagerListener listener)
    {
        // Register listener
        mListener.add(listener);
    }

    public void unregisterListener(ChatServiceManagerListener listener)
    {
        // Unregister listener
        // Assuming there is only one listener per class
        int i = 0;
        for (ChatServiceManagerListener l: mListener)
        {
            if (l.getClass().equals(listener.getClass())) break;
            i++;
        }
        mListener.remove(i);
    }

    // Starts Service if not started yet and register
    public void start()
    {
        if (!mIsBound) {
            // Bind service
            Context appContext = mApp.getApplicationContext();
            mApp.bindService(new Intent(appContext, ChatService.class),
                    mConnection, mApp.getApplicationContext().BIND_AUTO_CREATE);
            if (ChatService.isRunning()) Log.d(LOGTAG,"Service is already running - bind");
            else {
                mApp.startService(new Intent(appContext, ChatService.class));
                Log.d(LOGTAG, "Service is not running yet bind and start");
            }
            mIsBound = true;
        }
    }

    // This doesnt work yet.. somehow the service survives every time
    public void stop()
    {
        // Unbind service
        if (mIsBound) {
            if(mService != null) {
                sendMessageToService(MSG_UNREGISTER_CLIENT, 0, 0, null,null);
                mIsBound = false;
                Log.d(LOGTAG, "Unregister from service");
            }
        }
        sendMessageToService(MSG_STOP, 0, 0, null, null);
        mApp.unbindService(mConnection);
        mApp.stopService(new Intent(mApp.getApplicationContext(), ChatService.class));
    }

    // Send message to service
    protected void sendMessageToService(int messageType, int arg1, int arg2, String arg3,String arg4)
    {
        Log.d(LOGTAG,"Sending message to service: " + Integer.toString(messageType));
        try {
            Message msg = null;
            switch (messageType) {
                case MSG_REGISTER_CLIENT:
                case MSG_UNREGISTER_CLIENT:
                case MSG_START:
                case MSG_STOP:
                case MSG_1:
                case MSG_2:
                    msg = Message.obtain(null, messageType, arg1, arg2);
                    break;
                case MSG_3:
                case MSG_4:
                    Bundle b = new Bundle();
                    b.putString("arg3", arg3);
                    b.putString("arg4",arg4);
                    msg = Message.obtain(null, messageType);
                    msg.setData(b);
                    msg.arg1 = arg1;
                    msg.arg2 = arg2;
                    break;
                default:
                    msg = Message.obtain(null, MSG_DEFAULT);
                    break;
            }
            msg.replyTo = mMessenger;
            if (mService != null) mService.send(msg);
            else mMessageBuffer.add(msg);
        } catch (RemoteException e) {e.printStackTrace();}
    }


    // Helper function to initialize mConnection
    private void initConnection()
    {
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mService = new Messenger(service);
                Log.d(LOGTAG, "Service connected");
                try {
                    Message msg = Message.obtain(null, MSG_REGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                    for (Message m: mMessageBuffer) mService.send(m);
                    mMessageBuffer.removeAll(mMessageBuffer);
                }
                catch (RemoteException e) {e.printStackTrace();}
            }

            public void onServiceDisconnected(ComponentName className)
            {
                Log.d(LOGTAG, "Service unexpectedly disconnected");
                mService = null;
            }
        };
    }

}
