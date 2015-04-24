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

public class Explanation extends TimelineItem {

    private final String mEventName;

    public Explanation(Cursor cursor) {
        super(0);
        mEventName = cursor.getString(cursor.getColumnIndexOrThrow(Table.Event.NAME));
    }

    public String getEventName() {
        return mEventName;
    }

}
