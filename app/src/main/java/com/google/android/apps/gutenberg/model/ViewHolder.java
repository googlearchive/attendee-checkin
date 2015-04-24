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

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.gutenberg.R;

public abstract class ViewHolder<TimelineItemType extends TimelineItem>
        extends RecyclerView.ViewHolder {

    protected View mLineAbove;
    protected View mLineBelow;

    public ViewHolder(View itemView) {
        super(itemView);
        mLineAbove = itemView.findViewById(R.id.line_above);
        mLineBelow = itemView.findViewById(R.id.line_below);
    }

    public abstract void bind(TimelineItemType item, ImageLoader loader);

    public void setLines(boolean above, boolean below) {
        mLineAbove.setVisibility(above ? View.VISIBLE : View.INVISIBLE);
        mLineBelow.setVisibility(below ? View.VISIBLE : View.INVISIBLE);
    }

}
