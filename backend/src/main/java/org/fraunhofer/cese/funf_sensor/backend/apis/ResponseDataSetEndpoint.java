package org.fraunhofer.cese.funf_sensor.backend.apis;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.ListOptions;
import com.google.appengine.tools.cloudstorage.RetryParams;


import org.fraunhofer.cese.funf_sensor.backend.models.ProbeEntry;
import org.fraunhofer.cese.funf_sensor.backend.models.ResponseDataSet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.fraunhofer.cese.funf_sensor.backend.OfyService.ofy;

/**
 * Created by FDietrich on 10/23/2015.
 */
@Api(
        name = "responseDataSetApi",
        version = "v1",
        resource = "responseDataSet",
        namespace = @ApiNamespace(
                ownerDomain = "models.backend.funf_sensor.cese.fraunhofer.org",
                ownerName = "models.backend.funf_sensor.cese.fraunhofer.org",
                packagePath = ""
        )
)

public class ResponseDataSetEndpoint {

    private static final Logger logger = Logger.getLogger(ResponseDataSetEndpoint.class.getName());
    private static List<String> keys = new ArrayList<String>();


    @ApiMethod(name = "getResponseFromTo")
    public void getResponseFromTo(@Named("fromTimestamp") long fromTimestamp, @Named("toTimestamp") long toTimestamp) {

        dumpToGoogleCloudStorage();


//        if (fromTimestamp == 0 && toTimestamp == 0)
//            return getLatestEntries();
//        else
//            return getSpecificEntries(fromTimestamp, toTimestamp);
    }

    private static final String BUCKETNAME = "c3s3d4t4dump";
    private static final String FILENAME = "testfile";

    @ApiMethod(name = "flushData")
    private void dumpToGoogleCloudStorage() {
        GcsService gcsService = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
        GcsFilename filename = new GcsFilename(BUCKETNAME, FILENAME);
        logger.fine(gcsService.toString());
//        gcsService.list(BUCKETNAME, ListOptions.DEFAULT);
        try {
            GcsOutputChannel outputChannel = gcsService.createOrReplace(filename, GcsFileOptions.getDefaultInstance());
            List<ProbeEntry> entries = ofy().load().type(ProbeEntry.class).limit(1000).list();
            ObjectOutputStream oout =
                    new ObjectOutputStream(Channels.newOutputStream(outputChannel));
            oout.writeObject(entries);
            oout.close();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }

    }


    public static ResponseDataSet getLatestEntries() {

        ResponseDataSet responseDataSet = null;

        if (!keys.isEmpty()) {
            //Getting Ids for actual chunk of entries
            List<String> keysForChunk;
            if (keys.size() > ResponseDataSet.getCHUNK_SIZE())
                keysForChunk = keys.subList(0, ResponseDataSet.getCHUNK_SIZE()); // (inclusive, exclusive)
            else
                keysForChunk = keys.subList(0, keys.size());

            //Getting the chunk via Objectify
            Map<String, ProbeEntry> chunk =
                    ofy().load().type(ProbeEntry.class).ids(keysForChunk);
            keys.removeAll(keysForChunk);

            //Creating the Response
            responseDataSet = new ResponseDataSet();
            responseDataSet.setTimestamp(System.currentTimeMillis());
            responseDataSet.setNumberOfEntries(chunk.size());
            responseDataSet.setRemainingEntries(keys.size());
            responseDataSet.setContent(new ArrayList<>(chunk.values()));

        }
        return responseDataSet;
    }

    public static ResponseDataSet getSpecificEntries(long fromTimestamp, long toTimestamp) {

        ResponseDataSet responseDataSet;

        List<ProbeEntry> entries = ofy().load().type(ProbeEntry.class).filter("timestamp >=", fromTimestamp).list();
        entries.removeAll(ofy().load().type(ProbeEntry.class).filter("timestamp >", toTimestamp).list());
        Collections.sort(entries);


        responseDataSet = new ResponseDataSet();

        // if there are more than CHUNK_SIZE entries to download, download the first 1000 and also return the
        // timestamp of the last entry. the client will then start another request from this timestamp on.
        if (entries.size() > ResponseDataSet.getCHUNK_SIZE()) {
            responseDataSet.setTimestamp(System.currentTimeMillis());
            responseDataSet.setNumberOfEntries(ResponseDataSet.getCHUNK_SIZE());
            responseDataSet.setRemainingEntries(entries.size() - ResponseDataSet.getCHUNK_SIZE());
            responseDataSet.setContent(entries.subList(0, ResponseDataSet.getCHUNK_SIZE()));
            responseDataSet.setTimestampOfLastEntry(responseDataSet.getContent().get(ResponseDataSet.getCHUNK_SIZE()-1).getTimestamp());

        } else {
            responseDataSet.setTimestamp(System.currentTimeMillis());
            responseDataSet.setNumberOfEntries(entries.size());
            responseDataSet.setRemainingEntries(0);
            responseDataSet.setContent(entries);
            responseDataSet.setTimestampOfLastEntry(entries.get(entries.size()-1).getTimestamp());
        }


        return responseDataSet;
    }

    public static void addToKeys(String newProbeEntryID) {
        keys.add(newProbeEntryID);
    }

}