package edu.umd.fcmd.sensorlisteners.listener.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.awareness.SnapshotApi;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.common.api.ResultCallback;

import edu.umd.fcmd.sensorlisteners.model.LocationState;

/**
 * A asynchronous timed location task being started to retrieve
 * a location update every 15 seconds.
 */
class TimedLocationTask extends AsyncTask<Void, Location, Void> {
    private final String TAG = getClass().getSimpleName();
    private static final int LOCATION_SLEEP_TIME = 1500;
    private final SnapshotApi snapshotApi;
    private final LocationListener locationListener;

    /**
     * Constructor called by a factory.
     * @param locationListener the listener to connect to.
     * @param snapshotApi the snapshot api from google.
     */
    TimedLocationTask(LocationListener locationListener, SnapshotApi snapshotApi) {
        if ((locationListener != null) && (snapshotApi != null)) {
            this.locationListener = locationListener;
            this.snapshotApi = snapshotApi;
        } else {
            throw new NullPointerException("Cannot create a new TimedLocationTask for null parametes.");
        }
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    @Nullable
    @Override
    protected Void doInBackground(Void... params) {
        if (ContextCompat.checkSelfPermission(locationListener.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted");
            while (!isCancelled()) {
                //noinspection ObjectAllocationInLoop
                snapshotApi.getLocation(locationListener.getmGoogleApiClient())
                        .setResultCallback(new ResultCallback<LocationResult>() {
                            @Override
                            public void onResult(@NonNull LocationResult r) {
                                if (r.getStatus().isSuccess()) {
                                    publishProgress(r.getLocation());
                                    Log.d(TAG, "New Location results available");
                                } else {
                                    Log.e(TAG, "could not retrieve result");
                                }
                                //Log.d(TAG, "OnResult called");
                            }
                        });
                //Log.d(TAG, "Attempt to get location");
                try {
                    //Log.d(TAG, "Sleep now");
                    //noinspection BusyWait
                    Thread.sleep((long) LOCATION_SLEEP_TIME);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    Log.d(TAG, "Sleep has been tried to interrupt, but thread interrupted the interrupting Thread.");
                }
            }
        } else {
            Log.d(TAG, "Permission denied");
        }
        return null;
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    @Override
    protected void onProgressUpdate(Location... values) {
        Location location = values[0];
        LocationState state = new LocationState();
        state.setDate(System.currentTimeMillis());
        state.setAccuracy((double) location.getAccuracy());
        state.setAltitude(location.getAltitude());
        state.setBearing((double) location.getBearing());
        state.setExtras(location.getExtras());
        state.setLatitude(location.getLatitude());
        state.setLongitude(location.getLongitude());
        locationListener.onUpdate(state);
    }
}