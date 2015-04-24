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

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.apps.gutenberg.provider.Table;


public class EditNoteFragment extends DialogFragment implements
        View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_ATTENDEE_ID = "attendee_id";

    private static final int LOADER_ATTENDEE = 1;

    private EditText mEditNote;

    public static EditNoteFragment newInstance(String eventId, String attendeeId) {
        EditNoteFragment fragment = new EditNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_ATTENDEE_ID, attendeeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_note, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mEditNote = (EditText) view.findViewById(R.id.note);
        view.findViewById(R.id.ok).setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ATTENDEE, getArguments(), this);
    }

    @Override
    public void onPause() {
        saveNote();
        super.onPause();
    }

    private void saveNote() {
        if (mEditNote == null) {
            return;
        }
        Bundle args = getArguments();
        final String eventId = args.getString(ARG_EVENT_ID);
        final String attendeeId = args.getString(ARG_ATTENDEE_ID);
        final String note = mEditNote.getText().toString();
        final Context context = mEditNote.getContext();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                ContentValues values = new ContentValues();
                values.put(Table.Attendee.NOTE, note);
                int count = context.getContentResolver().update(
                        Table.ATTENDEE.getItemUri(eventId, attendeeId), values, null, null);
                return count > 0;
            }

            @Override
            protected void onPostExecute(Boolean updated) {
                if (!updated) {
                    Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                dismiss();
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String eventId = args.getString(ARG_EVENT_ID);
        String attendeeId = args.getString(ARG_ATTENDEE_ID);
        switch (id) {
            case LOADER_ATTENDEE:
                return new CursorLoader(getActivity(),
                        Table.ATTENDEE.getItemUri(eventId, attendeeId),
                        new String[]{
                                Table.Attendee.NAME,
                                Table.Attendee.NOTE,
                        }, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ATTENDEE:
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    String note = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.NOTE));
                    if (mEditNote != null) {
                        mEditNote.setText(note);
                    }
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.NAME));
                    Dialog dialog = getDialog();
                    if (dialog != null) {
                        dialog.setTitle(name);
                    }
                } else {
                    Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Ignore
    }

}
