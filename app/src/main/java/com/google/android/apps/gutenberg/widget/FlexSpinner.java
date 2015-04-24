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

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class FlexSpinner extends Spinner {

    private static final int ARROW_PADDING = 24; // dp

    public FlexSpinner(Context context) {
        super(context, null);
    }

    public FlexSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlexSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FlexSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
    }

    public FlexSpinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes,
                       int mode) {
        super(context, attrs, defStyleAttr, defStyleRes, mode);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int selectedPosition = getSelectedItemPosition();
        SpinnerAdapter adapter = getAdapter();
        if (selectedPosition >= 0 && adapter != null && selectedPosition < adapter.getCount()) {
            View view = adapter.getView(selectedPosition, null, this);
            measureChild(view, widthMeasureSpec, heightMeasureSpec);
            float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ARROW_PADDING,
                    getResources().getDisplayMetrics());
            setMeasuredDimension(Math.min((int) (view.getMeasuredWidth() + padding),
                    MeasureSpec.getSize(widthMeasureSpec)), getMeasuredHeight());
        }
    }

    @Override
    protected void drawableStateChanged() {
        // Prevent super.drawableStateChanged to change the state of the background drawable
    }

}
