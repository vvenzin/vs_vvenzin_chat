package ch.ethz.inf.vs.vsvvenzinchat;

import java.util.List;

/**
 * Created by Valentin on 22/10/15.
 */
public interface ChatServiceManagerListener {

    public void onRegister(boolean success);

    public void onRegisterError();

    public void onReceivedChatLog(List<String> messages);

}
