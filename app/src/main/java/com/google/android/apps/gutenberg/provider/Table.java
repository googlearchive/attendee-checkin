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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * Each database table is represented as an item of this enumeration type.
 */
public enum Table {

    /**
     * Event table.
     */
    EVENT("events", new Column[]{
            new Column(Event._ID, Column._ID_TYPE),
            new Column(Event.ID, "TEXT NOT NULL"),
            new Column(Event.NAME, "TEXT NOT NULL"),
            new Column(Event.ORGANIZER_NAME, "TEXT NOT NULL"),
            new Column(Event.PLACE, "TEXT NOT NULL"),
            new Column(Event.START_TIME, "INTEGER NOT NULL"), // Unix-time (seconds)
            new Column(Event.END_TIME, "INTEGER NOT NULL"), // Unix-time (seconds)
    }, new String[]{
            Event.ID
    }),

    /**
     * Attendee table.
     */
    ATTENDEE("attendees", new Column[]{
            new Column(Attendee._ID, Column._ID_TYPE),
            new Column(Attendee.EVENT_ID, "TEXT NOT NULL"),
            new Column(Attendee.ID, "TEXT NOT NULL"),
            new Column(Attendee.EMAIL, "TEXT NOT NULL"),
            new Column(Attendee.NAME, "TEXT NOT NULL"),
            new Column(Attendee.PLUSID, "TEXT"),
            new Column(Attendee.IMAGE_URL, "TEXT"),
            new Column(Attendee.CHECKIN, "INTEGER"), // Unix-time (seconds)
            new Column(Attendee.CHECKIN_MODIFIED, "BOOLEAN", "FALSE"),
            new Column(Attendee.NOTE, "TEXT"),
    }, new String[]{
            Attendee.EVENT_ID,
            Attendee.ID
    });

    /**
     * Authority for {@link android.content.ContentProvider}
     */
    public static final String AUTHORITY = "com.google.android.apps.gutenberg";

    /**
     * Constant value for base type in {@link android.content.ContentProvider}.
     */
    public static final int IS_BASE = 0;

    /**
     * Constant value for item type in {@link android.content.ContentProvider}.
     */
    public static final int IS_ITEM = 1;

    private static final int CODE_OFFSET = 1;
    private static final int CODE_SPAN = 2; // IS_BASE or IS_ITEM, so always 2

    private final String mName;
    private final Column[] mColumns;
    private final String[] mIdColumns;

    /**
     * Create a new instance of {@link Table}.
     *
     * @param name      The name of this table
     * @param columns   The list of columns in this table
     * @param idColumns The names of columns declared to be unique as a set
     */
    private Table(String name, Column[] columns, String[] idColumns) {
        mName = name;
        mColumns = columns;
        mIdColumns = idColumns;
    }

    /**
     * Get the instance of {@link MatchResult} that corresponds to the code.
     *
     * @param code The code
     * @return The instance of {@link MatchResult} that corresponds to the code.
     */
    public static MatchResult match(int code) {
        int baseOrItem = (code - CODE_OFFSET) % CODE_SPAN;
        int index = (code - CODE_OFFSET - baseOrItem) / CODE_SPAN;
        if (Table.values().length <= index) {
            return null;
        }
        return new MatchResult(Table.values()[index], baseOrItem == IS_ITEM);
    }

    /**
     * Get the code in {@link android.content.ContentProvider} for whole this table.
     *
     * @return The code
     */
    public int getBaseCode() {
        return CODE_OFFSET + IS_BASE + ordinal() * CODE_SPAN;
    }

    /**
     * Get the code in {@link android.content.ContentProvider} for each item in this table.
     *
     * @return The code
     */
    public int getItemCode() {
        return CODE_OFFSET + IS_ITEM + ordinal() * CODE_SPAN;
    }

    /**
     * Get the name of whole this table.
     *
     * @return The table name
     */
    public String getBaseName() {
        return mName;
    }

    /**
     * Get the name for each item in this table.
     *
     * @return The table name
     */
    public String getItemName() {
        StringBuilder name = new StringBuilder(mName);
        for (int i = 0; i < mIdColumns.length; i++) {
            name.append("/*");
        }
        return name.toString();
    }

    /**
     * Get the {@link Uri} of this table.
     *
     * @return The {@link Uri} in {@link android.content.ContentProvider}
     */
    public Uri getBaseUri() {
        return Uri.parse("content://" + Table.AUTHORITY + "/" + mName);
    }

    /**
     * Get the {@link Uri} for the specified item in this table.
     *
     * @param ids The IDs of the item
     * @return The {@link Uri} in {@link android.content.ContentProvider}
     */
    public Uri getItemUri(String... ids) {
        return Uri.parse("content://" + Table.AUTHORITY + "/" + mName + "/"
                + TextUtils.join("/", ids));
    }

    /**
     * Get the type of this table.
     *
     * @return The type
     */
    public String getBaseType() {
        return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + "." + mName;
    }

    /**
     * Get the type of each item in this table.
     *
     * @return The type
     */
    public String getItemType() {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + "." + mName;
    }

    public String getSelectionById() {
        StringBuilder selection = new StringBuilder();
        for (int i = 0; i < mIdColumns.length; i++) {
            if (i != 0) {
                selection.append(" AND ");
            }
            selection.append(mIdColumns[i]);
            selection.append(" = ?");
        }
        return selection.toString();
    }

    /**
     * Get the "CREATE TABLE" statement for this table.
     *
     * @return The "CREATE TABLE" statement as a {@link java.lang.String}
     */
    public String getCreateSql() {
        StringBuilder buffer = new StringBuilder("CREATE TABLE ");
        buffer.append(mName);
        buffer.append(" (");
        for (Column column : mColumns) {
            buffer.append(column.name);
            buffer.append(" ");
            buffer.append(column.type);
            if (column.defaultValue != null) {
                buffer.append("DEFAULT ");
                buffer.append(column.defaultValue);
            }
            buffer.append(", ");
        }
        buffer.append("UNIQUE (");
        buffer.append(TextUtils.join(", ", mIdColumns));
        buffer.append(") ON CONFLICT REPLACE);");
        return buffer.toString();
    }

    /**
     * Column constants for the `events` table.
     */
    public interface Event extends BaseColumns {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String ORGANIZER_NAME = "organizer_name";
        public static final String PLACE = "place";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
    }

    /**
     * Column constants for the `attendees` table.
     */
    public interface Attendee extends BaseColumns {
        public static final String ID = "code";
        public static final String EVENT_ID = "event_id";
        public static final String EMAIL = "email";
        public static final String NAME = "name";
        public static final String PLUSID = "plusid";
        public static final String IMAGE_URL = "image_url";
        public static final String CHECKIN = "checkin";
        public static final String CHECKIN_MODIFIED = "checkin_modified";
        public static final String NOTE = "note";
    }

}
