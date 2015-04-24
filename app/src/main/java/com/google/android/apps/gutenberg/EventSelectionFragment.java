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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ResourceCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.apps.gutenberg.provider.Table;

public class EventSelectionFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener {

    private static final int LOADER_EVENTS = 1;

    private Spinner mSpinner;
    private TextView mNoEvent;
    private EventAdapter mAdapter;

    private int mTextColorDefault;
    private int mTextColorCurrent;
    private int mTextColorPast;
    private int mTextColorInverse;

    public static EventSelectionFragment newInstance() {
        return new EventSelectionFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources resources = getResources();
        mTextColorDefault = resources.getColor(R.color.primary_text);
        mTextColorCurrent = resources.getColor(R.color.primary_dark);
        mTextColorPast = resources.getColor(R.color.tertiary_text);
        mTextColorInverse = resources.getColor(R.color.primary_text_inverse);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mSpinner = (Spinner) view.findViewById(R.id.spinner);
        mNoEvent = (TextView) view.findViewById(R.id.no_events);
        mSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new EventAdapter();
        mSpinner.setAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_EVENTS, getArguments(), this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_EVENTS: {
                return new CursorLoader(getActivity(), Table.EVENT.getBaseUri(), new String[]{
                        Table.Event._ID, //
                        Table.Event.ID, //
                        Table.Event.NAME, //
                        Table.Event.END_TIME, //
                }, null, null, Table.Event.END_TIME + " DESC");
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_EVENTS: {
                mAdapter.swapCursor(cursor);
                mSpinner.setOnItemSelectedListener(null);
                GutenbergApplication app = GutenbergApplication.from(getActivity());
                String eventId = app.getEventId();
                if (TextUtils.isEmpty(eventId)) {
                    if (cursor.moveToFirst()) {
                        eventId = cursor.getString(cursor.getColumnIndexOrThrow(Table.Event.ID));
                        mSpinner.setSelection(0, false);
                        app.setEventId(eventId);
                    }
                } else {
                    int position = mAdapter.findPosition(eventId);
                    if (position >= 0) {
                        mSpinner.setSelection(position, false);
                    }
                }
                mAdapter.setCurrentEventId(eventId);
                mSpinner.setOnItemSelectedListener(this);
                if (cursor.getCount() == 0) {
                    mNoEvent.setVisibility(View.VISIBLE);
                    mSpinner.setVisibility(View.GONE);
                } else {
                    mNoEvent.setVisibility(View.GONE);
                    mSpinner.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_EVENTS: {
                mAdapter.swapCursor(null);
                break;
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String eventId = (String) view.getTag();
        GutenbergApplication.from(getActivity()).setEventId(eventId);
        mAdapter.setCurrentEventId(eventId);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Ignore
    }

    private class EventAdapter extends ResourceCursorAdapter {

        private String mCurrentEventId;

        public EventAdapter() {
            super(getActivity(), R.layout.item_event_selection, null, 0);
            setDropDownViewResource(R.layout.item_event_selection_dropdown);
        }

        private void setCurrentEventId(String eventId) {
            mCurrentEventId = eventId;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CheckedTextView textView = (CheckedTextView) view;
            textView.setText(cursor.getString(cursor.getColumnIndexOrThrow(Table.Event.NAME)));
            String eventId = cursor.getString(cursor.getColumnIndexOrThrow(Table.Event.ID));
            view.setTag(eventId);
            if (textView.getPaddingLeft() > 0) { // on Dropdown
                long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(Table.Event.END_TIME));
                if (TextUtils.equals(mCurrentEventId, eventId)) {
                    textView.setTextColor(mTextColorCurrent);
                } else if (endTime * 1000 < System.currentTimeMillis()) { // Past
                    textView.setTextColor(mTextColorPast);
                } else {
                    textView.setTextColor(mTextColorDefault);
                }
            } else { // on Toolbar
                textView.setTextColor(mTextColorInverse);
            }
        }

        /**
         * Finds the position of the specified event in this adapter.
         *
         * @param eventId The event ID.
         * @return The position of the specified event, or -1 when not found.
         */
        public int findPosition(String eventId) {
            if (eventId == null) {
                return -1;
            }
            Cursor cursor = getCursor();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                if (TextUtils.equals(eventId,
                        cursor.getString(cursor.getColumnIndexOrThrow(Table.Event.ID)))) {
                    return cursor.getPosition();
                }
            }
            return -1;
        }

    }

}
