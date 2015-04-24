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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.gutenberg.R;

public class ExplanationHolder extends ViewHolder<Explanation> {

    private TextView mText;

    public ExplanationHolder(LayoutInflater inflater, ViewGroup parent) {
        super(inflater.inflate(R.layout.item_timeline_explanation, parent, false));
        mText = (TextView) itemView.findViewById(R.id.text);
    }

    @Override
    public void bind(Explanation item, ImageLoader loader) {
        mText.setText(item.getEventName());
    }

}
