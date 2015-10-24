package ch.ethz.inf.vs.vsvvenzinchat;

import android.os.AsyncTask;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;

public class ChatService extends Service {

    // Constants
    private final String LOGTAG = "## VV-ChatService ##";
    private final int PKT_SIZE = 256;
    private final int NO_RETRY = 5;
    private final int TIMEOUT = 10000; // 10s
    private final int TIMEOUT_LASR_MSG = 100;

    // Types of messages for client <-> server communication
    public enum MSG {REGISTER, DEREGISTER, RETRIEVE_LOG, ACK, ERROR, MESSAGE}
    private final String REGISTER_STR = "register";
    private final String DEREGISTER_STR = "deregister";
    private final String ACK_STR = "ack";
    private final String RETRIEVE_LOG_STR = "retrieve_chat_log";
    private final String ERROR_STR = "error";
    private final String MESSAGE_STR = "message";

    private static boolean isRunning = false;
    public static boolean isRunning() {return isRunning;}


    // Info
    private int mPort;
    private String mIp;
    private String mName;

    // Server Client
    private DatagramSocket mSocket;
    private boolean mRegistered;
    private String mUUID;
    private RunnableArg mRegisterHandler;
    private RunnableArg mDeregisterCleanupHandler;
    private RunnableArg mDeregisterHandler;
    private RunnableArg mChatLogHandler;


    /**
     *
     *
     * ################# Message passing Service <-> ServiceManager #################
     *
     *
     */


