package ch.ethz.inf.vs.vsvvenzinchat;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valentin on 22/10/15.
 */
public class UDPClientManager {


    // Constants
    private final String LOGTAG = "## VV-UDPClientM ##";


    private UDPClientManagerListener mListener;
    private ServiceConnection mConnection;
    Messenger mService = null;
    boolean mIsBound = false;


    /**
     *
     * Message passing
     *
     */

    // Command:  arg1 == 1 -> register  arg1 = 0 -> deregister
    // Response: arg1 == 1 -> success   arg1 = 0 -> failure
    static final int MSG_REGISTER = 0;

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;

    // Target we publish for clients to send messages to IncomingHandler.
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_REGISTER:
                    if (mListener != null) mListener.onRegister((msg.arg1 == 1));
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    UDPClientManager(UDPClientManagerListener listener)
    {
        Log.d(LOGTAG, "UDPClientManager()");
        mListener = listener;
        initConnection();
    }

    public void unregisterListener() {mListener = null;}


    // Register to server -> callback when registered or when error
    public void register(boolean register)
    {
        int arg = 0;
        if (register) arg = 1;
        try {
            Message msg = Message.obtain(null, MSG_REGISTER, arg, 0);
            msg.replyTo = mMessenger;
            mService.send(msg);
        }
        catch (RemoteException e) {e.printStackTrace();}
    }

    public ServiceConnection getConnection()
    {
        return mConnection;
    }

    // Unbind from ServerService
    public void unbindService()
    {
        if (mIsBound) {
            if(mService != null) {
                try {
                    Message msg = Message.obtain(null, MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {e.printStackTrace();}
                mIsBound = false;
                Log.d(LOGTAG, "Unbind ServerService");
            }
        }
    }

    // Unbind from ServerService
    public void bindService() {mIsBound = true;}

    // Helper function
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
