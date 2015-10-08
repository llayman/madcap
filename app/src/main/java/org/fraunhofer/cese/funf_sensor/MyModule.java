package org.fraunhofer.cese.funf_sensor;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.fraunhofer.cese.funf_sensor.appengine.GoogleAppEnginePipeline;
import org.fraunhofer.cese.funf_sensor.backend.models.probeDataSetApi.ProbeDataSetApi;
import org.fraunhofer.cese.funf_sensor.cache.Cache;
import org.fraunhofer.cese.funf_sensor.cache.CacheConfig;
import org.fraunhofer.cese.funf_sensor.cache.DatabaseAsyncTaskFactory;
import org.fraunhofer.cese.funf_sensor.cache.RemoteUploadAsyncTaskFactory;

import java.io.IOException;

/**
 * Created by Lucas on 10/6/2015.
 */
public class MyModule extends AbstractModule {

    @Override
    protected void configure() {
        binder.bind(Cache.class);
        binder.bind(GoogleAppEnginePipeline.class);
    }

    @Provides
    CacheConfig provideCacheConfig() {
        CacheConfig config = new CacheConfig();
        config.setMaxMemEntries(500);
        config.setMaxDbEntries(1000);

        config.setMemForcedCleanupLimit(5000);
        config.setDbForcedCleanupLimit(150000);

        config.setDbWriteInterval(2000);
        config.setUploadInterval(5000);

        config.setUploadWifiOnly(true);

        return config;
    }

    @Provides
    DatabaseAsyncTaskFactory provideDatabaseWriteAsyncTaskFactory() {
        return new DatabaseAsyncTaskFactory();
    }

    @Provides
    RemoteUploadAsyncTaskFactory provideRemoteUploadAsyncTaskFactory() {
        return new RemoteUploadAsyncTaskFactory();
    }

    @Provides
    ProbeDataSetApi provideProbeDataSetApi() {
        ProbeDataSetApi.Builder builder = new ProbeDataSetApi.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null)
                .setApplicationName("funfSensor")
                .setRootUrl("http://192.168.0.100:8080/_ah/api/")
                .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                    @Override
                    public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                        abstractGoogleClientRequest.setDisableGZipContent(true);
                    }
                });
        return builder.build();
    }
}