    // Target we publish for clients to send messages to IncomingHandler.
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case ChatServiceManager.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo); // Add caller
                    break;
                case ChatServiceManager.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo); // Remove caller
                    break;

                case ChatServiceManager.MSG_STOP:
                    Log.d(LOGTAG, "Going to kill myself");
                    isRunning = false;
                    if (mSocket != null) {
                        mSocket.close();
                        mSocket = null;
                    }
                    stopSelf();
                    break;

                case ChatServiceManager.MSG_REGISTER:
                    if (msg.arg1 == 1) register();
                    else deregister(false); // Deregister but leave socket
                    break;
                case ChatServiceManager.MSG_ADDRESS:
                    if (msg.arg2 == 0) {
                        // Regular info update
                        mPort = msg.arg1;
                        mIp = msg.getData().getString("arg3");
                        mName = msg.getData().getString("arg4");
                        try {
                            if (mSocket == null) mSocket = new DatagramSocket(mPort);
                            else {
                                mSocket.close();
                                mSocket = new DatagramSocket(mPort);
                            }
                        } catch (SocketException e) {e.printStackTrace();}
                    } else {
                        // Invalidate info
                        mPort = -1;
                        mIp = "";
                        mName = "";
                        deregister(true); // This closes socket afterwards
                    }
                    break;
                case ChatServiceManager.MSG_CHAT_LOG:
                    getChatLog();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    // Send Message to all clients of service
    private void sendMessageToClients(int messageType, int arg1, int arg2, String arg3, String arg4) {
        try {
            Message msg = null;
            switch (messageType) {
                case ChatServiceManager.MSG_REGISTER_CLIENT:
                case ChatServiceManager.MSG_UNREGISTER_CLIENT:
                case ChatServiceManager.MSG_START:
                case ChatServiceManager.MSG_STOP:
                case ChatServiceManager.MSG_1:
                case ChatServiceManager.MSG_2:
                    msg = Message.obtain(null, messageType, arg1, arg2);
                    break;
                case ChatServiceManager.MSG_3:
                case ChatServiceManager.MSG_4:
                    Bundle b = new Bundle();
                    b.putString("arg3", arg3);
                    b.putString("arg4",arg4);
                    msg = Message.obtain(null, messageType);
                    msg.setData(b);
                    msg.arg1 = arg1;
                    msg.arg2 = arg2;
                    break;
                default:
                    msg = Message.obtain(null, ChatServiceManager.MSG_DEFAULT);
                    break;
            }
            msg.replyTo = mMessenger;
            for (Messenger c : mClients) c.send(msg);
        } catch (RemoteException e) {e.printStackTrace();}
    }


    /**
     *
     *
     * ################# Service Callbacks #################
     *
     *
     */


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOGTAG, "onCreate()");
        isRunning = true;

        mUUID = UUID.randomUUID().toString();
    }

    @Override
    public IBinder onBind(Intent intent) { return mMessenger.getBinder();} // Pass 'Mailbox' to client activity

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOGTAG, "onStartCommand()");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy()");

    }



    /**
     *
     *
     * ################# Client <-> Server related #################
     *
     *
     */


    public ChatService()
    {
        mRegistered = false;

        // Definde functions to be called when received a packet
        mRegisterHandler = new RunnableArg() {
            @Override
            public void run() {}
            @Override
            public void run(Object... args)
            {
                super.run(args);
                if (args.length != 2) Log.d(LOGTAG,"Illegal use of pkt handler");
                else {
                    JSONObject header = (JSONObject) args[0];
                    JSONObject body = (JSONObject) args[1];

                    try {
                        String type =  header.getString(getString(R.string.json_type));
                        switch (type) {
                            case ACK_STR:
                                Log.d(LOGTAG, "Got ack - succesfully registered");
                                mRegistered = true;
                                int arg = 0;
                                if (mRegistered) arg = 1;
                                sendMessageToClients(ChatServiceManager.MSG_REGISTER, arg, 0, null, null);
                                break;
                            case ERROR_STR:
                                Log.d(LOGTAG,"Got error while registering - probably already registered - do as if??");
                                // TODO: Something smart

                                mRegistered = true;
                                arg = 0;
                                if (mRegistered) arg = 1;
                                sendMessageToClients(ChatServiceManager.MSG_REGISTER, arg, 0, null, null);
                                break;
                            default:
                                Log.d(LOGTAG,"Got wrong messagetype while (de)registering: " + type);
                        }
                    } catch (JSONException e) {e.printStackTrace();}

                }
            }
        };
        mDeregisterHandler = new RunnableArg() {
            @Override
            public void run() {}
            @Override
            public void run(Object... args)
            {
                super.run(args);
                if (args.length != 2) Log.d(LOGTAG,"Illegal use of pkt handler");
                else {
                    JSONObject header = (JSONObject) args[0];
                    JSONObject body = (JSONObject) args[1];

                    try {
                        String type =  header.getString(getString(R.string.json_type));
                        switch (type) {
                            case ACK_STR:
                                Log.d(LOGTAG, "Got ack - succesfully deregistered");
                                mRegistered = false;
                                int arg = 0;
                                if (mRegistered) arg = 1;
                                sendMessageToClients(ChatServiceManager.MSG_REGISTER, arg, 0, null, null);
                                break;
                            case ERROR_STR:
                                Log.d(LOGTAG, "Got error while deregistering");
                                if (mRegistered) reportServerProblem();
                                else Log.d(LOGTAG,"Probably lready deregistered");
                                break;
                            default:
                                Log.d(LOGTAG,"Got wrong messagetype while deregistering: " + type);
                        }
                    } catch (JSONException e) {e.printStackTrace();}

                }
            }
        };
        mDeregisterCleanupHandler = new RunnableArg() {
            @Override
            public void run() {}
            @Override
            public void run(Object... args) {
                super.run(args);
                if (args.length != 2) Log.d(LOGTAG,"Illegal use of pkt handler");
                else {
                    JSONObject header = (JSONObject) args[0];
                    JSONObject body = (JSONObject) args[1];
                    try {
                        String type =  header.getString(getString(R.string.json_type));
                        switch (type) {
                            case ACK_STR:
                                Log.d(LOGTAG, "Got ack - succesfully deregistered cleanup");
                                break;
                            case ERROR_STR:
                                Log.d(LOGTAG, "Got error while deregistering cleaunp");
                                break;
                            default:
                                Log.d(LOGTAG,"Got wrong messagetype while deregistering cleanup: " + type);
                        }
                    }
                    catch (JSONException e) {e.printStackTrace();}
                    mRegistered = false;
                    if(mSocket != null) {
                        mSocket.close();
                        mSocket = null;
                    }

                }
            }
        };

        mChatLogHandler = new RunnableArg() {
            @Override
            public void run() {}
            @Override
            public void run(Object... args)
            {
                super.run(args);
                if (args.length != 2) Log.d(LOGTAG,"Illegal use of pkt handler");
                else {
                    JSONObject header = (JSONObject) args[0];
                    JSONObject body = (JSONObject) args[1];
                    try {
                        String type =  header.getString(getString(R.string.json_type));
                        switch (type) {
                            case MESSAGE_STR:
                                Log.d(LOGTAG,"Got message " + body.getString(getString(R.string.json_content)).toString());

                                String content = body.getString(getString(R.string.json_content));
                                sendMessageToClients(ChatServiceManager.MSG_CHAT_LOG,0,0,content,null);

                                break;
                            case ERROR_STR:
                                Log.d(LOGTAG, "Got error while while receiving chatlog");
                                break;
                            default:
                                Log.d(LOGTAG,"Got wrong messagetype while receiving chatlog:" + type);
                        }
                    }
                    catch (JSONException e) {e.printStackTrace();}
                }
            }
        };

    }

    // Register to server
    private void register()
    {
        AsyncSender sender = new AsyncSender();
        AsyncReceiver receiver = new AsyncReceiver();
        RunnableArg callOnTimout = new RunnableArg()
        {
            @Override
            public void run() { }
            @Override
            public void run(Object... args) {
                super.run(args);
                boolean last = (boolean) args[0];
                // Send again
                if (!last) {
                    AsyncSender sender = new AsyncSender();
                    sender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MSG.REGISTER);
                } else {
                    reportServerProblem();
                }
            }
        };
        receiver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,1,mRegisterHandler,NO_RETRY, TIMEOUT,callOnTimout);
        sender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,MSG.REGISTER);

    }

    // Deregister from server - Cleanup to true for closing socket when done
    private void deregister(boolean cleanup)
    {
        AsyncSender sender = new AsyncSender();
        AsyncReceiver receiver = new AsyncReceiver();
        RunnableArg callOnTimout = new RunnableArg()
        {
            @Override
            public void run() { }
            @Override
            public void run(Object... args) {
                super.run(args);
                boolean last = (boolean) args[0];
                // Send again
                if (!last) {
                    AsyncSender sender = new AsyncSender();
                    sender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MSG.DEREGISTER);
                } else {
                    reportServerProblem();
                }
            }
        };
        if (cleanup) {
            receiver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,1,mDeregisterCleanupHandler);
        } else {
            receiver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,1,mDeregisterHandler,NO_RETRY, TIMEOUT,callOnTimout);
        }
        sender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MSG.DEREGISTER);

    }

    private void reportServerProblem()
    {
        // Could makre more general because this gets always fired when didnt receive any message after timout
        Log.d(LOGTAG, "Didnt receive ack");
        sendMessageToClients(ChatServiceManager.MSG_REGISTER, 0, 1, null, null);
    }


    private void getChatLog()
    {
        AsyncSender sender = new AsyncSender();
        AsyncReceiver receiver = new AsyncReceiver();
        RunnableArg callOnTimout = new RunnableArg() {
            public void run() {
                // Tell manager that no packets are arriving anymore
                sendMessageToClients(ChatServiceManager.MSG_CHAT_LOG,1,0,null,null);
            }
        };
        receiver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0, mChatLogHandler,0,TIMEOUT_LASR_MSG,callOnTimout);
        sender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,MSG.RETRIEVE_LOG);
    }

    // Sanity check for info
    private boolean infoReady()
    {
        boolean ready = (mIp != null && mName != null && mPort > 999 && !mIp.equals("") && !mName.equals(""));
        if (!ready) Log.d(LOGTAG,"Not ready. name: " + mName + " ip: " + mIp + " mPort " + Integer.toString(mPort));
        return ready;
    }

    /**
     *
     * ################# AsyncTasks and UDP related #################
     *
     */

    // Receive one ore multiple packets, with or without timeout
    public class AsyncReceiver extends AsyncTask<Object, Void, String> {

        /**
         * Must follow conventions below
         *
         * @param params
         * params[0]: int noPktExpected,         - # packets ecpected - if 0 listen for maxPkts packets
         * params[1]: RunnableArg pktHandler,    - Handler for received packet
         * params[2]: int retry,                 - (Optional) # of retries when timeout
         * params[3]: int timeout,               - (Optional) # milliseconds timeout
         * params[4]:Runnable callOnTimout       - (Optional) what to call when timeout
         */
        @Override
        protected String doInBackground(Object... params)
        {
            // Read argy
            int maxPkts = 1000; // Approx of listen forever
            int exptNoPkt = maxPkts;
            RunnableArg pktHandler = null;
            int timeout = 0;
            int retry = 1; // Must obviously try at least once
            RunnableArg callOnTimout = null;
            if (params.length >= 2) {
                exptNoPkt = (int) params[0];
                if (exptNoPkt == 0) exptNoPkt = maxPkts;
                pktHandler = (RunnableArg) params[1];
            }
            if (params.length >= 4) {
                retry += (int) params[2];
                timeout = (int) params[3];
            }
            if (params.length >= 5) callOnTimout = (RunnableArg) params[4];

            int noPkt = 0;
            do {
                noPkt = 0;
                try {
                    mSocket.setSoTimeout(timeout);
                    byte[] receiveData = new byte[PKT_SIZE];

                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    while(noPkt < exptNoPkt) // Bound while(true) -- prevent flooding
                    {
                        if (mSocket.isClosed()) {
                            Log.d(LOGTAG, "Wanted to receive - but socket was closed");
                            break;
                        }
                        else mSocket.receive(receivePacket);

                        try {
                            JSONObject jpkt = new JSONObject(new String(receivePacket.getData(), "UTF-8"));
                            JSONObject header = jpkt.getJSONObject(getString(R.string.json_header));
                            JSONObject body = jpkt.getJSONObject(getString(R.string.json_body));
                            pktHandler.run(header, body); // Handle packet
                        } catch (JSONException e) {e.printStackTrace();}
                        noPkt++;
                    }

                    retry = -1; // Break out of loop beacuse succeeded
                }
                catch (SocketTimeoutException e) {
                    retry--;
                    if (callOnTimout != null && retry > 0) callOnTimout.run(false);
                    else if (callOnTimout != null && retry == 0) callOnTimout.run(true);
                    if (retry > 0) Log.d(LOGTAG, "Timeout... retry");
                    else Log.d(LOGTAG,"Timeout... stop");
                }
                catch (UnknownHostException e) {e.printStackTrace();}
                catch (IOException e) {e.printStackTrace();}
            } while (retry > 0);

            String retVal = "ok";
            if (retry == 0) retVal = "nok";
            if (noPkt == maxPkts) retVal = "flooded";
            return retVal;
        }

        @Override
        protected void onPostExecute(String result)
        {
            /*
            Log.d(LOGTAG,"Receiver returned with: " + result);

            switch (result) {
                case "nok":
                    break;
                case "ok":
                case "flooded":
                default:
                    // Handle other return values here
            }*/
        }
    }

    // Sends packet to server - invoke sender after receiver
    public class AsyncSender extends AsyncTask<Object, Void, String> {

        private InetAddress ip;

        /**
         *
         * @param params - params[0]: MSG mesaggeType
         * @return
         */
        @Override
        protected String doInBackground(Object... params)
        {
            if (!infoReady()) return "not_ready";
            try {
                ip = InetAddress.getByName(mIp);

                // Create and send packet
                DatagramPacket pkt = makePacket((MSG) params[0],ip, mPort);
                mSocket.send(pkt);
            }
            catch (UnknownHostException e) {e.printStackTrace();}
            catch (IOException e) {e.printStackTrace();}
            return "";
        }

        @Override
        protected void onPostExecute(String result) {}
    }


    // Create packet
    private DatagramPacket makePacket(MSG type, InetAddress ip, int port)
    {
        if (!infoReady()) return null;
        byte[] sendData = new byte[PKT_SIZE];

        // Build JSON
        JSONObject jpkt = new JSONObject();
        JSONObject jheader = new JSONObject();
        try {
            jheader.put(getString(R.string.json_username), mName);
            jheader.put(getString(R.string.uuid),mUUID);
            jheader.put(getString(R.string.json_timestamp),getString(R.string.json_empty));

            switch (type) {
                case REGISTER:
                    jheader.put(getString(R.string.json_type),getString(R.string.msg_register));
                    break;
                case DEREGISTER:
                    jheader.put(getString(R.string.json_type),getString(R.string.msg_deregister));
                    break;
                case ACK:
                    jheader.put(getString(R.string.json_type),getString(R.string.msg_ack));
                    break;
                case RETRIEVE_LOG:
                    jheader.put(getString(R.string.json_type),getString(R.string.msg_retrieve_log));
                    break;
                case MESSAGE:
                    jheader.put(getString(R.string.json_type),getString(R.string.msg_message));
                    break;
                case ERROR:
                default:
                    jheader.put(getString(R.string.json_type),getString(R.string.msg_error));
            }

            jpkt.put(getString(R.string.json_header),jheader);
            jpkt.put(getString(R.string.json_body), getString(R.string.json_empty));
            sendData = jpkt.toString().getBytes("UTF-8");
        }
        catch (JSONException e) {e.printStackTrace();}
        catch (UnsupportedEncodingException e) {e.printStackTrace();}

        return new DatagramPacket(sendData,sendData.length,ip,port);
    }
}
