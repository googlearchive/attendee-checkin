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

package com.google.android.apps.gutenberg.provider;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.toolbox.RequestFuture;
import com.google.android.apps.gutenberg.BuildConfig;
import com.google.android.apps.gutenberg.GutenbergApplication;
import com.google.android.apps.gutenberg.util.CheckInRequest;
import com.google.android.apps.gutenberg.util.GaeJsonArrayRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String EXTRA_AUTH_TOKEN = "auth_token";

    /**
     * Boolean extra for syncing checkinTime only
     */
    public static final String EXTRA_ONLY_CHECKINS = "only_checkins";

    private static final String TAG = "SyncAdapter";

    private GoogleApiClient mApiClient;

    public SyncAdapter(Context context, boolean autoInitialize) {
        this(context, autoInitialize, false);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        String authToken = extras.getString(EXTRA_AUTH_TOKEN);
        if (TextUtils.isEmpty(authToken)) {
            Log.d(TAG, "Not authorized. Cannot sync.");
            return;
        }
        mApiClient.blockingConnect(5, TimeUnit.SECONDS);
        try {
            String cookie = getCookie(authToken);
            syncCheckins(provider, cookie);
            if (!extras.getBoolean(EXTRA_ONLY_CHECKINS, false)) {
                syncEvents(provider, cookie);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error performing sync.", e);
        }
    }

    private void syncCheckins(ContentProviderClient provider, String cookie) {
        Cursor cursor = null;
        try {
            cursor = provider.query(Table.ATTENDEE.getBaseUri(), new String[]{
                    Table.Attendee.ID,
                    Table.Attendee.CHECKIN,
                    Table.Attendee.EVENT_ID,
            }, Table.Attendee.CHECKIN_MODIFIED, null, null);
            if (0 == cursor.getCount()) {
                Log.d(TAG, "No checkin to sync.");
                return;
            }
            int syncCount = 0;
            while (cursor.moveToNext()) {
                String attendeeId = cursor.getString(
                        cursor.getColumnIndexOrThrow(Table.Attendee.ID));
                String eventId = cursor.getString(
                        cursor.getColumnIndexOrThrow(Table.Attendee.EVENT_ID));
                long checkin = cursor.getLong(cursor.getColumnIndexOrThrow(Table.Attendee.CHECKIN));
                long serverCheckin = postCheckIn(attendeeId, eventId, checkin == 0, cookie);
                if (serverCheckin >= 0) {
                    ContentValues values = new ContentValues();
                    values.put(Table.Attendee.CHECKIN_MODIFIED, false);
                    if (0 == serverCheckin) {
                        values.putNull(Table.Attendee.CHECKIN);
                    } else {
                        values.put(Table.Attendee.CHECKIN, serverCheckin);
                    }
                    provider.update(Table.ATTENDEE.getItemUri(eventId, attendeeId),
                            values, null, null);
                    ++syncCount;
                }
            }
            Log.d(TAG, syncCount + " checkin(s) synced.");
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private long postCheckIn(String attendeeId, String eventId, boolean revert, String cookie) {
        RequestQueue queue = GutenbergApplication.from(getContext()).getRequestQueue();
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        queue.add(new CheckInRequest(cookie, eventId, attendeeId, revert, future, future));
        try {
            JSONObject object = future.get();
            return object.getLong("checkinTime");
        } catch (InterruptedException | ExecutionException | JSONException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServerError) {
                ServerError error = (ServerError) cause;
                Log.e(TAG, "Server error: " + new String(error.networkResponse.data));
            }
            Log.e(TAG, "Cannot sync checkin.", e);
        }
        return -1;
    }

    private void syncEvents(ContentProviderClient provider, String cookie) {
        try {
            RequestQueue requestQueue = GutenbergApplication.from(getContext()).getRequestQueue();
            JSONArray events = getEvents(requestQueue, cookie);
            Pair<String[], ContentValues[]> pair = parseEvents(events);
            String[] eventIds = pair.first;
            provider.bulkInsert(Table.EVENT.getBaseUri(), pair.second);
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            operations.add(ContentProviderOperation.newDelete(Table.EVENT.getBaseUri())
                    .withSelection(Table.Event.ID + " NOT IN ('" +
                            TextUtils.join("', '", eventIds) + "')", null)
                    .build());
            operations.add(ContentProviderOperation.newDelete(Table.ATTENDEE.getBaseUri())
                    .withSelection(Table.Attendee.EVENT_ID + " NOT IN ('" +
                            TextUtils.join("', '", eventIds) + "')", null)
                    .build());
            provider.applyBatch(operations);
            for (String eventId : eventIds) {
                JSONArray attendees = getAttendees(requestQueue, eventId, cookie);
                provider.bulkInsert(
                        Table.ATTENDEE.getBaseUri(), parseAttendees(eventId, attendees));
            }
            Log.d(TAG, eventIds.length + " event(s) synced.");
        } catch (ExecutionException | InterruptedException | JSONException | RemoteException |
                OperationApplicationException e) {
            Log.e(TAG, "Error performing sync.", e);
        }
    }

    private static String getCookie(String authToken) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(
                    BuildConfig.HOST + "/_ah/login?continue=http://localhost/&auth=" +
                            authToken).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            if (connection.getResponseCode() != 302) {
                Log.e(TAG, "Cannot fetch the cookie: " + connection.getResponseCode());
                return null;
            }
            String cookie = connection.getHeaderField("Set-Cookie");
            if (!cookie.contains("SACSID")) {
                return null;
            }
            return cookie;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONArray getEvents(RequestQueue requestQueue, String cookie) throws
            ExecutionException, InterruptedException {
        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        requestQueue.add(new GaeJsonArrayRequest(
                BuildConfig.HOST + "/v1/event/list", cookie, future, future));
        try {
            return future.get();
        } catch (ExecutionException e) {
            if (didServerReturnNull(e)) {
                return new JSONArray();
            }
            throw e;
        }
    }

    private static Pair<String[], ContentValues[]> parseEvents(JSONArray events) throws JSONException {
        int length = events.length();
        ContentValues[] array = new ContentValues[length];
        String[] ids = new String[length];
        for (int i = 0; i < length; ++i) {
            JSONObject event = events.getJSONObject(i);
            array[i] = new ContentValues();
            String id = event.getString("id");
            ids[i] = id;
            array[i].put(Table.Event.ID, id);
            array[i].put(Table.Event.NAME, event.getString("name"));
            array[i].put(Table.Event.PLACE, event.getString("place"));
            array[i].put(Table.Event.ORGANIZER_NAME, event.getString("organizerName"));
            array[i].put(Table.Event.START_TIME, event.getString("startTime"));
            array[i].put(Table.Event.END_TIME, event.getString("endTime"));
        }
        return new Pair<>(ids, array);
    }

    private static JSONArray getAttendees(RequestQueue requestQueue, String eventId, String cookie)
            throws ExecutionException, InterruptedException {
        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        requestQueue.add(new GaeJsonArrayRequest(
                BuildConfig.HOST + "/v1/event/" + eventId + "/attendees", cookie, future, future));
        try {
            return future.get();
        } catch (ExecutionException e) {
            if (didServerReturnNull(e)) {
                return new JSONArray();
            }
            throw e;
        }
    }

    // A workaround for the server returning "null" for empty array
    private static boolean didServerReturnNull(ExecutionException e) {
        if (e.getCause() instanceof ParseError) {
            ParseError cause = (ParseError) e.getCause();
            if (cause.getCause() instanceof JSONException) {
                JSONException causeCause = (JSONException) cause.getCause();
                if (causeCause.getMessage().contains("Value null of")) {
                    return true;
                }
            }
        }
        return false;
    }

    private ContentValues[] parseAttendees(String eventId, JSONArray attendees)
            throws JSONException {
        int length = attendees.length();
        ContentValues[] array = new ContentValues[length];
        HashMap<String, String> imageUrls = new HashMap<>();
        for (int i = 0; i < length; ++i) {
            JSONObject attendee = attendees.getJSONObject(i);
            array[i] = new ContentValues();
            array[i].put(Table.Attendee.EVENT_ID, eventId);
            array[i].put(Table.Attendee.NAME, attendee.getString("name"));
            array[i].put(Table.Attendee.ID, attendee.getString("id"));
            array[i].put(Table.Attendee.EMAIL, attendee.getString("email"));
            String plusid = attendee.getString("plusid");
            if (!TextUtils.isEmpty(plusid)) {
                array[i].put(Table.Attendee.PLUSID, plusid);
                imageUrls.put(plusid, "null");
            }
            long checkinTime = attendee.getLong("checkinTime");
            if (0 == checkinTime) {
                array[i].putNull(Table.Attendee.CHECKIN);
            } else {
                array[i].put(Table.Attendee.CHECKIN, checkinTime);
            }
            array[i].putNull(Table.Attendee.IMAGE_URL);
        }
        // Fetch all the Google+ Image URLs at once if necessary
        if (mApiClient != null && mApiClient.isConnected() && !imageUrls.isEmpty()) {
            People.LoadPeopleResult result =
                    Plus.PeopleApi.load(mApiClient, imageUrls.keySet()).await();
            PersonBuffer personBuffer = result.getPersonBuffer();
            if (personBuffer != null) {
                // Copy URLs into the HashMap
                for (Person person : personBuffer) {
                    if (person.hasImage()) {
                        imageUrls.put(extractId(person.getUrl()), person.getImage().getUrl());
                    }
                }
                // Fill the missing URLs in the array of ContentValues
                for (ContentValues values : array) {
                    if (values.containsKey(Table.Attendee.PLUSID)) {
                        String plusId = values.getAsString(Table.Attendee.PLUSID);
                        String imageUrl = imageUrls.get(plusId);
                        if (!TextUtils.isEmpty(imageUrl)) {
                            values.put(Table.Attendee.IMAGE_URL, imageUrl);
                        }
                    }
                }
            }
        }
        return array;
    }

    private static String extractId(String profileUrl) {
        return profileUrl.substring(profileUrl.lastIndexOf('/') + 1);
    }

}
