package org.fraunhofer.cese.funf_sensor.backend;

/**
 * API Keys, Client Ids and Audience Ids for accessing APIs and configuring
 * Cloud Endpoints.
 * When you deploy your solution, you need to use your own API Keys and IDs.
 * Please refer to the documentation for this sample for more details.
 */
public final class Constants {

    /**
     * Google Cloud Messaging API key.
     */
    public static final String GCM_API_KEY = "funfcese";

    /**
     * Android client ID from Google Cloud console.
     */
    public static final String ANDROID_CLIENT_ID = "funfcese";


    /**
     * Web client ID from Google Cloud console.
     */
    public static final String WEB_CLIENT_ID = "funfcese";

    /**
     * Audience ID used to limit access to some client to the API.
     */
    public static final String AUDIENCE_ID = WEB_CLIENT_ID;

    /**
     * API package name.
     */
    public static final String API_OWNER =
            "backend.funf-sensor.cese.fraunhofer.org";

    /**
     * API package path.
     */
    public static final String API_PACKAGE_PATH = "org.fraunhofer.cese.funf_sensor.backend.apis";

    /**
     * Default constructor, never called.
     */
    private Constants() { }
}