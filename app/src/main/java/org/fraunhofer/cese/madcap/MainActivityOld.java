package org.fraunhofer.cese.madcap;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.iid.InstanceID;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.fraunhofer.cese.madcap.Probe.AccelerometerProbe;
import org.fraunhofer.cese.madcap.Probe.ActivityProbe.ActivityProbe;
import org.fraunhofer.cese.madcap.Probe.AudioProbe;
import org.fraunhofer.cese.madcap.Probe.BluetoothProbe;
import org.fraunhofer.cese.madcap.Probe.CallStateProbe;
import org.fraunhofer.cese.madcap.Probe.ForegroundProbe;
import org.fraunhofer.cese.madcap.Probe.NetworkConnectionProbe;
import org.fraunhofer.cese.madcap.Probe.PowerProbe;
import org.fraunhofer.cese.madcap.Probe.RunningApplicationsProbe;
import org.fraunhofer.cese.madcap.Probe.SMSProbe;
import org.fraunhofer.cese.madcap.Probe.StateProbe;
import org.fraunhofer.cese.madcap.appengine.GoogleAppEnginePipeline;
import org.fraunhofer.cese.madcap.authentification.MadcapAuthEventHandler;
import org.fraunhofer.cese.madcap.authentification.MadcapAuthManager;
import org.fraunhofer.cese.madcap.cache.Cache;
import org.fraunhofer.cese.madcap.cache.RemoteUploadResult;
import org.fraunhofer.cese.madcap.cache.UploadStatusListener;
import org.fraunhofer.cese.madcap.services.DataCollectionService;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.probe.builtin.ScreenProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;

public class MainActivityOld extends Activity implements MadcapAuthEventHandler{
    private static final String TAG = "Fraunhofer." + MainActivityOld.class.getSimpleName();
    public static final String PIPELINE_NAME = "appengine";
    private static final String STATE_UPLOAD_STATUS = "uploadStatus";
    private static final String STATE_DATA_COUNT = "dataCount";
    private static final String STATE_COLLECTING_DATA = "isCollectingData";

    private static final long CACHE_UPDATE_UI_DELAY = 5000;
    private static final int MAX_EXCEPTION_MESSAGE_LENGTH = 20;

    private MadcapAuthManager madcapAuthManager;


    @Nullable
    private FunfManager funfManager;

    /**
     * Pipeline used to manage probes and store data.
     * The @Inject annotation on this field indicates that the instance should be injected.
     */
    @Inject
    GoogleAppEnginePipeline pipeline;

    //probes
    private ActivityProbe activityProbe;
    private AccelerometerProbe accelerometerProbe;
    private AudioProbe audioProbe;
    private BluetoothProbe bluetoothProbe;
    private CallStateProbe callStateProbe;
    private ForegroundProbe foregroundProbe;
    private NetworkConnectionProbe networkConnectionProbe;
    private PowerProbe powerProbe;
    private RunningApplicationsProbe runningApplicationsProbe;
    private ScreenProbe screenProbe;
    private SimpleLocationProbe locationProbe;
    private SMSProbe sMSProbe;
    private StateProbe stateProbe;


    // UI elements
    private TextView dataCountView;
    private TextView uploadResultView;
    private TextView usernameTextview;

    private UploadStatusListener uploadStatusListener;
    private AsyncTask<Void, Long, Void> cacheCountUpdater;

    private String uploadResultText;
    private String dataCountText;
    private boolean isCollectingData;

    private final ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            funfManager = ((FunfManager.LocalBinder) service).getManager();
            Gson gson = funfManager.getGson();

            activityProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
            //((MyApplication) getApplication()).getComponent().inject(activityProbe);

            accelerometerProbe = gson.fromJson(new JsonObject(), AccelerometerProbe.class);
            audioProbe = gson.fromJson(new JsonObject(), AudioProbe.class);

            bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
            ((MyApplication) getApplication()).getComponent().inject(bluetoothProbe);

            callStateProbe = gson.fromJson(new JsonObject(), CallStateProbe.class);
            foregroundProbe = gson.fromJson(new JsonObject(), ForegroundProbe.class);
            locationProbe = gson.fromJson(getString(R.string.probe_location), SimpleLocationProbe.class);
            networkConnectionProbe = gson.fromJson(new JsonObject(), NetworkConnectionProbe.class);
            powerProbe = gson.fromJson(new JsonObject(), PowerProbe.class);
            runningApplicationsProbe = gson.fromJson(new JsonObject(), RunningApplicationsProbe.class);
            screenProbe = gson.fromJson(new JsonObject(), ScreenProbe.class);
            sMSProbe = gson.fromJson(new JsonObject(), SMSProbe.class);
            stateProbe = gson.fromJson(new JsonObject(), StateProbe.class);

