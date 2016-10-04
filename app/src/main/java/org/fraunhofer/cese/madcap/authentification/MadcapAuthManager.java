package org.fraunhofer.cese.madcap.authentification;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import java.io.Serializable;

import static com.pathsense.locationengine.lib.detectionLogic.b.p;
import static org.fraunhofer.cese.madcap.R.id.status;

/**
 * Created by MMueller on 9/29/2016.
 * @Singleton due to Google requirments
 */
public class MadcapAuthManager implements GoogleApiClient.OnConnectionFailedListener, Serializable{
    private static MadcapAuthManager instance = null;

    private static final String TAG = "MADCAP Auth Manager";
    private static final int RC_SIGN_IN = 9001;

    private static MadcapAuthEventHandler callbackClass;

    private static GoogleSignInOptions gso;
    private static GoogleApiClient mGoogleApiClient;
    private static GoogleSignInResult lastSignInResult;

    /**
     * Getter for the API Client.
     * @return Bond Api client.
     */
    public static GoogleApiClient getMGoogleApiClient() {
        return mGoogleApiClient;
    }

    /**
     * Getter for the Sign in Options
     * @return Bond Sign In Options.
     */
    public static GoogleSignInOptions getGso() {
        return gso;
    }

    /**
     * Getter for the callback class.
     * @return Callback class which needs to implement MadcapAuthEventHandler
     */
    public static MadcapAuthEventHandler getCallbackClass() {
        return callbackClass;
    }

    /**
     * Empty private Constructor making sure that it is a singleton.
     */
    private MadcapAuthManager(){
    }

    public static void setUp(GoogleSignInOptions gso, GoogleApiClient mGoogleApiClient){
        if(MadcapAuthManager.gso == null && MadcapAuthManager.mGoogleApiClient == null){
            MadcapAuthManager.gso = gso;
            MadcapAuthManager.mGoogleApiClient = mGoogleApiClient;
        }else{
            Log.e(TAG, "Static class already set up. Not possible to do it twice");
        }

    }

    /**
     * Sets the callback class. Needed every time, when the interacting
     * class changes.
     * @param callbackClass
     */
    public static void setCallbackClass(MadcapAuthEventHandler callbackClass){
        MadcapAuthManager.callbackClass = callbackClass;
    }

    /**
     * Performs a silent login with cached credentials
     */
    public static void silentLogin(){
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        Log.d(TAG, "First silent sign in result: "+opr.isDone());

        if (opr.isDone()) {
            // In Case there is a result available intermediately.
            Log.d(TAG, "Immediate result available ");
            lastSignInResult = opr.get();
            callbackClass.onSilentLoginSuccessfull(lastSignInResult);
        } else {
            // In case no immediate result available.
            Log.d(TAG, "Immediate result NOT available ");
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult result) {
                    callbackClass.onSilentLoginSuccessfull(lastSignInResult);
                }
            });
        }
    }

    /**
     * Sometimes needed for getting the explicit instance of the singleton.
     * @return The singleton instance.
     */
    public static MadcapAuthManager getInstance(){
        if(instance == null){
            instance = new MadcapAuthManager();
        }
        return instance;
    }

    /**
     * Performs a regualr login with an intent.
     */
    public static void signIn(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        Log.d(TAG,signInIntent.toString());

        callbackClass.onSignInIntent(signInIntent, RC_SIGN_IN);

    }

    /**
     * Sign out from Google Account. Calls callbackClass.onSignOutResults.
     */
    public static void signOut(){
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.e(TAG, "Logout status "+status);
                        lastSignInResult = null;
                        //revokeAccess();
                        callbackClass.onSignOutResults(status);
                    }
                });
    }

    /**
     * Retrieves the currently logged in User Id.
     * @return User ID.
     */
    public static String getUserId(){
        return lastSignInResult.getSignInAccount().getId();
    }

    /**
     * Should be called to disconnect Google Account.
     */
    public static void revokeAccess(){
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                    callbackClass.onRevokeAccess(status);
                    }
                });

    }



    /**
     * Getter for Options
     * @return Scope Accaray
     */
    public static Scope[] getGsoScopeArray(){
        return gso.getScopeArray();
    }

    public static GoogleSignInAccount getSignInAccount(){
        return lastSignInResult.getSignInAccount();
    }

    /**
     * Makes the Signed in User accessable.
     * @return the last users name.
     */
    @Nullable
    public static String getLastSignedInUsersName(){

        if(lastSignInResult != null){
            String givenName = lastSignInResult.getSignInAccount().getGivenName();
            String lastName = lastSignInResult.getSignInAccount().getFamilyName();
            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(givenName);
            nameBuilder.append(" ");
            nameBuilder.append(lastName);
            return nameBuilder.toString();
        }else{
            Log.e(TAG, "No last user cached");
            return null;
        }
    }

    /**
     * Handles the sign in result, being called from the activity
     * which is implmenting AdcapAuthEventHandler
     * @param result
     */
    public static void handleSignInResult(GoogleSignInResult result){
        lastSignInResult = result;
    }

    /**
     * Connects the Google Api client.
     * Needs to be called whenever the Activity changes.
     */
    public static void connect(){
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection to Google Authenticatin failed");
    }
}
