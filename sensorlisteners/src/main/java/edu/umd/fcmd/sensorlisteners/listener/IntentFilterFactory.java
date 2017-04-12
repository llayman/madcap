package edu.umd.fcmd.sensorlisteners.listener;

import android.content.IntentFilter;

import javax.inject.Inject;

/**
 * Created by MMueller on 9/26/2016.
 *
 * IntentFilterFactory class following the well known
 * factory pattern.
 */

public class IntentFilterFactory {

    @Inject
    public IntentFilterFactory() {}

    /**
     * Creating a new instance.
     * @return a new instance.
     */
    public IntentFilter create() {
        return new IntentFilter();
    }

}
