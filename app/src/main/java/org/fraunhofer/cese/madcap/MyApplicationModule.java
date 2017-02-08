package org.fraunhofer.cese.madcap;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceApi;
import com.google.android.gms.awareness.SnapshotApi;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import org.fraunhofer.cese.madcap.authentication.AuthenticationProvider;
import org.fraunhofer.cese.madcap.backend.probeEndpoint.ProbeEndpoint;
import org.fraunhofer.cese.madcap.cache.CacheConfig;
import org.fraunhofer.cese.madcap.cache.RemoteUploadAsyncTaskFactory;
import org.fraunhofer.cese.madcap.issuehandling.GoogleApiClientConnectionIssueManagerLocation;
import org.fraunhofer.cese.madcap.issuehandling.MadcapPermissionDeniedHandler;
import org.fraunhofer.cese.madcap.issuehandling.MadcapSensorNoAnswerReceivedHandler;
import org.fraunhofer.cese.madcap.util.EndpointApiBuilder;
import org.fraunhofer.cese.madcap.util.ManualProbeUploader;

import java.util.Calendar;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import edu.umd.fcmd.sensorlisteners.listener.IntentFilterFactory;
import edu.umd.fcmd.sensorlisteners.listener.activity.TimedActivityTaskFactory;
import edu.umd.fcmd.sensorlisteners.listener.applications.TimedApplicationTaskFactory;
import edu.umd.fcmd.sensorlisteners.listener.audio.AudioReceiverFactory;
import edu.umd.fcmd.sensorlisteners.listener.bluetooth.BluetoothInformationReceiverFactory;
import edu.umd.fcmd.sensorlisteners.listener.location.LocationServiceStatusReceiverFactory;
import edu.umd.fcmd.sensorlisteners.listener.location.TimedLocationTaskFactory;
import edu.umd.fcmd.sensorlisteners.listener.network.ConnectionInfoReceiverFactory;
import edu.umd.fcmd.sensorlisteners.listener.network.MMSOutObserverFactory;
import edu.umd.fcmd.sensorlisteners.listener.network.MSMSReceiverFactory;
import edu.umd.fcmd.sensorlisteners.listener.network.SMSOutObserverFactory;
import edu.umd.fcmd.sensorlisteners.listener.network.TelephonyListenerFactory;
import edu.umd.fcmd.sensorlisteners.listener.system.SystemReceiverFactory;

/**
 * This class defines the providers to use for dependency injection
 */
@SuppressWarnings({"SameReturnValue", "InstanceMethodNamingConvention", "MethodMayBeStatic"})
@Module
class MyApplicationModule {


    /**
     * Local instance of the Application. Needed to provide the {@link Context} to injected classes.
     */
    private final Application application;

    /**
     * Constructor used to initiate the module with application-specific context
     *
     * @param application the calling application
     */
    MyApplicationModule(Application application) {
        this.application = application;
    }

    /**
     * Needed by the {@link org.fraunhofer.cese.madcap.cache.Cache}
     *
     * @return a Context object (probably the Application) to be used
     */
    @Provides
    Context provideContext() {
        return application;
    }

    @Provides
    @Named("AwarenessApi")
    GoogleApiClient provideGoogleApiClient() {
        return new GoogleApiClient.Builder(application)
                .addApi(Awareness.API)
                .build();
    }

    @Provides
    Calendar provideCalendar() {
        return Calendar.getInstance();
    }


    /**
     * Needed by the DataCollectionService.
     *
     * @return a static FenceApi
     */
    @Provides
    FenceApi provideFenceApi() {
        return Awareness.FenceApi;
    }

    /**
     * Needed by the DataCollectionService.
     *
     * @return a statuc SnapshotApi
     */
    @Provides
    SnapshotApi provideSnapshotApi() {
        return Awareness.SnapshotApi;
    }

    /**
     * Needed by the DataCollectionService.
     *
     * @return a factory.
     */
    @Provides
    TimedLocationTaskFactory provideTimedLocationTaskFactory() {
        return new TimedLocationTaskFactory();
    }

    /**
     * Needed by the DataCollectionService.
     *
     * @return a factory.
     */
    @Provides
    TimedApplicationTaskFactory provideTimedApplicationTask() {
        return new TimedApplicationTaskFactory();
    }

    /**
     * Needed by the DataCollectionService.
     *
     * @return a factory.
     */
    @Provides
    LocationServiceStatusReceiverFactory provideLocationServiceStatusReceiverFactory() {
        return new LocationServiceStatusReceiverFactory();
    }


