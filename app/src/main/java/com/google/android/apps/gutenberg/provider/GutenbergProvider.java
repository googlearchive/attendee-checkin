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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.apps.gutenberg.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class GutenbergProvider extends ContentProvider {

    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        for (Table table : Table.values()) {
            MATCHER.addURI(Table.AUTHORITY, table.getBaseName(), table.getBaseCode());
            MATCHER.addURI(Table.AUTHORITY, table.getItemName(), table.getItemCode());
        }
    }

    private GutenbergDatabaseHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = new GutenbergDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        MatchResult match = Table.match(MATCHER.match(uri));
        if (match == null) {
            throw new IllegalArgumentException("Illegal URI: " + uri);
        }
        if (match.isItem()) {
            List<String> segments = uri.getPathSegments();
            if (segments == null || segments.isEmpty()) {
                throw new IllegalArgumentException("Malformed URI: " + uri);
            }
            List<String> ids = segments.subList(1, segments.size());
            String selectionById = match.getTable().getSelectionById();
            String[] selectionArgsById = ids.toArray(new String[ids.size()]);
            if (selection == null) {
                selection = selectionById;
                selectionArgs = selectionArgsById;
            } else {
                selection = selectionById + " " + selection;
                selectionArgs = ArrayUtils.concat(selectionArgsById, selectionArgs);
            }
        }
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(match.getTable().getBaseName(), projection, selection,
                selectionArgs, null, null, sortOrder);
        Context context = getContext();
        if (context == null) {
            return null;
        }
        cursor.setNotificationUri(context.getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        MatchResult match = Table.match(MATCHER.match(uri));
        if (match == null) {
            throw new IllegalArgumentException("Illegal URI: " + uri);
        }
        return match.getType();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        MatchResult match = Table.match(MATCHER.match(uri));
        if (match == null || match.isItem()) {
            throw new IllegalArgumentException("Illegal URI: " + uri);
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long id = db.insert(match.getTable().getBaseName(), null, values);
        Uri newUri = ContentUris.withAppendedId(uri, id);
        Context context = getContext();
        context.getContentResolver().notifyChange(uri, null);
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        MatchResult match = Table.match(MATCHER.match(uri));
        if (match == null) {
            throw new IllegalArgumentException("Illegal URI: " + uri);
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = db.delete(match.getTable().getBaseName(), selection, selectionArgs);
        Context context = getContext();
        context.getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        MatchResult match = Table.match(MATCHER.match(uri));
        if (match == null) {
            throw new IllegalArgumentException("Illegal URI: " + uri);
        }
        if (match.isItem()) {
            List<String> segments = uri.getPathSegments();
            if (segments == null || segments.isEmpty()) {
                throw new IllegalArgumentException("Malformed URI: " + uri);
            }
            List<String> ids = segments.subList(1, segments.size());
            String selectionById = match.getTable().getSelectionById();
            String[] selectionArgsById = ids.toArray(new String[ids.size()]);
            if (selection == null) {
                selection = selectionById;
                selectionArgs = selectionArgsById;
            } else {
                selection = selectionById + " " + selection;
                selectionArgs = ArrayUtils.concat(selectionArgsById, selectionArgs);
            }
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = db.update(match.getTable().getBaseName(), values, selection, selectionArgs);
        Context context = getContext();
        context.getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] result = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] valuesArray) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            MatchResult match = Table.match(MATCHER.match(uri));
            if (match == null || match.isItem()) {
                throw new IllegalArgumentException("Invalid URI.");
            }
            for (ContentValues values : valuesArray) {
                db.insert(match.getTable().getBaseName(), null, values);
            }
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);
            db.setTransactionSuccessful();
            return valuesArray.length;
        } finally {
            db.endTransaction();
        }
    }

}
