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

package com.google.android.apps.gutenberg.model;

import android.database.Cursor;

import com.google.android.apps.gutenberg.provider.Table;

public class Checkin extends TimelineItem {

    public static final String[] PROJECTION = {
            Table.Attendee.ID,
            Table.Attendee.NAME,
            Table.Attendee.CHECKIN,
            Table.Attendee.IMAGE_URL,
    };

    private final String mAttendeeId;
    private final String mAttendeeName;
    private final String mAttendeeImageUrl;

    public Checkin(Cursor cursor) {
        super(cursor.getLong(cursor.getColumnIndexOrThrow(Table.Attendee.CHECKIN)));
        mAttendeeId = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.ID));
        mAttendeeName = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.NAME));
        mAttendeeImageUrl = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.IMAGE_URL));
    }

    public String getAttendeeId() {
        return mAttendeeId;
    }

    public String getAttendeeName() {
        return mAttendeeName;
    }

    public String getAttendeeImageUrl() {
        return mAttendeeImageUrl;
    }

}
