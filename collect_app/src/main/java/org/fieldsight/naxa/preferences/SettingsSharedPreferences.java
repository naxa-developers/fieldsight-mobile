/*
 * Copyright (C) 2017 Shobhit
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.fieldsight.naxa.preferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;


import androidx.annotation.Nullable;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.tasks.ServerPollingJob;

import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import static org.odk.collect.android.preferences.GeneralKeys.GENERAL_KEYS;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_PERIODIC_FORM_UPDATES_CHECK;


public class SettingsSharedPreferences {

    private static SettingsSharedPreferences instance;
    private final SharedPreferences sharedPreferences;

    private SettingsSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
    }

    public void register(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }


    public void unregister(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static synchronized SettingsSharedPreferences getInstance() {
        if (instance == null) {
            instance = new SettingsSharedPreferences();
        }
        return instance;
    }


    public Object get(String key) {
        if (sharedPreferences == null) {
            return null;
        }

        Object defaultValue = null;
        Object value = null;

        try {
            defaultValue = SettingsKeys.DEFAULTVALUES.get(key);
        } catch (Exception e) {
            Timber.e("Default for %s not found", key);
        }

        if (defaultValue == null || defaultValue instanceof String) {
            value = sharedPreferences.getString(key, (String) defaultValue);
        } else if (defaultValue instanceof Boolean) {
            value = sharedPreferences.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Long) {
            value = sharedPreferences.getLong(key, (Long) defaultValue);
        } else if (defaultValue instanceof Integer) {
            value = sharedPreferences.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof Float) {
            value = sharedPreferences.getFloat(key, (Float) defaultValue);
        }
        return value;
    }

    public void reset(String key) {
        Object defaultValue = GENERAL_KEYS.get(key);
        save(key, defaultValue);
    }

    public SettingsSharedPreferences save(String key, @Nullable Object value) {
        Editor editor = sharedPreferences.edit();

        if (value == null || value instanceof String) {
            if (key.equals(KEY_PERIODIC_FORM_UPDATES_CHECK) && get(KEY_PERIODIC_FORM_UPDATES_CHECK) != value) {
                ServerPollingJob.schedulePeriodicJob((String) value);
            }
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Set) {
            editor.putStringSet(key, (Set<String>) value);
        } else {
            throw new RuntimeException("Unhandled preference value type: " + value);
        }
        editor.apply();
        return this;
    }

    public boolean getBoolean(String key, boolean value) {
        return sharedPreferences.getBoolean(key, value);
    }

    public void clear() {
        for (Map.Entry<String, ?> prefs : getAll().entrySet()) {
            String key = prefs.getKey();
            reset(key);
        }
    }

    public Map<String, ?> getAll() {
        return sharedPreferences.getAll();
    }


    public static class ValidationException extends RuntimeException {
    }
}