            // Initialize the pipeline
            funfManager.registerPipeline(PIPELINE_NAME, pipeline);
            pipeline = (GoogleAppEnginePipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

            if (isCollectingData)
                enablePipelines();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (funfManager != null && pipeline.isEnabled()) {
                disablePipelines();
            }
            funfManager = null;
        }
    };

    private void disablePipelines() {
        MyApplication.madcapLogger.d(TAG, "Disabling pipeline: " + PIPELINE_NAME);

        activityProbe.unregisterPassiveListener(pipeline);
        accelerometerProbe.unregisterPassiveListener(pipeline);
        audioProbe.unregisterPassiveListener(pipeline);
        bluetoothProbe.unregisterListener(pipeline);
        callStateProbe.unregisterPassiveListener(pipeline);
        foregroundProbe.unregisterPassiveListener(pipeline);
        locationProbe.unregisterPassiveListener(pipeline);
        networkConnectionProbe.unregisterPassiveListener(pipeline);
        powerProbe.unregisterPassiveListener(pipeline);
        runningApplicationsProbe.unregisterPassiveListener(pipeline);
        screenProbe.unregisterPassiveListener(pipeline);
        sMSProbe.unregisterPassiveListener(pipeline);
        stateProbe.unregisterPassiveListener(pipeline);


        pipeline.setEnabled(false);
        assert funfManager != null;
        funfManager.disablePipeline(PIPELINE_NAME);
    }

    private void enablePipelines() {
        MyApplication.madcapLogger.i(TAG, "Enabling pipeline: " + PIPELINE_NAME);
        assert funfManager != null;
        funfManager.enablePipeline(PIPELINE_NAME);

        pipeline.setEnabled(true);

        activityProbe.registerPassiveListener(pipeline);
        accelerometerProbe.registerPassiveListener(pipeline);
        audioProbe.registerPassiveListener(pipeline);
        bluetoothProbe.registerListener(pipeline);
        callStateProbe.registerPassiveListener(pipeline);
        foregroundProbe.registerPassiveListener(pipeline);
        locationProbe.registerPassiveListener(pipeline);
        networkConnectionProbe.registerPassiveListener(pipeline);
        powerProbe.registerPassiveListener(pipeline);
        runningApplicationsProbe.registerPassiveListener(pipeline);
        screenProbe.registerPassiveListener(pipeline);
        sMSProbe.registerPassiveListener(pipeline);
        stateProbe.registerPassiveListener(pipeline);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Performs dependency injection
        //((MyApplication) getApplication()).getComponent().inject(this);

        //Manage the MadcapAuthManager
        madcapAuthManager = MadcapAuthManager.getInstance();
        madcapAuthManager.setCallbackClass(this);
        madcapAuthManager.connect();

        //MyApplication.madcapLogger.d(TAG, "Context of Auth Manager is "+MadcapAuthManager.getContext().toString());

        if (savedInstanceState != null) {
            dataCountText = savedInstanceState.getString(STATE_DATA_COUNT);
            uploadResultText = savedInstanceState.getString(STATE_UPLOAD_STATUS);
        } else {
            dataCountText = "Computing...";
            uploadResultText = "None.";
            isCollectingData = true;
        }

        //Set the toggle button on the last set preference configuration
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isCollectingData = prefs.getBoolean(getString(R.string.data_collection_pref), true);

        setContentView(R.layout.main_old);
        dataCountView = (TextView) findViewById(R.id.dataCountText);
        uploadResultView = (TextView) findViewById(R.id.uploadResult);
        usernameTextview = (TextView) findViewById(R.id.usernameTextview);
        Switch collectDataSwitch = (Switch) findViewById(R.id.switch1);

        if(madcapAuthManager.getLastSignedInUsersName() != null){
            usernameTextview.setText(madcapAuthManager.getLastSignedInUsersName());
        }


        ((TextView) findViewById(R.id.instanceIdText)).setText(getString(R.string.instanceIdText, InstanceID.getInstance(getApplicationContext()).getId()));

        collectDataSwitch.setChecked(isCollectingData);
        collectDataSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            isCollectingData = true;
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivityOld.this);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(getString(R.string.data_collection_pref), true);
                            editor.commit();
                            boolean currentCollectionState = prefs.getBoolean(getString(R.string.data_collection_pref), true);
                            MyApplication.madcapLogger.d(TAG, "Current data collection preference is now "+currentCollectionState);
                            Intent intent = new Intent(MainActivityOld.this, DataCollectionService.class);
                            startService(intent);
                            enablePipelines();
                        } else {
                            isCollectingData = false;
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivityOld.this);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(getString(R.string.data_collection_pref), false);
                            editor.commit();
                            boolean currentCollectionState = prefs.getBoolean(getString(R.string.data_collection_pref), true);
                            MyApplication.madcapLogger.d(TAG, "Current data collection preference is now "+currentCollectionState);
                            Intent intent = new Intent(MainActivityOld.this, DataCollectionService.class);
                            stopService(intent);
                            disablePipelines();
                        }
                    }
                }
        );

        final Button quitProjcetButton = (Button) findViewById(R.id.QuitButton);
        quitProjcetButton.setOnClickListener(new View.OnClickListener(){
            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                MyApplication.madcapLogger.d(TAG, "QUIT project clicked");

                String url = "https://www.pocket-security.org/quitting-pocket-security/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

            }
        });

        Button logoutButton = (Button) findViewById(R.id.SignOut);
        logoutButton.setOnClickListener(new View.OnClickListener(){
            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                MyApplication.madcapLogger.d(TAG, "Logout clicked");
                madcapAuthManager.signOut();

                Intent intent = new Intent(MainActivityOld.this, DataCollectionService.class);
                stopService(intent);

                goBackToSignIn();
            }
        });

        Button instantSendButton = (Button) findViewById(R.id.SendButton);
        instantSendButton.setOnClickListener(
                new View.OnClickListener() {
                    private final DateFormat df = DateFormat.getDateTimeInstance();

                    @Override
                    public void onClick(View view) {
                        String text = "\nUpload requested on " + df.format(new Date()) + "\n";

                        int status = pipeline.requestUpload();
                        if (status == Cache.UPLOAD_READY)
                            text += "Upload started...";
                        else if (status == Cache.UPLOAD_ALREADY_IN_PROGRESS)
                            text += "Upload in progress...";
                        else {
                            String errorText = "";
                            if ((status & Cache.INTERNAL_ERROR) == Cache.INTERNAL_ERROR)
                                errorText += "\n- An internal error occurred and data could not be uploaded.";
                            if ((status & Cache.UPLOAD_INTERVAL_NOT_MET) == Cache.UPLOAD_INTERVAL_NOT_MET)
                                errorText += "\n- An upload was just requested; please wait a few seconds.";
                            if ((status & Cache.NO_INTERNET_CONNECTION) == Cache.NO_INTERNET_CONNECTION)
                                errorText += "\n- No WiFi connection detected.";
                            if ((status & Cache.DATABASE_LIMIT_NOT_MET) == Cache.DATABASE_LIMIT_NOT_MET)
                                errorText += "\n- No entries to upload";

                            text += !errorText.isEmpty() ? "Error:" + errorText : "No status to report. Please wait.";
                        }
                        uploadResultText = text;
                        uploadResultView.setText(getString(R.string.uploadResultText, uploadResultText));
                    }
                }
        );

        // Bind to the service, to create the connection with FunfManager+
        MyApplication.madcapLogger.d(TAG, "Starting FunfManager");
        startService(new Intent(this, FunfManager.class));
        MyApplication.madcapLogger.d(TAG, "Binding Funf ServiceConnection to activity");
        getApplicationContext().bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
        pipeline.addUploadListener(getUploadStatusListener());
        getCacheCountUpdater().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onStart() {
        super.onStart();
        dataCountView.setText(getString(R.string.dataCountText, dataCountText));
        uploadResultView.setText(getString(R.string.uploadResultText, uploadResultText));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        pipeline.removeUploadListener(getUploadStatusListener());
        AsyncTask.Status status = getCacheCountUpdater().getStatus();
        if (!getCacheCountUpdater().isCancelled() && (status == AsyncTask.Status.PENDING || status == AsyncTask.Status.RUNNING)) {
            getCacheCountUpdater().cancel(true);
        }

        boolean isBound = getApplicationContext().bindService(new Intent(getApplicationContext(), FunfManager.class), funfManagerConn, Context.BIND_AUTO_CREATE);
        if (isBound)
            getApplicationContext().unbindService(funfManagerConn);
        stopService(new Intent(this, FunfManager.class));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_UPLOAD_STATUS, uploadResultText);
        outState.putString(STATE_DATA_COUNT, dataCountText);
        outState.putBoolean(STATE_COLLECTING_DATA, isCollectingData);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        pipeline.onTrimMemory();
    }

    private AsyncTask<Void, Long, Void> getCacheCountUpdater() {
        if (cacheCountUpdater == null) {
            cacheCountUpdater = new AsyncTask<Void, Long, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    while (!isCancelled()) {
                        try {
                            if (pipeline != null)
                                publishProgress(pipeline.getCacheSize());
                            Thread.sleep(CACHE_UPDATE_UI_DELAY);
                        } catch (InterruptedException e) {
                            MyApplication.madcapLogger.i("Fraunhofer.CacheCounter", "Cache counter task to update UI thread has been interrupted.");
                        }
                    }
                    return null;
                }

                @Override
                protected void onProgressUpdate(Long... values) {
                    updateDataCount(values[0]);
                }
            };
        }
        return cacheCountUpdater;
    }

    private UploadStatusListener getUploadStatusListener() {
        if (uploadStatusListener == null) {

            uploadStatusListener = new UploadStatusListener() {
                private static final String TAG = "UploadStatusListener";
                private static final String pre = "\nLast upload attempt: ";
                private final DateFormat df = DateFormat.getDateTimeInstance();

                @Override
                public void uploadFinished(RemoteUploadResult result) {
                    // handle the various options
                    if (uploadResultView == null)
                        return;

                    String text = pre + df.format(new Date()) + "\n";
                    if (result == null) {
                        text += "Result: No upload due to an internal error.";
                    } else if (!result.isUploadAttempted()) {
                        text += "Result: No entries to upload.";
                    } else if (result.getException() != null) {
                        String exceptionMessage;
                        if (result.getException().getMessage() != null)
                            exceptionMessage = result.getException().getMessage();
                        else if (result.getException().toString() != null)
                            exceptionMessage = result.getException().toString();
                        else
                            exceptionMessage = "Unspecified error";

                        text += "Result: Upload failed due to " + (exceptionMessage.length() > MAX_EXCEPTION_MESSAGE_LENGTH ? exceptionMessage.substring(0, MAX_EXCEPTION_MESSAGE_LENGTH - 1) : exceptionMessage);
                    } else if (result.getSaveResult() == null) {
                        text += "Result: An error occurred on the remote server.";
                    } else {
                        text += "Result:\n";
                        text += "\t" + (result.getSaveResult().getSaved() == null ? 0 : result.getSaveResult().getSaved().size()) + " entries saved.";
                        if (result.getSaveResult().getAlreadyExists() != null)
                            text += "\n\t" + result.getSaveResult().getAlreadyExists().size() + " duplicate entries ignored.";
                    }

                    uploadResultText = text;
                    if (uploadResultView.isShown())
                        uploadResultView.setText(getString(R.string.uploadResultText, uploadResultText));
                    if (pipeline != null && pipeline.isEnabled())
                        updateDataCount(-1);
                    MyApplication.madcapLogger.d(TAG, "Upload result received");
                }

                private final Pattern pattern = Pattern.compile("[0-9]+% completed.");

                @Override
                public void progressUpdate(int value) {
                    Matcher matcher = pattern.matcher(uploadResultText);
                    if (matcher.find()) {
                        uploadResultText = matcher.replaceFirst(value + "% completed.");
                    } else {
                        uploadResultText += " " + value + "% completed.";
                    }

                    if (uploadResultView.isShown())
                        uploadResultView.setText(getString(R.string.uploadResultText, uploadResultText));
                }

                @Override
                public void cacheClosing() {
                    MyApplication.madcapLogger.d(TAG, "Cache is closing");
                }
            };
        }
        return uploadStatusListener;
    }

    private void goBackToSignIn(){
        MyApplication.madcapLogger.d(TAG, "Now going back to SignInActivity");
        Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtra("distractfromsilentlogin", true);
        startActivity(intent);
        finish();
    }

    private void updateDataCount(long count) {
        dataCountText = count < 0 ? "Computing..." : Long.toString(count);

        if (dataCountView != null && dataCountView.isShown()) {
            dataCountView.setText(getString(R.string.dataCountText, dataCountText));
        }
    }


    /**
     * Specifies what the class is expected to do, when the silent login was sucessfull.
     *
     * @param result
     */
    @Override
    public void onSilentLoginSuccessfull(GoogleSignInResult result) {
        if(madcapAuthManager.getLastSignedInUsersName() != null){
            usernameTextview.setText(madcapAuthManager.getLastSignedInUsersName());
        }
    }

    /**
     * Specifies what the class is expected to do, when the silent login was not successfull.
     *
     * @param opr
     */
    @Override
    public void onSilentLoginFailed(OptionalPendingResult<GoogleSignInResult> opr) {

    }

    /**
     * Specifies what the class is expected to do, when the regular sign in was successful.
     */
    @Override
    public void onSignInSucessfull() {
        if(madcapAuthManager.getLastSignedInUsersName() != null){
            usernameTextview.setText(madcapAuthManager.getLastSignedInUsersName());
        }
    }

    /**
     * Specifies what the app is expected to do when the Signout was sucessfull.
     *
     * @param status
     */
    @Override
    public void onSignOutResults(Status status) {
        //Exit Application
        /**
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
         */

        goBackToSignIn();
    }

    /**
     * Specifies what the class is expected to do, when disconnected.
     *
     * @param status
     */
    @Override
    public void onRevokeAccess(Status status) {

    }

    @Override
    public void onSignInIntent(Intent intent, int requestCode) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}