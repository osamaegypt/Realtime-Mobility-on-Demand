
package com.gudana.mod.data;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;


public class Preferences {
    static final String KEY_FAVORITES = "key_favorites";
    private static final String KEY_UNITS = "key_units";
    private static final String KEY_GASTRO_FILTER = "key_gastro_filter";
    private static final String KEY_GASTRO_LAST_MODIFIED = "key_gastro_last_modified";
    private static final String KEY_SHOPPING_LAST_MODIFIED = "key_shopping_last_modified";

    public static boolean isMetricUnit(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(Preferences.KEY_UNITS, true);
    }


    public static void removeGastroFilter(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(Preferences.KEY_GASTRO_FILTER);
        editor.apply();

    }



    public static void saveFavorites(Context context, Set<String> favoriteIDs) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putStringSet(KEY_FAVORITES, favoriteIDs);
        editor.apply();
    }

    public static Set<String> getFavorites(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // returns a new cloned instance, because its not allowed to "work" on getStringSet result directly
        // see https://stackoverflow.com/questions/14034803/misbehavior-when-trying-to-store-a-string-set-using-sharedpreferences/14034804#14034804
        return new HashSet<>(prefs.getStringSet(Preferences.KEY_FAVORITES, new HashSet<String>()));
    }

    /**
     * return last modified date of the gastro location database, in milliseconds
     *
     * @return date in milliseconds or 0 if not set
     */
    public static long getGastroLastModified(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(Preferences.KEY_GASTRO_LAST_MODIFIED, 0);
    }

    public static void saveGastroLastModified(Context context, long lastModified) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(Preferences.KEY_GASTRO_LAST_MODIFIED, lastModified);
        editor.apply();
    }

    /**
     * return last modified date of the shopping location database, in milliseconds
     *
     * @return date in milliseconds or 0 if not set
     */
    public static long getShoppingLastModified(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(Preferences.KEY_SHOPPING_LAST_MODIFIED, 0);
    }

    public static void saveShoppingLastModified(Context context, long lastModified) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(Preferences.KEY_SHOPPING_LAST_MODIFIED, lastModified);
        editor.apply();
    }
}
