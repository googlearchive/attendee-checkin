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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.gutenberg.model.Checkin;
import com.google.android.apps.gutenberg.model.CheckinHolder;
import com.google.android.apps.gutenberg.model.Explanation;
import com.google.android.apps.gutenberg.model.ExplanationHolder;
import com.google.android.apps.gutenberg.model.TimelineItem;
import com.google.android.apps.gutenberg.model.ViewHolder;
import com.google.android.apps.gutenberg.provider.Table;
import com.google.android.apps.gutenberg.widget.RecyclerViewFragment;

import java.util.ArrayList;

public class TimelineFragment extends RecyclerViewFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int LOADER_ATTENDEES = 1;
    private static final int LOADER_EVENT = 2;

    private static final String ARG_EVENT_ID = "event_id";

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private TimelineAdapter mAdapter;

    public static TimelineFragment newInstance() {
        return new TimelineFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timeline, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mEmptyView = (TextView) view.findViewById(R.id.empty_message);
        Context context = mRecyclerView.getContext();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new TimelineAdapter(context);
        mRecyclerView.setAdapter(mAdapter);
        dispatchOnRecyclerViewReady();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Initialize the loaders
        LoaderManager manager = getLoaderManager();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, GutenbergApplication.from(getActivity()).getEventId());
        manager.initLoader(LOADER_ATTENDEES, args, this);
        manager.initLoader(LOADER_EVENT, args, this);
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
        Activity activity = getActivity();
        String eventId = args.getString(ARG_EVENT_ID);
        if (TextUtils.isEmpty(eventId)) {
            return null;
        }
        switch (id) {
            case LOADER_ATTENDEES:
                return new CursorLoader(activity, Table.ATTENDEE.getBaseUri(), Checkin.PROJECTION,
                        Table.Attendee.EVENT_ID + " = ? AND " +
                                Table.Attendee.CHECKIN + " IS NOT NULL",
                        new String[]{eventId},
                        null);
            case LOADER_EVENT:
                return new CursorLoader(activity, Table.EVENT.getItemUri(eventId),
                        new String[]{Table.Event.NAME}, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ATTENDEES:
                mAdapter.removeAttendees();
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    mAdapter.add(new Checkin(cursor));
                }
                mAdapter.notifyDataSetChanged();
                break;
            case LOADER_EVENT:
                if (cursor.moveToFirst()) {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                    mAdapter.removeExplanations();
                    mAdapter.add(new Explanation(cursor));
                    mAdapter.notifyDataSetChanged();
                } else {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ATTENDEES:
                mAdapter.removeAttendees();
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(GutenbergApplication.PREF_EVENT_ID)) {
            LoaderManager manager = getLoaderManager();
            Bundle args = new Bundle();
            args.putString(ARG_EVENT_ID, prefs.getString(key, null));
            manager.destroyLoader(LOADER_ATTENDEES);
            manager.initLoader(LOADER_ATTENDEES, args, this);
            manager.destroyLoader(LOADER_EVENT);
            manager.initLoader(LOADER_EVENT, args, this);
        }
    }

    public void scrollToTop() {
        if (mRecyclerView != null) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    @Override
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    private static class TimelineAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final int TYPE_EXPLANATION = 1;
        private static final int TYPE_CHECKIN = 2;

        private final LayoutInflater mInflater;
        private final ArrayList<TimelineItem> mItems = new ArrayList<>();
        private final ImageLoader mImageLoader;

        public TimelineAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            GutenbergApplication app = GutenbergApplication.from(context);
            mImageLoader = new ImageLoader(app.getRequestQueue(), app.getBitmapCache());
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            switch (type) {
                case TYPE_EXPLANATION:
                    return new ExplanationHolder(mInflater, parent);
                case TYPE_CHECKIN:
                    return new CheckinHolder(mInflater, parent);
            }
            throw new RuntimeException("Unknown timeline item type: " + type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(mItems.get(position), mImageLoader);
            holder.setLines(position != 0, position != getItemCount() - 1);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            TimelineItem item = mItems.get(position);
            if (item instanceof Explanation) {
                return TYPE_EXPLANATION;
            } else if (item instanceof Checkin) {
                return TYPE_CHECKIN;
            }
            throw new RuntimeException("Unknown timeline item: " + item.getClass().getSimpleName());
        }

        public void removeAttendees() {
            for (int i = mItems.size() - 1; i >= 0; i--) {
                if (mItems.get(i) instanceof Checkin) {
                    mItems.remove(i);
                }
            }
        }

        public void removeExplanations() {
            for (int i = mItems.size() - 1; i >= 0; i--) {
                if (mItems.get(i) instanceof Explanation) {
                    mItems.remove(i);
                }
            }
        }

        public void add(TimelineItem item) {
            boolean added = false;
            for (int i = 0; i < mItems.size(); i++) {
                if (mItems.get(i).getTimestamp() < item.getTimestamp()) {
                    mItems.add(i, item);
                    added = true;
                    break;
                }
            }
            if (!added) {
                mItems.add(item);
            }
        }

    }

}
