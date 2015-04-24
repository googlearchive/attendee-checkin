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

package com.google.android.apps.gutenberg.util;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.apps.gutenberg.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CheckInRequest extends JsonObjectRequest {

    private final String mCookie;

    public CheckInRequest(String cookie, String eventId, String attendeeId, boolean revert,
                          Response.Listener<JSONObject> listener,
                          Response.ErrorListener errorListener) {
        super(Method.POST,
                BuildConfig.HOST + "/v1/event/" + eventId + "/" + attendeeId + "/checkin",
                createRequestJSONObject(revert),
                listener,
                errorListener);
        mCookie = cookie;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<String, String>();
        headers.putAll(super.getHeaders());
        headers.put("Cookie", mCookie);
        return headers;
    }

    private static JSONObject createRequestJSONObject(boolean revert) {
        JSONObject request = new JSONObject();
        try {
            request.put("revert", revert);
            return request;
        } catch (JSONException e) {
            return null;
        }
    }

}
