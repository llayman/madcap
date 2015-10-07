package org.fraunhofer.cese.funf_sensor.cache;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Factory for creating aysnchronous database related tasks.
 *
 * @author Lucas
 * @see Cache
 */
public class DatabaseAsyncTaskFactory {

    /**
     * Create an asychronous task which supports asynchronous writing of cache entries to a persistent SQLite database.
     * This class is used when cache entries stored in memory should be persisted. Results of the database writing actions
     * are stored in a DatabaseWriteResult.
     * <p/>
     * This task supports partial saves, i.e., the task will attempt to save each entry. If it encounters an exception or error,
     * it will abort but any previously saved entries will be persisted in the database.
     *
     * @param cache the cache object handling the request. Needed for callbacks on write completion.
     * @return a new instance of an asynchronous database writing task
     * @see org.fraunhofer.cese.funf_sensor.cache.DatabaseWriteResult
     */
    AsyncTask<Map<String, CacheEntry>, Void, DatabaseWriteResult> createWriteTask(final Cache cache) {

        return new AsyncTask<Map<String, CacheEntry>, Void, DatabaseWriteResult>() {
            private final String TAG = "Fraunhofer.DBWrite";

            @Override
            @SafeVarargs
            public final DatabaseWriteResult doInBackground(Map<String, CacheEntry>... memcaches) {
                DatabaseWriteResult result = DatabaseWriteResult.create();

                // Check preconditions for full or partial write of entry objects to database
                if (cache == null) {
                    result.setError(new RuntimeException("{doInBackground} cache object is null!"));
                    return result;
                }

                if (cache.getHelper() == null) {
                    result.setError(new RuntimeException("{doInBackground} Attempting to write cache to database, but DatabaseOpenHelper is null. Returning empty result."));
                    return result;
                }

                Collection<String> savedEntries = new ArrayList<>();
                RuntimeExceptionDao<CacheEntry, String> dao;
                try {
                    dao = cache.getHelper().getDao();
                } catch (Exception e) {
                    result.setError(e);
                    return result;
                }

                if (dao == null) {
                    result.setError(new RuntimeException("{doInBackground} Attempting to write cache to database, but DatabaseOpenHelper.getDao() is null. Returning empty result."));
                    return result;
                }

                // Try to save objects to the database. Supports partial saves, i.e., only some objects are saved.
                try {
                    long oldDatabaseSize = dao.countOf();

                    if (memcaches != null) {
                        for (Map<String, CacheEntry> memcache : memcaches) {
                            if (memcache != null && !memcache.isEmpty()) {
                                for (CacheEntry entry : memcache.values()) {
                                    if (dao.create(entry) > 0) {
                                        savedEntries.add(entry.getId());
                                    } else {
                                        Log.d(TAG, "{doInBackground} Entry was not saved to the database: " + entry.toString());
                                    }
                                }
                            }
                        }

                        long newDatabaseSize = dao.countOf();
                        Log.d(TAG, "{doInBackground} db entries added: " + (newDatabaseSize - oldDatabaseSize) + ", total db entries: " + newDatabaseSize);
                    }
                } catch (Exception e) {
                    result.setError(e);
                } finally {
                    result.setDatabaseSize(dao.countOf());
                    result.setSavedEntries(savedEntries);
                }
                return result;
            }

            @Override
            public void onPostExecute(DatabaseWriteResult result) {
                cache.doPostDatabaseWrite(result);
            }
        };
    }

    /**
     * Creates an asynchronous task for removing entries from the database. The task reports the number of entries successfully removed.
     *
     * @param cache the handling cache. Needed for callbacks.
     * @return a task object
     */
    AsyncTask<List<String>, Void, Integer> createRemoveTask(final Cache cache) {
        return new AsyncTask<List<String>, Void, Integer>() {
            private static final String TAG = "Fraunhofer.DBRemove";

            @Override
            @SafeVarargs
            protected final Integer doInBackground(List<String>... lists) {
                if (lists == null || cache == null || cache.getHelper() == null || cache.getHelper().getDao() == null)
                    return 0;

                Log.d(TAG, "Removing entries from database.");
                int result = 0;
                for (List<String> ids : lists) {
                    if (ids != null && !ids.isEmpty()) {
                        result += cache.getHelper().getDao().deleteIds(ids);
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer numEntriesRemoved) {
                Log.d(TAG, "cached entries removed: " + numEntriesRemoved);
            }
        };
    }

    /**
     * Creates an asynchronous task to remove entries from a database that has surpassed the specified limit. The oldest entries will be removed
     * based on their timestamp.
     *
     * @param cache        the handling cache. Needed for callbacks.
     * @param dbEntryLimit the maximum number of objects to be left in the database. If < 0, no operation is performed.
     * @return the new task instance
     */

    public AsyncTask<Void, Void, Void> createCleanupTask(final Cache cache, final long dbEntryLimit) {
        return new AsyncTask<Void, Void, Void>() {
            private static final String TAG = "Fraunhofer.DBCleanup";

            @Override
            protected Void doInBackground(Void... voids) {
                if (dbEntryLimit < 0 || cache == null || cache.getHelper() == null || cache.getHelper().getDao() == null)
                    return null;

                RuntimeExceptionDao<CacheEntry, String> dao = cache.getHelper().getDao();

                long size = dao.countOf();
                long numToDelete = size - (dbEntryLimit / 2);

                try {
                    List<CacheEntry> toDelete = dao.queryBuilder()
                            .selectColumns(CacheEntry.ID_FIELD_NAME)
                            .orderBy(CacheEntry.TIMESTAMP_FIELD_NAME, true)
                            .limit(numToDelete)
                            .query();

                    List<String> toDeleteIds = Lists.transform(toDelete, new Function<CacheEntry, String>() {
                        @Nullable
                        @Override
                        public String apply(CacheEntry cacheEntry) {
                            return cacheEntry.getId();
                        }
                    });

                    dao.deleteIds(toDeleteIds);
                } catch (SQLException e) {
                    Log.e(TAG, "Unable to delete entries from database", e);
                }

                return null;
            }
        };
    }
}


