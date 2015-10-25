package ch.ethz.inf.vs.vsvvenzinchat;

import java.util.List;

/**
 * Created by Valentin on 22/10/15.
 */
public interface ChatServiceManagerListener {

    void onRegister(boolean register); // True for registering, false for deregistering
    void onError();
    void onReceivedChatLog(List<String> messages);

}
