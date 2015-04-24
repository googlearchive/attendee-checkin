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

package com.google.android.apps.gutenberg.scanner;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.android.apps.gutenberg.R;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

import java.util.ArrayList;
import java.util.List;

public class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    private static final String TAG = "ViewfinderView";

    private ScannerManager mScannerManager;
    private final Paint mPaint;
    private final int mMaskColor;
    private final int mLaserColor;
    private final int mResultPointColor;
    private int mScannerAlpha;
    private List<ResultPoint> mPossibleResultPoints;
    private List<ResultPoint> mLastPossibleResultPoints;

    public ViewfinderView(Context context) {
        this(context, null);
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewfinderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //
        // Initialize these once for performance rather than calling them every time in onDraw().
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        mMaskColor = resources.getColor(R.color.viewfinder_mask);
        mLaserColor = resources.getColor(R.color.viewfinder_laser);
        mResultPointColor = resources.getColor(R.color.possible_result_points);
        mScannerAlpha = 0;
        mPossibleResultPoints = new ArrayList<>(5);
        mLastPossibleResultPoints = null;
    }

    public void setScannerManager(ScannerManager cameraManager) {
        mScannerManager = cameraManager;
        if (mScannerManager != null) {
            mScannerManager.setResultPointCallback(mResultPointCallback);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mScannerManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = mScannerManager.getFramingRect();
        Rect previewFrame = mScannerManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        mPaint.setColor(mMaskColor);
        canvas.drawRect(0, 0, width, frame.top, mPaint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, mPaint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, mPaint);
        canvas.drawRect(0, frame.bottom + 1, width, height, mPaint);

        // Draw a red "laser scanner" line through the middle to show decoding is active
        mPaint.setColor(mLaserColor);
        mPaint.setAlpha(SCANNER_ALPHA[mScannerAlpha]);
        mScannerAlpha = (mScannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = frame.height() / 2 + frame.top;
        canvas.drawRect(frame.left + 2, middle - 4, frame.right - 1, middle + 4, mPaint);

        float scaleX = frame.width() / (float) previewFrame.width();
        float scaleY = frame.height() / (float) previewFrame.height();

        List<ResultPoint> currentPossible = mPossibleResultPoints;
        List<ResultPoint> currentLast = mLastPossibleResultPoints;
        int frameLeft = frame.left;
        int frameTop = frame.top;
        if (currentPossible.isEmpty()) {
            mLastPossibleResultPoints = null;
        } else {
            mPossibleResultPoints = new ArrayList<>(5);
            mLastPossibleResultPoints = currentPossible;
            mPaint.setAlpha(CURRENT_POINT_OPACITY);
            mPaint.setColor(mResultPointColor);
            for (ResultPoint point : currentPossible) {
                canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                        frameTop + (int) (point.getY() * scaleY),
                        POINT_SIZE, mPaint);
            }
        }
        if (currentLast != null) {
            mPaint.setAlpha(CURRENT_POINT_OPACITY / 2);
            mPaint.setColor(mResultPointColor);
            float radius = POINT_SIZE / 2.0f;
            for (ResultPoint point : currentLast) {
                canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                        frameTop + (int) (point.getY() * scaleY),
                        radius, mPaint);
            }
        }

        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = mPossibleResultPoints;
        points.add(point);
        int size = points.size();
        if (size > MAX_RESULT_POINTS) {
            // trim it
            points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
        }
    }

    private ResultPointCallback mResultPointCallback = new ResultPointCallback() {
        @Override
        public void foundPossibleResultPoint(ResultPoint point) {
            addPossibleResultPoint(point);
        }
    };

}
