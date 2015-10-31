package ch.ethz.inf.vs.vsvvenzinchat;

import android.os.Message;

import java.util.Comparator;

/**
 * Created by Schnidi on 31.10.2015.
 */
public class MessageComparator implements Comparator<MyMessage> {

    //returns <0 if left happened before, otherwise >0
    public int compare(MyMessage lhs, MyMessage rhs){
        if(lhs.vecClock.happenedBefore(rhs.vecClock)) return -1;
        else return 1;
    }
}
