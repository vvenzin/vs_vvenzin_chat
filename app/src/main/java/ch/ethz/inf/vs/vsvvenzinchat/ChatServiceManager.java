package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.Application;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by Valentin on 22/10/15.
 *
 */

public class ChatServiceManager extends ServiceManager{


    // Constants
    private final String LOGTAG = "## VV-UDPClientM ##";


    // Info, only need as cache
    private String mIp;
    private int mPort;
    private String mName;

    /**
     * To service:      arg1=1 -> register, arg1=0 -> deregister
     * From service:    arg1=1 -> success,  arg1=0 -> failure
     */
    static final int MSG_REGISTER = MSG_1;

    /**
     * To service:      nor arguments
     * From service:    arg1=1 -> all messages arrived, may process them
     *                  arg1!=1 -> arg3 contains message header and arg4 message body
     */
    static final int MSG_CHAT_LOG = MSG_3;

    /**
     * To service:      arg1=port, arg3=ip, arg4=name
     *                  arg2=-1 -> invalidate all info
     * From service:    arg1=1 -> wanted to register but hadnt info
     *                  arg1=2 -> wanted to deregister but hadnt info
     */
    static final int MSG_ADDRESS = MSG_4;

    // Store messages until all arrived
    private List<String> mMessageBuffer;

    public ChatServiceManager(Application app)
    {
        super(app);
        Log.d(LOGTAG, "ChatServiceManager()");
        mMessageBuffer = new ArrayList<>();
    }

    // Call to set the service's ip, port and name
    public void setIpPortName(String ip, int port,String name) {sendMessageToService(MSG_ADDRESS, port, 0, ip, name);}

    // Invalidates services ip, port and name
    public void invalidateIpPortName() {sendMessageToService(MSG_ADDRESS,0,-1,null,null);}

    // Request chat log
    public void getChatLog() {sendMessageToService(MSG_CHAT_LOG,0,0,null,null);}

    // Register to server
    public void register(boolean register, String ip, int port,String name)
    {
        mIp = ip;
        mPort = port;
        mName = name;
        int arg = 0; // deregister
        if (register) arg = 1; // register
        sendMessageToService(MSG_REGISTER, arg, 0, null, null);
    }

    public boolean isRegistered() {return ChatService.isRegistered();}

    private boolean infoReady()
    {
        return (mName != null && mIp != null && !mName.equals("") && !mIp.equals("") && mPort > 999 && mPort < 10000);
    }

    /**
     *
     * Abstract methods
     *
     */

    protected void handleCustomMessage(Message msg)
    {
        switch (msg.what){
            case MSG_REGISTER:
                if (mListener != null) {
                    if (msg.arg2 == 0) for (ChatServiceManagerListener l: mListener) l.onRegister((msg.arg1 == 1));
                    else for (ChatServiceManagerListener l: mListener) l.onError();
                }
                break;
            case MSG_CHAT_LOG:
                if(msg.arg1 == 1) { // All messages arrived
                    List messages = new ArrayList();
                    for (String m : mMessageBuffer) messages.add(m); // Copy list
                    for (ChatServiceManagerListener l: mListener) l.onReceivedChatLog(messages);
                    mMessageBuffer.removeAll(mMessageBuffer);
                } else {
                    Bundle b = msg.getData();
                    mMessageBuffer.add(b.getString("arg3"));
                }
                break;
            case MSG_ADDRESS:
                // Service hadnt info to register so
                if (infoReady()) {
                    sendMessageToService(MSG_ADDRESS, mPort, 0, mIp, mName);

                    if (msg.arg1 == 1) sendMessageToService(MSG_REGISTER, 1, 0, null, null);
                    else if (msg.arg1 == 2) sendMessageToService(MSG_REGISTER, 0, 0, null, null);

                }
                else Log.d(LOGTAG,"Service needs info but manager hasnt");

                break;
        }
    }

}
