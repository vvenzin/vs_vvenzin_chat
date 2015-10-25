package ch.ethz.inf.vs.vsvvenzinchat;

import android.app.Application;

import java.lang.reflect.Constructor;

/**
 *
 * Created by Valentin on 23/10/15.
 *
 * Use this class to get a singleton instance of a ServiceManager.
 * This is useful when the manager (and the service) wants to be shared across different activities.
 *
 */
public class ServiceManagerSingleton {

    private ServiceManager mServiceManager;
    private static ServiceManagerSingleton instance = null;

    // Create the ServiceManager - This constructor cant be called from outside
    private ServiceManagerSingleton(Application app, Class serviceManagerImpl)
    {
        Constructor<?> ctor = null;
        try {
            ctor = serviceManagerImpl.getConstructor(Application.class);
        } catch (NoSuchMethodException e) {e.printStackTrace();}
        try {
            mServiceManager = (ServiceManager) ctor.newInstance(new Object[] { app });
        } catch(Exception e) {e.printStackTrace();}
    }

    /**
     *
     * @param serviceManagerImpl    - class of ServiceManager
     * @return     - A single instance of the ServiceManager
     */
    public static ServiceManager getInstance(Application app, Class serviceManagerImpl) {
        if(instance == null) {
            instance = new ServiceManagerSingleton(app, serviceManagerImpl);
        }
        return instance.mServiceManager;
    }



}
