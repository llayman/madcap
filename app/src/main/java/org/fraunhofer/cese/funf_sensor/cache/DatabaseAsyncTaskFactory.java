package org.fraunhofer.cese.funf_sensor.cache;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;

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
     * Create an asynchronous task which supports asynchronous writing of cache entries to a persistent SQLite database.
     * This class is used when cache entries stored in memory should be persisted. Results of the database writing actions
     * are stored in a DatabaseWriteResult.
     * <p/>
     * This task supports partial saves, i.e., the task will attempt to save each entry. If it encounters an exception or error,
     * it will abort but any previously saved entries will be persisted in the database.
     *
     * @param cache          the cache object handling the request. Needed for callbacks on write completion.
     * @param uploadStrategy the upload strategy to employ after the memcache has been written to the database
     * @return a new instance of an asynchronous database writing task
     * @see org.fraunhofer.cese.funf_sensor.cache.DatabaseWriteResult
     */
    AsyncTask<Map<String, CacheEntry>, Void, DatabaseWriteResult> createWriteTask(final Context context, final Cache cache, final Cache.UploadStrategy uploadStrategy) {

        return new AsyncTask<Map<String, CacheEntry>, Void, DatabaseWriteResult>() {
            private final String TAG = "Fraunhofer.DBWrite";

            @Override
            @SafeVarargs
            public final DatabaseWriteResult doInBackground(Map<String, CacheEntry>... memcaches) {
                DatabaseWriteResult result = DatabaseWriteResult.create();
                // Check preconditions for full or partial write of entry objects to database
                if (context == null) {
                    result.setError(new RuntimeException("{doInBackground} context object is null!"));
                    return result;
                }

                DatabaseOpenHelper databaseHelper = OpenHelperManager.getHelper(context, DatabaseOpenHelper.class);

                if (databaseHelper == null) {
                    result.setError(new RuntimeException("{doInBackground} Attempting to write cache to database, but DatabaseOpenHelper is null. Returning empty result."));
                    return result;
                }

                Collection<String> savedEntries = new ArrayList<>();
                RuntimeExceptionDao<CacheEntry, String> dao;
                try {
                    dao = databaseHelper.getDao();
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
                    if (memcaches != null) {
                        for (Map<String, CacheEntry> memcache : memcaches) {
                            if (memcache != null && !memcache.isEmpty()) {
                                int skippedCount = 0;
                                for (CacheEntry entry : memcache.values()) {
                                    if (databaseHelper.isOpen() && dao.queryForId(entry.getId()) == null && dao.create(entry) > 0) {
                                        savedEntries.add(entry.getId());
                                    } else {
                                        skippedCount++;
                                    }
                                }
                                Log.d(TAG, "{doInBackground} Skipped " + skippedCount + " entries already in database.");
                            }
                        }
                    }
                } catch (Exception e) {
                    result.setError(e);
                } finally {
                    if (databaseHelper.isOpen()) {
                        result.setDatabaseSize(dao.countOf());
                    }
                    result.setSavedEntries(savedEntries);
                }
                return result;
            }

            @Override
            public void onPostExecute(DatabaseWriteResult result) {
                OpenHelperManager.releaseHelper();
                cache.doPostDatabaseWrite(result, uploadStrategy);
            }

            @Override
            protected void onCancelled(DatabaseWriteResult databaseWriteResult) {
                super.onCancelled(databaseWriteResult);
                OpenHelperManager.releaseHelper();
            }
        };
    }

    /**
     * Creates an asynchronous task for removing entries from the database. The task reports the number of entries successfully removed.
     *
     * @param cache the handling cache. Needed for callbacks.
     * @return a task object
     */
    AsyncTask<List<String>, Void, Integer> createRemoveTask(final Context context, final Cache cache) {
        return new AsyncTask<List<String>, Void, Integer>() {
            private static final String TAG = "Fraunhofer.DBRemove";
            private static final int BUFFER_SIZE = 250;

            @Override
            @SafeVarargs
            protected final Integer doInBackground(List<String>... lists) {
                if (lists == null || context == null || cache == null)
                    return 0;

                DatabaseOpenHelper databaseHelper = OpenHelperManager.getHelper(context, DatabaseOpenHelper.class);
                if (databaseHelper == null || databaseHelper.getDao() == null)
                    return 0;

                Log.d(TAG, "Removing entries from database.");
                int result = 0;
                for (List<String> ids : lists) {
                    if (ids != null && !ids.isEmpty() && databaseHelper.isOpen()) {
                        int cursor = 0;
                        while (cursor < ids.size()) {
                            result += databaseHelper.getDao().deleteIds(ids.subList(cursor, (cursor + BUFFER_SIZE > ids.size() ? ids.size() : cursor + BUFFER_SIZE)));
                            cursor += BUFFER_SIZE;
                        }
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer numEntriesRemoved) {
                OpenHelperManager.releaseHelper();
                Log.d(TAG, "Database entries removed: " + numEntriesRemoved);
            }

            @Override
            protected void onCancelled(Integer numEntriesRemoved) {
                Log.d(TAG, "Database entries removed: " + numEntriesRemoved);
                super.onCancelled(numEntriesRemoved);
                OpenHelperManager.releaseHelper();
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

    public AsyncTask<Void, Void, Void> createCleanupTask(final Context context, final Cache cache, final long dbEntryLimit) {
        return new AsyncTask<Void, Void, Void>() {
            private static final String TAG = "Fraunhofer.DBCleanup";

            @Override
            protected Void doInBackground(Void... voids) {
                Log.d(TAG, "Running task to determine if database is still within size limits");

                if (dbEntryLimit < 0 || cache == null)
                    return null;

                DatabaseOpenHelper databaseHelper = OpenHelperManager.getHelper(context, DatabaseOpenHelper.class);
                if (databaseHelper == null || databaseHelper.getDao() == null)
                    return null;

                RuntimeExceptionDao<CacheEntry, String> dao = databaseHelper.getDao();
                long size = dao.countOf();
                if (size < dbEntryLimit) {
                    Log.i(TAG, "No cleanup needed. Database limit: " + dbEntryLimit + " > Database size: " + size);
                } else {
                    Log.i(TAG, "Attempting cleanup. Database limit: " + dbEntryLimit + " <= Database size: " + size);
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

                        int cursor = 0;
                        while (cursor < toDeleteIds.size()) {
                            dao.deleteIds(toDeleteIds.subList(cursor, (cursor + 250 > toDeleteIds.size() ? toDeleteIds.size() : cursor + 250)));
                            cursor += 250;
                        }

                        Log.i(TAG, "Cleanup completed. New database size: " + dao.countOf());
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to delete entries from database", e);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                OpenHelperManager.releaseHelper();
            }

            @Override
            protected void onCancelled(Void aVoid) {
                super.onCancelled(aVoid);
                OpenHelperManager.releaseHelper();
            }
        };
    }
}


