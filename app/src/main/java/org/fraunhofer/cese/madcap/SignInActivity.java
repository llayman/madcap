package org.fraunhofer.cese.madcap;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.fraunhofer.cese.madcap.authentification.MadcapAuthEventHandler;
import org.fraunhofer.cese.madcap.authentification.MadcapAuthManager;

import static com.pathsense.locationengine.lib.detectionLogic.b.o;

/**
 * Activity to demonstrate basic retrieval of the Google user's ID, email address, and basic
 * profile.
 */
public class SignInActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, MadcapAuthEventHandler {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;


    //private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signinactivity);


        MadcapAuthManager.setCallbackClass(this);

        Log.d(TAG, "CREATED");
        //Log.d(TAG, "Context of Auth Manager is "+MadcapAuthManager.getContext());

        // Views
        mStatusTextView = (TextView) findViewById(R.id.status);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);


        // [END build_client]

        // [START customize_button]
        // Customize sign-in button. The sign-in button can be displayed in
        // multiple sizes and color schemes. It can also be contextually
        // rendered based on the requested scopes. For example. a red button may
        // be displayed when Google+ scopes are requested, but a white button
        // may be displayed when only basic profile is requested. Try adding the
        // Scopes.PLUS_LOGIN scope to the GoogleSignInOptions to see the
        // difference.
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(MadcapAuthManager.getGsoScopeArray());
        // [END customize_button]
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "On start being called, now trying to silently log in");
        // Try the silent login. After that callbacks are called.
        //MadcapAuthManager.silentLogin();
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    public void handleSignInResult(GoogleSignInResult result) {
        //From the result we can retrieve some credentials

        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            MadcapAuthManager.handleSignInResult(result);
            updateUI(true);
            proceedToMainActivity();
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    /**
     * Starts the transition to the main activity
     */
    public void proceedToMainActivity(){
        Log.d(TAG, "Now going to the MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        //intent.putExtra("Madcap Auth Manager", madcapAuthManager);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                Log.d(TAG, "pressed sign in");
                MadcapAuthManager.signIn();
                break;
            case R.id.sign_out_button:
                Log.d(TAG, "pressed sign ou");
                MadcapAuthManager.signOut();
                break;
            case R.id.disconnect_button:
                Log.d(TAG, "pressed disconnect");
                MadcapAuthManager.revokeAccess();
                break;
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mProgressDialog.dismiss();
    }

    /**
     * Specifies what the class is expected to do, when the silent login was sucessfull.
     */
    @Override
    public void onSilentLoginSuccessfull(GoogleSignInResult result) {
        // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
        // and the GoogleSignInResult will be available instantly.
        Log.d(TAG, "Got cached sign-in");
        handleSignInResult(result);
    }

    /**
     * Specifies what the clas is expected to do, when the silent login was not successfull.
     */
    @Override
    public void onSilentLoginFailed(OptionalPendingResult<GoogleSignInResult> opr) {
        // If the user has not previously signed in on this device or the sign-in has expired,
        // this asynchronous branch will attempt to sign in the user silently.  Cross-device
        // single sign-on will occur in this branch.
        showProgressDialog();
        opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
            @Override
            public void onResult(GoogleSignInResult googleSignInResult) {
                hideProgressDialog();
                handleSignInResult(googleSignInResult);
            }
        });
    }

    /**
     * Specifies what the class is expected to do, when the regular sign in was successful.
     */
    @Override
    public void onSignInSucessfull() {
        Log.d(TAG, "onSignInSuccessfull");
        proceedToMainActivity();
    }

    @Override
    public void onSignInIntent(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
        Log.d(TAG,"Now starting the signing procedure with intnet");
    }

    /**
     * Specifies what the app is expected to do when the Signout was sucessfull.
     */
    @Override
    public void onSignOutResults(Status status) {
        updateUI(false);
    }

    /**
     * Specifies what the class is expected to do, when disconnected.
     * @param status
     */
    @Override
    public void onRevokeAccess(Status status) {
        updateUI(false);
    }


}
