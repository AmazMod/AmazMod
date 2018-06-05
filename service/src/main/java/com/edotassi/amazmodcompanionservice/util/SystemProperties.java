/*
 * Copyright (C) 2015 Jared Rummler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.edotassi.amazmodcompanionservice.util;

/**
 * Gives access to the system properties store. The system properties store contains a list of
 * string key-value pairs.
 */
public class SystemProperties {

    private static final Class<?> SP = getSystemPropertiesClass();

    /**
     * Get the value for the given key.
     */
    public static String get(String key) {
        try {
            return (String) SP.getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the value for the given key.
     *
     * @return if the key isn't found, return def if it isn't null, or an empty string otherwise
     */
    public static String get(String key, String def) {
        try {
            return (String) SP.getMethod("get", String.class, String.class).invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Get the value for the given key, returned as a boolean. Values 'n', 'no', '0', 'false' or
     * 'off' are considered false. Values 'y', 'yes', '1', 'true' or 'on' are considered true. (case
     * sensitive). If the key does not exist, or has any other value, then the default result is
     * returned.
     *
     * @param key
     *     the key to lookup
     * @param def
     *     a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is not able to be
     * parsed as a boolean.
     */
    public static boolean getBoolean(String key, boolean def) {
        try {
            return (Boolean) SP.getMethod("getBoolean", String.class, boolean.class)
                    .invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Get the value for the given key, and return as an integer.
     *
     * @param key
     *     the key to lookup
     * @param def
     *     a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or cannot be parsed
     */
    public static int getInt(String key, int def) {
        try {
            return (Integer) SP.getMethod("getInt", String.class, int.class).invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Get the value for the given key, and return as a long.
     *
     * @param key
     *     the key to lookup
     * @param def
     *     a default value to return
     * @return the key parsed as a long, or def if the key isn't found or cannot be parsed
     */
    public static long getLong(String key, long def) {
        try {
            return (Long) SP.getMethod("getLong", String.class, long.class).invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    private static Class<?> getSystemPropertiesClass() {
        try {
            return Class.forName("android.os.SystemProperties");
        } catch (ClassNotFoundException shouldNotHappen) {
            return null;
        }
    }

    private SystemProperties() {
        throw new AssertionError("no instances");
    }

}
