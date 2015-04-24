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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.google.android.apps.gutenberg.GutenbergApplication;
import com.google.android.apps.gutenberg.model.Checkin;
import com.google.android.apps.gutenberg.provider.Table;

public class CheckInTask extends AsyncTask<Void, Void, Checkin> {

    private final Context mContext;
    private final String mAttendeeId;
    private final String mEventId;
    private final boolean mRevert;
    private final OnCompleteListener mListener;
    private int mError;

    public static final int ERROR_ALREADY_CHECKED_IN = 1;
    public static final int ERROR_NOT_YET_CHECKED_IN = 2;
    public static final int ERROR_BAD_CHECK_IN = 3;

    public CheckInTask(Context context, String attendeeId, String eventId, boolean revert,
                       OnCompleteListener listener) {
        mContext = context;
        mAttendeeId = attendeeId;
        mEventId = eventId;
        mRevert = revert;
        mListener = listener;
    }

    @Override
    protected Checkin doInBackground(Void... params) {
        ContentResolver resolver = mContext.getContentResolver();
        boolean userCheckedIn = isUserCheckedIn(resolver);
        if (mError == ERROR_BAD_CHECK_IN) {
            return null;
        }
        if (!mRevert && userCheckedIn) {
            mError = ERROR_ALREADY_CHECKED_IN;
            return null;
        } else if (mRevert && !userCheckedIn) {
            mError = ERROR_NOT_YET_CHECKED_IN;
            return null;
        }
        ContentValues values = new ContentValues();
        if (mRevert) {
            values.putNull(Table.Attendee.CHECKIN);
        } else {
            values.put(Table.Attendee.CHECKIN, System.currentTimeMillis());
        }
        values.put(Table.Attendee.CHECKIN_MODIFIED, true);
        int count = resolver.update(Table.ATTENDEE.getBaseUri(), values, Table.Attendee.ID + " = ?",
                new String[]{mAttendeeId});
        return count == 0 ? null : loadCheckin(resolver);
    }

    private boolean isUserCheckedIn(ContentResolver resolver) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(Table.ATTENDEE.getBaseUri(),
                    new String[]{Table.Attendee.CHECKIN},
                    Table.Attendee.EVENT_ID + " = ? AND " + Table.Attendee.ID + " = ?",
                    new String[]{mEventId, mAttendeeId}, null);
            if (0 == cursor.getCount()) {
                mError = ERROR_BAD_CHECK_IN;
                return false;
            }
            cursor.moveToFirst();
            return cursor.getLong(cursor.getColumnIndexOrThrow(Table.Attendee.CHECKIN)) > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Checkin loadCheckin(ContentResolver resolver) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(Table.ATTENDEE.getItemUri(mEventId, mAttendeeId),
                    Checkin.PROJECTION, null, null, null);
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return new Checkin(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onPostExecute(Checkin checkin) {
        if (checkin != null) {
            GutenbergApplication.from(mContext).requestSync(true);
        }
        if (mListener != null) {
            mListener.onComplete(checkin, mError);
        }
    }

    public interface OnCompleteListener {
        public void onComplete(Checkin checkin, int error);
    }

}
