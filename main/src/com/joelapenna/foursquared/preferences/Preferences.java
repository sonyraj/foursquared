/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquared.preferences;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.error.FoursquareCredentialsException;
import com.joelapenna.foursquare.error.FoursquareError;
import com.joelapenna.foursquare.error.FoursquareException;
import com.joelapenna.foursquare.types.City;
import com.joelapenna.foursquare.types.Data;
import com.joelapenna.foursquare.types.Settings;
import com.joelapenna.foursquare.types.User;
import com.joelapenna.foursquared.FoursquaredSettings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
public class Preferences {
    private static final String TAG = "Preferences";
    private static final boolean DEBUG = FoursquaredSettings.DEBUG;

    // Visible Preferences (sync with preferences.xml)
    public static final String PREFERENCE_TWITTER_CHECKIN = "twitter_checkin";
    public static final String PREFERENCE_SHARE_CHECKIN = "share_checkin";
    public static final String PREFERENCE_IMMEDIATE_CHECKIN = "immediate_checkin";

    // Hacks for preference activity extra UI elements.
    public static final String PREFERENCE_FRIEND_REQUESTS = "friend_requests";
    public static final String PREFERENCE_FRIEND_ADD = "friend_add";
    public static final String PREFERENCE_CITY_NAME = "city_name";
    public static final String PREFERENCE_LOGOUT = "logout";

    // Credentials related preferences
    public static final String PREFERENCE_LOGIN = "phone";
    public static final String PREFERENCE_PASSWORD = "password";
    public static final String PREFERENCE_OAUTH_TOKEN = "oauth_token";
    public static final String PREFERENCE_OAUTH_TOKEN_SECRET = "oauth_token_secret";

    // Extra info for getUser
    private static final String PREFERENCE_CITY_ID = "city_id";
    private static final String PREFERENCE_CITY_GEOLAT = "city_geolat";
    private static final String PREFERENCE_CITY_GEOLONG = "city_geolong";
    private static final String PREFERENCE_FIRST = "first_name";
    private static final String PREFERENCE_GENDER = "gender";
    private static final String PREFERENCE_ID = "id";
    private static final String PREFERENCE_LAST = "last_name";
    private static final String PREFERENCE_PHOTO = "photo";

    // Not-in-XML preferences for dumpcatcher
    public static final String PREFERENCE_DUMPCATCHER_CLIENT = "dumpcatcher_client";

    public static String createUniqueId(SharedPreferences preferences) {
        String uniqueId = preferences.getString(PREFERENCE_DUMPCATCHER_CLIENT, null);
        if (uniqueId == null) {
            uniqueId = UUID.randomUUID().toString();
            Editor editor = preferences.edit();
            editor.putString(PREFERENCE_DUMPCATCHER_CLIENT, uniqueId);
            editor.commit();
        }
        return uniqueId;
    }

    /**
     * Log in a user and put credential information into the preferences edit queue.
     *
     * @param foursquare
     * @param login
     * @param password
     * @param editor
     * @throws FoursquareCredentialsException
     * @throws FoursquareException
     * @throws IOException
     */
    public static User loginUser(Foursquare foursquare, String login, String password,
            Location location, Editor editor) throws FoursquareCredentialsException,
            FoursquareException, IOException {
        if (DEBUG) Log.d(Preferences.TAG, "Trying to log in.");

        foursquare.setCredentials(login, password);
        storeLoginAndPassword(editor, login, password);
        editor.commit();

        City city = switchCity(foursquare, location);
        storeCity(editor, city);
        editor.commit();

        User user = foursquare.user(null, false, false);
        storeUser(editor, user);
        editor.commit();

        return user;
    }

    public static boolean logoutUser(Foursquare foursquare, Editor editor) {
        if (DEBUG) Log.d(Preferences.TAG, "Trying to log out.");
        // TODO: If we re-implement oAuth, we'll have to call clearAllCrendentials here.
        foursquare.setCredentials(null, null);
        return editor.clear().commit();
    }

    public static User getUser(SharedPreferences prefs) {
        City city = new City();
        city.setId(prefs.getString(Preferences.PREFERENCE_CITY_ID, null));
        city.setName(prefs.getString(Preferences.PREFERENCE_CITY_NAME, null));
        city.setGeolat(prefs.getString(Preferences.PREFERENCE_CITY_GEOLAT, null));
        city.setGeolong(prefs.getString(Preferences.PREFERENCE_CITY_GEOLONG, null));

        Settings settings = new Settings();
        settings.setSendtotwitter(prefs.getBoolean(PREFERENCE_TWITTER_CHECKIN, false));

        User user = new User();
        user.setId(prefs.getString(PREFERENCE_ID, null));
        user.setFirstname(prefs.getString(PREFERENCE_FIRST, null));
        user.setLastname(prefs.getString(PREFERENCE_LAST, null));
        user.setGender(prefs.getString(PREFERENCE_GENDER, null));
        user.setPhoto(prefs.getString(PREFERENCE_PHOTO, null));
        user.setCity(city);
        user.setSettings(settings);

        return user;
    }

    public static City switchCity(Foursquare foursquare, Location location)
            throws FoursquareException, FoursquareError, IOException {
        City finalCity = null;

        if (location != null) {
            City newCity = foursquare.checkCity(Foursquare.Location.fromAndroidLocation(location));

            if (newCity != null) {
                Data response = foursquare.switchCity(newCity.getId());
                if (response.status()) {
                    finalCity = newCity;
                }
            }

        }
        return finalCity;
    }

    public static void storeLoginAndPassword(final Editor editor, String login, String password) {
        editor.putString(PREFERENCE_LOGIN, login);
        editor.putString(PREFERENCE_PASSWORD, password);
    }

    public static void storeUser(final Editor editor, User user) {
        if (user != null && user.getId() != null) {
            editor.putString(PREFERENCE_ID, user.getId());
            editor.putBoolean(PREFERENCE_TWITTER_CHECKIN, user.getSettings().sendtotwitter());
            if (DEBUG) Log.d(TAG, "Setting user info");
        } else {
            if (Preferences.DEBUG) Log.d(Preferences.TAG, "Unable to lookup user.");
        }
    }

    public static void storeCity(final Editor editor, City city) {
        if (city != null) {
            editor.putString(PREFERENCE_CITY_ID, city.getId());
            editor.putString(PREFERENCE_CITY_GEOLAT, city.getGeolat());
            editor.putString(PREFERENCE_CITY_GEOLONG, city.getGeolong());
            editor.putString(PREFERENCE_CITY_NAME, city.getName());
        }
    }
}