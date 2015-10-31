package ch.ethz.inf.vs.helperclasses;

import android.support.v7.app.AppCompatActivity;

/**
 *
 * Created by Valentin on 23/10/15.
 *
 * This additional level of indirection allows us to know when the Application enters the background
 * or foreground. By overriding the functions onEnterBackground and onEnterForeground we can react on
 * it.
 *
 */
public class EnhancedActivity extends AppCompatActivity {

    @Override
    protected void onStart()
    {
        super.onStart();
        if (SharedData.runningActivites == 0) {
            onEnterForeground();
        }
        SharedData.runningActivites++;

    }


    @Override
    protected void onStop()
    {
        super.onStop();
        SharedData.runningActivites--;
        if (SharedData.runningActivites == 0) {
            onEnterBackground();
        }
    }

    protected void onEnterBackground() {}
    protected void onEnterForeground() {}

}
