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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.gutenberg.animation.FastOutSlowInInterpolator;
import com.google.android.apps.gutenberg.provider.Table;
import com.google.android.apps.gutenberg.util.CheckInTask;
import com.google.android.apps.gutenberg.util.RoundedImageListener;
import com.google.android.apps.gutenberg.widget.RecyclerViewFragment;

/**
 * Shows the list of attendees. Attendees can be filtered by name and check-in status.
 */
public class AttendeeListFragment extends RecyclerViewFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_ONLY_COMING = "only_coming";
    private static final String ARG_EVENT_ID = "event_id";

    private static final int LOADER_ATTENDEES = 1;
    private static final int LOADER_COUNT_ALL_ATTENDEES = 2;
    private static final String FRAGMENT_EDIT_NOTE = "edit_note";

    private AttendeeAdapter mAdapter;

    private ViewHolder mExpandedViewHolder;
    private RecyclerView mRecyclerView;
    private TextView mTextEmptyMessage;

    public static AttendeeListFragment newInstance(boolean onlyComing) {
        AttendeeListFragment fragment = new AttendeeListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_ONLY_COMING, onlyComing);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendee_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mTextEmptyMessage = (TextView) view.findViewById(R.id.empty_message);
        Context context = view.getContext();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new AttendeeAdapter(context);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle args = new Bundle(getArguments());
        args.putString(ARG_EVENT_ID, GutenbergApplication.from(getActivity()).getEventId());
        LoaderManager manager = getLoaderManager();
        manager.initLoader(LOADER_ATTENDEES, args, this);
        if (args.getBoolean(ARG_ONLY_COMING)) {
            manager.initLoader(LOADER_COUNT_ALL_ATTENDEES, args, this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        GutenbergApplication.from(activity).getDefaultSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        GutenbergApplication.from(getActivity()).getDefaultSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onDetach();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        FragmentActivity activity = getActivity();
        String eventId = args.getString(ARG_EVENT_ID);
        if (TextUtils.isEmpty(eventId)) {
            return null;
        }
        String selectionExtra = args.getBoolean(ARG_ONLY_COMING) ?
                " AND " + Table.Attendee.CHECKIN + " IS NULL" : "";
        switch (id) {
            case LOADER_ATTENDEES:
                return new CursorLoader(activity, Table.ATTENDEE.getBaseUri(), new String[]{
                        Table.Attendee._ID,
                        Table.Attendee.ID,
                        Table.Attendee.EVENT_ID,
                        Table.Attendee.EMAIL,
                        Table.Attendee.NAME,
                        Table.Attendee.PLUSID,
                        Table.Attendee.IMAGE_URL,
                        Table.Attendee.CHECKIN,
                        Table.Attendee.CHECKIN_MODIFIED,
                        Table.Attendee.NOTE,
                }, Table.Attendee.EVENT_ID + " = ?" + selectionExtra, new String[]{eventId},
                        Table.Attendee.NAME);
            case LOADER_COUNT_ALL_ATTENDEES:
                return new CursorLoader(activity, Table.ATTENDEE.getBaseUri(), new String[]{
                        "COUNT(*) AS c"
                }, Table.Attendee.EVENT_ID + " = ?", new String[]{eventId}, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ATTENDEES: {
                mAdapter.swapCursor(cursor);
                if (cursor.getCount() == 0) {
                    mRecyclerView.setVisibility(View.GONE);
                    mTextEmptyMessage.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mTextEmptyMessage.setVisibility(View.GONE);
                }
                break;
            }
            case LOADER_COUNT_ALL_ATTENDEES: {
                if (cursor.moveToFirst()) {
                    mTextEmptyMessage.setText(cursor.getInt(0) == 0 ?
                            R.string.no_attendees : R.string.everyone_here);
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ATTENDEES: {
                mAdapter.swapCursor(null);
                break;
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(GutenbergApplication.PREF_EVENT_ID)) {
            Bundle args = new Bundle(getArguments());
            args.putString(ARG_EVENT_ID, prefs.getString(key, null));
            LoaderManager manager = getLoaderManager();
            manager.destroyLoader(LOADER_ATTENDEES);
            manager.initLoader(LOADER_ATTENDEES, args, this);
        }
    }

    @Override
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ImageView mIcon;
        private final ImageView mCheckin;
        private final TextView mName;
        private final TextView mEmail;
        private final TextView mNote;
        private final ImageView mSyncInProcess;
        private final LinearLayout mActions;
        private final ImageView mActionCheck;
        private final ImageView mActionEdit;

        private boolean mAttendeeCheckedIn;
        private String mEventId;
        private String mAttendeeId;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_attendee, parent, false));
            mIcon = (ImageView) itemView.findViewById(R.id.icon);
            mCheckin = (ImageView) itemView.findViewById(R.id.checkin);
            mName = (TextView) itemView.findViewById(R.id.name);
            mEmail = (TextView) itemView.findViewById(R.id.email);
            mNote = (TextView) itemView.findViewById(R.id.note);
            mSyncInProcess = (ImageView) itemView.findViewById(R.id.sync_in_progress);
            mActions = (LinearLayout) itemView.findViewById(R.id.actions);
            mActionCheck = (ImageView) itemView.findViewById(R.id.action_check);
            mActionEdit = (ImageView) itemView.findViewById(R.id.action_edit);
        }

        public void bind(Cursor cursor, ImageLoader imageLoader) {
            itemView.setBackgroundDrawable(null);
            ViewCompat.setTranslationZ(itemView, 0.f);
            mName.setText(cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.NAME)));
            mEmail.setText(cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.EMAIL)));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.NOTE));
            mNote.setVisibility(TextUtils.isEmpty(note) ? View.GONE : View.VISIBLE);
            mNote.setText(note);
            mEventId = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.EVENT_ID));
            mAttendeeId = cursor.getString(cursor.getColumnIndexOrThrow(Table.Attendee.ID));
            mAttendeeCheckedIn = !cursor.isNull(cursor.getColumnIndexOrThrow(Table.Attendee.CHECKIN));
            mCheckin.setVisibility(mAttendeeCheckedIn ? View.VISIBLE : View.INVISIBLE);
            mActionCheck.setImageResource(mAttendeeCheckedIn ?
                    R.drawable.ic_check_green : R.drawable.ic_check_gray);
            boolean modified = 0 != cursor.getInt(cursor.getColumnIndexOrThrow(Table.Attendee.CHECKIN_MODIFIED));
            mSyncInProcess.setVisibility(modified && BuildConfig.DEBUG ?
                    View.VISIBLE : View.INVISIBLE);
            mActions.setVisibility(View.GONE);
            itemView.setOnClickListener(this);
            mActionCheck.setOnClickListener(this);
            mActionEdit.setOnClickListener(this);
            // Icon
            ImageLoader.ImageContainer container = (ImageLoader.ImageContainer)
                    mIcon.getTag();
            if (container != null) {
                container.cancelRequest();
            }
            int columnIndexImageUrl = cursor.getColumnIndexOrThrow(Table.Attendee.IMAGE_URL);
            if (!cursor.isNull(columnIndexImageUrl)) {
                mIcon.setTag(imageLoader.get(cursor.getString(columnIndexImageUrl),
                        new RoundedImageListener(mIcon,
                                R.drawable.ic_person, R.drawable.ic_person)));
            } else {
                mIcon.setImageResource(R.drawable.ic_person);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.attendee:
                    if (mActions.isShown()) {
                        shrink();
                        mExpandedViewHolder = null;
                    } else {
                        if (mExpandedViewHolder != null) {
                            mExpandedViewHolder.shrink();
                        }
                        expand();
                        mExpandedViewHolder = this;
                    }
                    break;
                case R.id.action_check:
                    new CheckInTask(v.getContext(), mAttendeeId,
                            GutenbergApplication.from(v.getContext()).getEventId(),
                            mAttendeeCheckedIn, null).execute();
                    break;
                case R.id.action_edit:
                    EditNoteFragment.newInstance(mEventId, mAttendeeId)
                            .show(getFragmentManager(), FRAGMENT_EDIT_NOTE);
                    break;
            }
        }

        private void expand() {
            ViewCompat.setScaleY(mActions, 0.f);
            mActions.setVisibility(View.VISIBLE);
            ViewCompat.animate(mActions)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .scaleY(1.f)
                    .setListener(null)
                    .start();
            itemView.setBackgroundColor(Color.rgb(0xf8, 0xf8, 0xf8));
            ViewCompat.setTranslationZ(itemView, 8.f);
        }

        private void shrink() {
            ViewCompat.animate(mActions)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .scaleY(0.f)
                    .setListener(new EasyAnimatorListener() {
                        @Override
                        public void onAnimationEnd(View view) {
                            mActions.setVisibility(View.GONE);
                        }
                    }).start();
            itemView.setBackgroundDrawable(null);
            ViewCompat.setTranslationZ(itemView, 0.f);
        }

    }

    private class AttendeeAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final LayoutInflater mInflater;
        private final ImageLoader mImageLoader;
        private Cursor mCursor;

        public AttendeeAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            GutenbergApplication app = GutenbergApplication.from(context);
            mImageLoader = new ImageLoader(app.getRequestQueue(), app.getBitmapCache());
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater, parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (mCursor.getPosition() != position) {
                mCursor.moveToPosition(position);
            }
            holder.bind(mCursor, mImageLoader);
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        public void swapCursor(Cursor cursor) {
            mCursor = cursor;
            notifyDataSetChanged();
        }

    }

    private static abstract class EasyAnimatorListener implements ViewPropertyAnimatorListener {
        @Override
        public void onAnimationStart(View view) {
        }

        @Override
        public void onAnimationCancel(View view) {
        }
    }

}
