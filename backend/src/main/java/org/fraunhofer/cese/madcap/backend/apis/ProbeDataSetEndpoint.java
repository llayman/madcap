package org.fraunhofer.cese.madcap.backend.apis;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.appengine.api.oauth.OAuthRequestException;

import org.fraunhofer.cese.madcap.backend.models.ProbeDataSet;
import org.fraunhofer.cese.madcap.backend.models.ProbeEntry;
import org.fraunhofer.cese.madcap.backend.models.ProbeSaveResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import static org.fraunhofer.cese.madcap.backend.OfyService.ofy;

/**
 * An endpoint class we are exposing
 */
@SuppressWarnings("ResourceParameter")
@Api(
        name = "probeEndpoint",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "madcap.cese.fraunhofer.org",
                ownerName = "madcap.cese.fraunhofer.org",
                packagePath="backend"
        )
)
public class ProbeDataSetEndpoint {

    private static final Logger logger = Logger.getLogger(ProbeDataSetEndpoint.class.getName());

    /**
     * This inserts a new <code>ProbeDataSet</code> object.
     *
     * @param probeDataSet The object to be added.
     * @return The object to be added.
     */
    @ApiMethod(
            name = "insertProbeDataset"
    )
    public ProbeSaveResult insertSensorDataSet(HttpServletRequest req, ProbeDataSet probeDataSet) throws OAuthRequestException, ConflictException, BadRequestException {

        long startTime = System.currentTimeMillis();
        logger.info("Upload request received from " + req.getRemoteAddr());
        if (probeDataSet == null) {
            throw new BadRequestException("sensorDataSet cannot be null");
        }

        Collection<ProbeEntry> entryList = probeDataSet.getEntryList();
        if (entryList == null || entryList.isEmpty()) {
            throw new BadRequestException("entryList is null or empty");
        }

        logger.fine("Logging request received. Data: " + entryList);
        logger.info("Number of entries received: " + entryList.size() + ", Request size: " + humanReadableByteCount(Long.parseLong(req.getHeader("Content-Length")), false));

        Collection<String> saved = new ArrayList<>();
        Collection<String> alreadyExists = new ArrayList<>();

        Collection<ProbeEntry> toSave = new ArrayList<>();

        Collection<String> uploadedIds = new ArrayList<>();
        for (ProbeEntry entry : entryList) {
            uploadedIds.add(entry.getId());
        }

        Map<String, ProbeEntry> ids = ofy().load().type(ProbeEntry.class).ids(uploadedIds);
        for (ProbeEntry entry : entryList) {
            if (ids.get(entry.getId()) == null) {
                saved.add(entry.getId());
                toSave.add(entry);
            } else {
                alreadyExists.add(entry.getId());
            }
        }
        ofy().save().entities(toSave).now();
        ofy().clear();

        logger.info("Num Saved: " + saved.size() + ", Num already existing: " + alreadyExists.size() + ", Time taken: " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        return ProbeSaveResult.create(saved, alreadyExists);
    }


    /**
     * Displays raw byte counts (e.g., 1024) in human readable format (e.g., 1.0 KiB).
     * <p/>
     * From <a href="http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java">http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java</a>
     *
     * @param bytes size in bytes
     * @param si    use si units or not
     * @return a human readable string of the byte size
     */
    @SuppressWarnings("NonReproducibleMathCall")
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.ENGLISH,"%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}