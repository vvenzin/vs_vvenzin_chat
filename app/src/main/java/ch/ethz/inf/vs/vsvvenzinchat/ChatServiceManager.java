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


    // Command:  arg1 == 1 -> register  arg1 = 0 -> deregister
    // Response: arg1 == 1 -> success   arg1 = 0 -> failure
    static final int MSG_REGISTER = MSG_1;

    // No arguments needed to Service
    // From service: If arg1 == 1: all messages received else arg3 should contain content String
    static final int MSG_CHAT_LOG = MSG_3;

    // Only to service to tell port ip and name.
    // arg1 for port and arg3 for ip, arg4 for name.
    // arg2 == -1 if want to reset all info.
    static final int MSG_ADDRESS = MSG_4;


    private List<String> mMessageBuffer;

    public ChatServiceManager(Application app)
    {
        super(app);
        Log.d(LOGTAG, "ChatServiceManager()");
        mMessageBuffer = new ArrayList<>();
    }

    public void setIpPortName(String ip, int port,String name) {sendMessageToService(MSG_ADDRESS, port, 0, ip, name);}

    public void invalidateIpPortName() {sendMessageToService(MSG_ADDRESS,0,-1,null,null);}

    public void getChatLog() {sendMessageToService(MSG_CHAT_LOG,0,0,null,null);}

    // Register to server -> callback when registered or when error
    public void register(boolean register)
    {
        int arg = 0;
        if (register) arg = 1;
        sendMessageToService(MSG_REGISTER, arg, 0, null, null);
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
                    else for (ChatServiceManagerListener l: mListener) l.onRegisterError();
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

        }
    }

}
