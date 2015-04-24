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

package com.google.android.apps.gutenberg.widget;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

public abstract class RecyclerViewFragment extends Fragment {

    private OnRecyclerViewReadyListener mListener;

    public abstract RecyclerView getRecyclerView();

    public void setOnRecyclerViewReadyListener(OnRecyclerViewReadyListener listener) {
        mListener = listener;
    }

    protected void dispatchOnRecyclerViewReady() {
        if (mListener != null) {
            mListener.onRecyclerViewReady(getRecyclerView());
        }
    }

    public interface OnRecyclerViewReadyListener {
        public void onRecyclerViewReady(RecyclerView rv);
    }

}
