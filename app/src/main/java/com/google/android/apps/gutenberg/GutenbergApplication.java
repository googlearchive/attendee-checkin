/*
 * Copyright 2015 Google Inc.
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

package com.google.android.apps.gutenberg;

import android.accounts.Account;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.apps.gutenberg.provider.SyncAdapter;
import com.google.android.apps.gutenberg.provider.Table;
import com.google.android.apps.gutenberg.util.BitmapCache;

/**
 * Manages information shared among all parts of the app.
 */
public class GutenbergApplication extends Application {

    private static final String PREF_NAME = "gutenberg";
    private static final String PREF_AUTH_TOKEN = "auth_token";
    private static final String PREF_ACCOUNT = "account";
    public static final String PREF_EVENT_ID = "event_id";

    private RequestQueue mRequestQueue;
    private BitmapCache mBitmapCache;

    private Account mAccount;
    private String mAuthToken;
    private String mEventId;

    public static GutenbergApplication from(Context context) {
        return (GutenbergApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getDefaultSharedPreferences();
        String account = prefs.getString(PREF_ACCOUNT, null);
        if (account != null) {
            mAccount = new Account(account, "com.google");
        }
        mAuthToken = prefs.getString(PREF_AUTH_TOKEN, null);
        mEventId = prefs.getString(PREF_EVENT_ID, null);
    }

    public Account getAccount() {
        return mAccount;
    }

    public SharedPreferences getDefaultSharedPreferences() {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    public void setAccount(Account account) {
        getDefaultSharedPreferences()
                .edit()
                .putString(PREF_ACCOUNT, account.name)
                .apply();
        mAccount = account;
    }

    public void setAuthToken(String authToken) {
        getDefaultSharedPreferences()
                .edit()
                .putString(PREF_AUTH_TOKEN, authToken)
                .apply();
        mAuthToken = authToken;
    }

    public String getEventId() {
        return mEventId;
    }

    public void setEventId(String eventId) {
        getDefaultSharedPreferences()
                .edit()
                .putString(PREF_EVENT_ID, eventId)
                .apply();
        mEventId = eventId;
    }

    /**
     * @return The instance of {@link BitmapCache}.
     */
    public BitmapCache getBitmapCache() {
        if (mBitmapCache == null) {
            mBitmapCache = new BitmapCache();
        }
        return mBitmapCache;
    }

    /**
     * @return The instance of {@link RequestQueue}.
     */
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
            mRequestQueue.start();
        }
        return mRequestQueue;
    }

    public boolean isUserLoggedIn() {
        return mAccount != null && mAuthToken != null;
    }

    public boolean requestSync(boolean onlyCheckins) {
        if (!isUserLoggedIn()) {
            return false;
        }
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        extras.putString(SyncAdapter.EXTRA_AUTH_TOKEN, mAuthToken);
        extras.putBoolean(SyncAdapter.EXTRA_ONLY_CHECKINS, onlyCheckins);
        ContentResolver.setSyncAutomatically(mAccount, Table.AUTHORITY, true);
        ContentResolver.setIsSyncable(mAccount, Table.AUTHORITY, 1);
        if (ContentResolver.isSyncPending(mAccount, Table.AUTHORITY) ||
                ContentResolver.isSyncActive(mAccount, Table.AUTHORITY)) {
            ContentResolver.cancelSync(mAccount, Table.AUTHORITY);
        }
        ContentResolver.requestSync(mAccount, Table.AUTHORITY, extras);
        return true;
    }

}
