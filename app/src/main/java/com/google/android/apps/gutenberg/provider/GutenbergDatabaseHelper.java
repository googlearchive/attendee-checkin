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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * The database of Gutenberg.
 */
public class GutenbergDatabaseHelper extends SQLiteOpenHelper {

    /**
     * The filename for the database file.
     */
    private static final String DATABASE_NAME = "gutenberg.db";

    /**
     * The current version of the database
     */
    private static final int DATABASE_VERSION = 6;

    public GutenbergDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Table table : Table.values()) {
            db.execSQL(table.getCreateSql());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2 && 2 <= newVersion) {
            db.execSQL("ALTER TABLE " + Table.ATTENDEE.getBaseName() + " ADD " +
                    Table.Attendee.IMAGE_URL + " TEXT;");
        } else if (oldVersion < 3 && 3 <= newVersion) {
            db.execSQL("DROP TABLE " + Table.ATTENDEE.getBaseName() + ";");
            db.execSQL(Table.EVENT.getCreateSql());
            db.execSQL(Table.ATTENDEE.getCreateSql());
        } else if (oldVersion < 4 && 4 <= newVersion) {
            db.execSQL("DROP TABLE " + Table.ATTENDEE.getBaseName() + ";");
            db.execSQL("DROP TABLE " + Table.EVENT.getBaseName() + ";");
            db.execSQL(Table.EVENT.getCreateSql());
            db.execSQL(Table.ATTENDEE.getCreateSql());
        } else if (oldVersion < 5 && 5 <= newVersion) {
            db.execSQL("DROP TABLE " + Table.ATTENDEE.getBaseName() + ";");
            db.execSQL(Table.ATTENDEE.getCreateSql());
        } else if (oldVersion < 6 && 6 <= newVersion) {
            db.execSQL("DROP TABLE " + Table.ATTENDEE.getBaseName() + ";");
            db.execSQL(Table.ATTENDEE.getCreateSql());
        }
    }

}
