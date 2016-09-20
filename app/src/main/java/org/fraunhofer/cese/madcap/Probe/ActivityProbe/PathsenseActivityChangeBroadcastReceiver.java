/*
 * Copyright (c) 2015 PathSense, Inc.
 */

package org.fraunhofer.cese.madcap.Probe.ActivityProbe;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.pathsense.android.sdk.location.PathsenseActivityRecognitionReceiver;
import com.pathsense.android.sdk.location.PathsenseDetectedActivities;

public class PathsenseActivityChangeBroadcastReceiver extends PathsenseActivityRecognitionReceiver {
	private ActivityProbe callback;
	static final String TAG = PathsenseActivityChangeBroadcastReceiver.class.getName();

	public PathsenseActivityChangeBroadcastReceiver(ActivityProbe callback){
		this.callback = callback;
	}

	@Override
	protected void onDetectedActivities(Context context, PathsenseDetectedActivities detectedActivities)
	{
		Log.i(TAG, "detectedActivities = " + detectedActivities);
		// broadcast detected activities
		Intent detectedActivitiesIntent = new Intent("activityChange");
		detectedActivitiesIntent.putExtra("detectedActivities", detectedActivities);
		callback.sendData(detectedActivitiesIntent);
	}
}
