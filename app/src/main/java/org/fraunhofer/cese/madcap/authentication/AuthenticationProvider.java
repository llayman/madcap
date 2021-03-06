package org.fraunhofer.cese.madcap.authentication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.fraunhofer.cese.madcap.R;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Main entry point for authentication and getting the currently signed in user.
 * <p>
 * Created by MMueller on 9/29/2016.
 */
@Singleton
public class AuthenticationProvider {

    private static final String TAG = "AuthenticationProvider";

    private final GoogleApiAvailability googleApiAvailability;

    private final GoogleApiClient mGoogleApiClient;

    @Nullable
    private volatile GoogleSignInAccount user;

    @Nullable
    private volatile GoogleSignInAccount lastLoggedInUser;

    @Inject
    public AuthenticationProvider(@Named("SigninApi") GoogleApiClient googleApiClient, GoogleApiAvailability googleApiAvailability) {
        mGoogleApiClient = googleApiClient;
        this.googleApiAvailability = googleApiAvailability;
    }

    /**
     * Helper method for interactive signin to handle common SignInApi issues.
     *
     * @param activity   the calling activity. Must implement {@link android.app.Activity#onActivityResult(int, int, Intent)}
     * @param resultCode the result code that is listened for by the calling activity's {@link android.app.Activity#onActivityResult(int, int, Intent)} method
     * @param callback   callback class for handling common login events
     */
    @SuppressWarnings("SameParameterValue")
    void interactiveSignIn(@NonNull final SignInActivity activity, final int resultCode, @NonNull LoginResultCallback callback) {
        Timber.d("interactiveSignIn initiated");

        int connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (connectionResult != ConnectionResult.SUCCESS) {
            callback.onServicesUnavailable(connectionResult);
            return;
        }

        if (mGoogleApiClient.isConnected()) {
            activity.startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient), resultCode);
            //per Endpoints v2
//            GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(activity, "server:client_id:"+ Constants.ANDROID_AUDIENCE);
        } else {

            mGoogleApiClient.registerConnectionFailedListener(callback);
            mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(@Nullable Bundle bundle) {
                    mGoogleApiClient.unregisterConnectionCallbacks(this);
                    activity.startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient), resultCode);
                }

                @Override
                public void onConnectionSuspended(int i) {
                    Timber.w("onConnectionSuspended: Unexpected suspension of connection. Error code: " + i);
                }
            });
            mGoogleApiClient.connect();
        }
    }


    /**
     * Attempts to perform a silent login using cached credentials
     *
     * @param context  the calling context
     * @param callback callback handler for login event callbacks triggered during the silent login attempt.
     */
    public void silentLogin(@NonNull Context context, @NonNull final SilentLoginResultCallback callback) {
        Timber.d("silentSignIn initiated");

        int connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if (connectionResult != ConnectionResult.SUCCESS) {
            callback.onServicesUnavailable(connectionResult);
            return;
        }
        if (mGoogleApiClient.isConnected()) {
            doSignin(callback);
        } else {
            mGoogleApiClient.registerConnectionFailedListener(callback);
            mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(@Nullable Bundle bundle) {
                    mGoogleApiClient.unregisterConnectionCallbacks(this);
                    doSignin(callback);
                }

                @Override
                public void onConnectionSuspended(int i) {
                    Timber.w(TAG, "onConnectionSuspended: Unexpected suspension of connection. Error code: " + i);
                }
            });
            mGoogleApiClient.connect();
        }
    }

    /**
     * private method to handle actual google sign in events
     *
     * @param callback the callback to handle sign in events
     */
    private void doSignin(@NonNull final SilentLoginResultCallback callback) {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

        if (opr.isDone()) {
            // In Case there is a result available immediately. This should happen if they signed in before.
            GoogleSignInResult result = opr.get();
            Timber.d("Immediate result available: " + result);
            setUser(result.getSignInAccount());
            callback.onLoginResult(result);
        } else {
            Timber.d("Immediate results are not available.");
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult r) {
                    Timber.d("Received asynchronous login result. Code: " + r.getStatus().getStatusCode() + ", message: " + r.getStatus().getStatusMessage());
                    setUser(r.getSignInAccount());
                    callback.onLoginResult(r);
                }
            });
        }
    }

    /**
     * Attempts to log the user out using the Google SignIn API
     *
     * @param context  the calling context
     * @param callback callback handler for logout events
     */
    public void signout(@NonNull Context context, @NonNull final LogoutResultCallback callback) {
        setUser(null);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(context.getString(R.string.madcap_action_logout));
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

        int connectionResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (connectionResult != ConnectionResult.SUCCESS) {
            callback.onServicesUnavailable(connectionResult);
            return;
        }

        if (mGoogleApiClient.isConnected()) {
            doSignout(callback);
        } else {
            mGoogleApiClient.registerConnectionFailedListener(callback);
            mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(@Nullable Bundle bundle) {
                    mGoogleApiClient.unregisterConnectionCallbacks(this);
                    doSignout(callback);
                }

                @Override
                public void onConnectionSuspended(int i) {
                    Timber.w("onConnectionSuspended: Unexpected suspension of connection. Error code: " + i);
                }

            });
            mGoogleApiClient.connect();
        }
    }

    /**
     * Private method to handle actual calls to google signout
     *
     * @param callback specifies how to handle various signout events
     */
    private void doSignout(@NonNull final LogoutResultCallback callback) {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status r) {
                callback.onRevokeAccess(r);
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new SignOutResultCallback(callback));
            }
        });
    }

    /**
     * Gets the currently signed in user.
     *
     * @return the signed in user.
     */
    @Nullable
    public GoogleSignInAccount getUser() {
        return user;
    }

    /**
     * Returns the last logged in user, if any.
     *
     * @return The last logged in user.
     */
    @Nullable
    public GoogleSignInAccount getLastLoggedInUser() {
        return lastLoggedInUser;
    }


    /**
     * Sets the currently logged in user. Only exposed here to support interactive sign in with the google sign in activitiy.
     *
     * @param user the user to set as currently logged in
     */
    synchronized void setUser(@Nullable GoogleSignInAccount user) {
        if (this.user != null) {
            lastLoggedInUser = this.user;
            Timber.d("lastLoggedInUser is now: " + lastLoggedInUser);
        }
        this.user = user;
    }


    /**
     * Used to handle the signout event following the revoke access event. Static class recommended by inspector to avoid memory issues.
     */
    private static class SignOutResultCallback implements ResultCallback<Status> {
        private final LogoutResultCallback callback;

        SignOutResultCallback(LogoutResultCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onResult(@NonNull Status r) {
            callback.onSignOut(r);
        }
    }

}