    /**
     * Needed by the DataCollectionService.
     *
     * @return an issuemanager.
     */
    @Provides
    GoogleApiClientConnectionIssueManagerLocation provideGoogleConnectionIssueManager() {
        return new GoogleApiClientConnectionIssueManagerLocation();
    }

    /**
     * Needed by the DataCollectionService.
     *
     * @return an MadcapPermissionDeniedHandler.
     */
    @Provides
    MadcapPermissionDeniedHandler provideMadcapPermissionDeniedHandler() {
        return new MadcapPermissionDeniedHandler(application);
    }

    @Provides
    MadcapSensorNoAnswerReceivedHandler provideSensorNoAnswerReceivedHandler() {
        return new MadcapSensorNoAnswerReceivedHandler();
    }

    @Provides
    TelephonyListenerFactory provideTelephonyListenerFactory() {
        return new TelephonyListenerFactory();
    }

    @Provides
    ConnectionInfoReceiverFactory provideConnectionInfoReceiverFactory() {
        return new ConnectionInfoReceiverFactory();
    }

    @Provides
    MSMSReceiverFactory provideMSMSReceiverFactory() {
        return new MSMSReceiverFactory();
    }

    @Provides
    SMSOutObserverFactory provideSMSOutObserverFactory() {
        return new SMSOutObserverFactory();
    }

    @Provides
    MMSOutObserverFactory provideMMSOutObserverFactory() {
        return new MMSOutObserverFactory();
    }

    @Provides
    IntentFilterFactory provideIntentFilterFactory() {
        return new IntentFilterFactory();
    }

    @Provides
    TimedActivityTaskFactory provideTimedActivityTaskFactory() {
        return new TimedActivityTaskFactory();
    }

    @Provides
    ManualProbeUploader provideManualProbeUploader() {
        return new ManualProbeUploader();
    }

    @Provides
    SystemReceiverFactory provideSystemReceiverFactory() {
        return new SystemReceiverFactory();
    }

    @Provides
    AudioReceiverFactory provideAudioReceiverFactory() {
        return new AudioReceiverFactory();
    }


    /**
     * Needed by the {@link org.fraunhofer.cese.madcap.cache.Cache}
     *
     * @return the ConnectivityManager to use
     */
    @Provides
    @Singleton
    final ConnectivityManager provideConnectivityManager() {
        return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }


    /**
     * Needed by the {@link org.fraunhofer.cese.madcap.cache.Cache}
     *
     * @return the CacheConfig to use
     */
    @Provides
    CacheConfig provideCacheConfig() {
        CacheConfig config = new CacheConfig();
        config.setMaxMemEntries(100);
        config.setMaxDbEntries(1000);

        config.setMemForcedCleanupLimit(5000);
        config.setDbForcedCleanupLimit(30000); // value must ensure that we do not exceed Google API limits for a single request

        config.setDbWriteInterval(2000);
        config.setUploadInterval(900000);

        config.setUploadWifiOnly(false);

        return config;
    }

//    /**
//     * Needed by the {@link org.fraunhofer.cese.madcap.cache.Cache} and the {@link RemoteUploadAsyncTaskFactory}
//     *
//     * @return the ProbeEndpoint to use
//     */
//    @Provides
//    static ProbeEndpoint provideProbeDataSetApi() {
//        String endpointUrl = "https://madcap-dev1.appspot.com/_ah/api/";
////      String endpointUrl = "https://madcap-142815.appspot.com/_ah/api/";
//        ProbeEndpoint.Builder builder = new ProbeEndpoint.Builder(AndroidHttp.newCompatibleTransport(),
//                new AndroidJsonFactory(), null)
//                .setApplicationName("Fraunhofer MADCAP")
//                .setRootUrl(endpointUrl);
//
//        return builder.build();
//    }

    @Nullable
    @Provides
    BluetoothAdapter provideBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    @Provides
    BluetoothInformationReceiverFactory provideBluetoothInformationReceiverFactory() {
        return new BluetoothInformationReceiverFactory();
    }

    @Provides
    @Named("SigninApi")
    GoogleApiClient providesGoogleSigninApiClient() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();

        return new GoogleApiClient.Builder(application)
                //.enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build();
    }

    @Provides
    GoogleApiAvailability providesGoogleApiAvailability() {
        return GoogleApiAvailability.getInstance();
    }
}

