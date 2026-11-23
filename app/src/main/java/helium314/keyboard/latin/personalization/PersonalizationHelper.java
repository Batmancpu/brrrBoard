/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.latin.personalization;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import helium314.keyboard.latin.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import helium314.keyboard.latin.common.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helps handle and manage personalized dictionaries such as {@link UserHistoryDictionary}.
 */
public class PersonalizationHelper {
    private static final String TAG = PersonalizationHelper.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final ConcurrentHashMap<String, SoftReference<UserHistoryDictionary>>
            sLangUserHistoryDictCache = new ConcurrentHashMap<>();

    @Nullable
    public static UserHistoryDictionary getUserHistoryDictionary(final Context context, final Locale locale) {
        final Context storageContext = getUnlockedCredentialStorageContext(context);
        if (storageContext == null) return null;
        String lookupStr = locale.toString();
        synchronized (sLangUserHistoryDictCache) {
            if (sLangUserHistoryDictCache.containsKey(lookupStr)) {
                final SoftReference<UserHistoryDictionary> ref =
                        sLangUserHistoryDictCache.get(lookupStr);
                final UserHistoryDictionary dict = ref == null ? null : ref.get();
                if (dict != null) {
                    if (DEBUG) {
                        Log.d(TAG, "Use cached UserHistoryDictionary with lookup: " + lookupStr);
                    }
                    dict.reloadDictionaryIfRequired();
                    return dict;
                }
            }
            final UserHistoryDictionary dict = new UserHistoryDictionary(storageContext, locale);
            sLangUserHistoryDictCache.put(lookupStr, new SoftReference<>(dict));
            return dict;
        }
    }

    /**
     * User-history dictionaries contain words and short context associations. The application
     * itself is Direct-Boot-aware, so its default filesDir is device-protected. Keep this
     * sensitive dictionary in credential-encrypted storage and omit it until Android reports the
     * user unlocked. Android versions before Direct Boot use the ordinary app-private directory.
     */
    @Nullable
    private static Context getUnlockedCredentialStorageContext(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return context;
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null || !userManager.isUserUnlocked()) return null;
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        final Context credentialContext = context.createCredentialProtectedStorageContext();
        if (!PersonalizationStorageMigration.moveLegacyHistoryFiles(
                deviceContext.getFilesDir(), credentialContext.getFilesDir(), UserHistoryDictionary.NAME)) {
            Log.w(TAG, "User-history storage migration did not complete; keeping history unavailable and preserving source data.");
            return null;
        }
        return credentialContext;
    }

    public static void removeAllUserHistoryDictionaries(final Context context) {
        synchronized (sLangUserHistoryDictCache) {
            for (final ConcurrentHashMap.Entry<String, SoftReference<UserHistoryDictionary>> entry
                    : sLangUserHistoryDictCache.entrySet()) {
                if (entry.getValue() != null) {
                    final UserHistoryDictionary dict = entry.getValue().get();
                    if (dict != null) {
                        dict.clear();
                    }
                }
            }
            sLangUserHistoryDictCache.clear();
            deleteHistoryFiles(context.getFilesDir());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    deleteHistoryFiles(context.createCredentialProtectedStorageContext().getFilesDir());
                    deleteHistoryFiles(context.createDeviceProtectedStorageContext().getFilesDir());
                } catch (final IllegalStateException error) {
                    // Credential storage can be unavailable before the first device unlock.
                }
            }
        }
    }

    private static void deleteHistoryFiles(final File filesDir) {
        if (filesDir == null) {
            Log.e(TAG, "Storage directory unavailable while clearing user history.");
            return;
        }
        if (!FileUtils.deleteFilteredFiles(filesDir, new DictFilter(UserHistoryDictionary.NAME))) {
            Log.e(TAG, "Could not remove all user-history dictionary files.");
        }
    }

    private static class DictFilter implements FilenameFilter {
        private final String mName;

        DictFilter(final String name) {
            mName = name;
        }

        @Override
        public boolean accept(final File dir, final String name) {
            return name.startsWith(mName);
        }
    }
}
