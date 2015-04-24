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
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.android.apps.gutenberg.R;
import com.google.android.apps.gutenberg.util.HandlerThreadCompat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.Map;

public class ScannerManager {

    public interface ScanListener {
        public void onScan(String text);
    }

    private static final String TAG = "ScannerManager";
    private static final java.lang.String THREAD_DECODE = "ScannerManager_decode";

    private final Context mContext;

    private Camera mCamera;

    private HandlerThread mDecodeThread;
    private DecodeHandler mDecodeHandler;

    private final ScanHandler mScanHandler;
    private ScanListener mListener;

    private Rect mFramingRect;
    private Rect mFramingRectInPreview;

    private Point mScreenSize;
    private Point mCaptureSize;

    private String mLastResult;

    public ScannerManager(@NonNull Context context) {
        mContext = context;
        mScanHandler = new ScanHandler(this);
    }

    public void setScanListener(ScanListener listener) {
        mListener = listener;
    }

    public void setScreenSize(int width, int height) {
        mScreenSize = new Point(width, height);
        mFramingRect = new Rect((int) (width * 0.2), (int) (height * 0.2),
                (int) (width * 0.8), (int) (height * 0.8));
        mFramingRectInPreview = null; // recalculated later
    }

    public void start(SurfaceTexture texture) throws IOException {
        if (mDecodeThread != null || mCamera != null) {
            throw new IllegalStateException("Already started.");
        }
        mDecodeThread = new HandlerThread(THREAD_DECODE);
        mDecodeThread.start();
        mDecodeHandler = new DecodeHandler(this, mDecodeThread.getLooper(), mScanHandler);
        mCamera = openCamera();
        mCamera.setPreviewTexture(texture);
        mCamera.startPreview();
        restart();
    }

    public void stop() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        if (mDecodeThread != null) {
            HandlerThreadCompat.quitSafely(mDecodeThread);
            mDecodeThread = null;
        }
        if (mDecodeHandler != null) {
            mDecodeHandler = null;
        }
    }

    public void restart() {
        if (mCamera == null) {
            return;
        }
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mCamera == null) {
                    return;
                }
                Camera.Size size = mCamera.getParameters().getPreviewSize();
                Message message = Message.obtain(mDecodeHandler, R.id.message_decode,
                        size.width, size.height, data);
                message.sendToTarget();
            }
        });
    }

    public Rect getFramingRect() {
        return mFramingRect;
    }

    public Rect getFramingRectInPreview() {
        if (mFramingRectInPreview == null) {
            mFramingRectInPreview = new Rect(
                    mFramingRect.left * mCaptureSize.x / mScreenSize.x,
                    mFramingRect.top * mCaptureSize.y / mScreenSize.y,
                    mFramingRect.right * mCaptureSize.x / mScreenSize.x,
                    mFramingRect.bottom * mCaptureSize.y / mScreenSize.y);
        }
        return mFramingRectInPreview;
    }

    public void setResultPointCallback(ResultPointCallback callback) {
        mDecodeHandler.setResultPointCallback(callback);
    }

    private boolean deliverScanResult(String text) {
        if (mListener == null) {
            return false;
        }
        if (TextUtils.equals(mLastResult, text)) {
            return false;
        }
        mLastResult = text;
        mListener.onScan(text);
        return true;
    }

    private Camera openCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int id = chooseBackFacingCamera(info);
        Camera camera = Camera.open(id);
        Point screenResolution = new Point();
        camera.setDisplayOrientation((info.orientation - getDisplayInfo(screenResolution) + 360) % 360);
        Camera.Parameters parameters = camera.getParameters();
        CameraConfigurationUtils.setFocus(parameters, true, true, false);
        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
        CameraConfigurationUtils.setVideoStabilization(parameters);
        CameraConfigurationUtils.setFocusArea(parameters);
        CameraConfigurationUtils.setMetering(parameters);
        mCaptureSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution);
        Log.d(TAG, "Screen resolution: " + screenResolution.x + "x" + screenResolution.y);
        Log.d(TAG, "Preview size: " + mCaptureSize.x + "x" + mCaptureSize.y);
        parameters.setPreviewSize(mCaptureSize.x, mCaptureSize.y);
        camera.setParameters(parameters);
        return camera;
    }

    private int chooseBackFacingCamera(Camera.CameraInfo info) {
        if (info == null) {
            info = new Camera.CameraInfo();
        }
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; ++i) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        throw new RuntimeException("No camera found");
    }

    private int getDisplayInfo(Point size) {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (size != null) {
            display.getSize(size);
        }
        int rotation = display.getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        throw new RuntimeException("Unknown screen rotation: " + rotation);
    }

    private static class ScanHandler extends Handler {

        private final WeakReference<ScannerManager> mManager;

        public ScanHandler(ScannerManager manager) {
            mManager = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message message) {
            ScannerManager manager = mManager.get();
            if (manager == null) {
                return;
            }
            switch (message.what) {
                case R.id.message_scan_succeeded:
                    if (!manager.deliverScanResult((String) message.obj)) {
                        manager.restart();
                    }
                    break;
                case R.id.message_scan_failed:
                    manager.restart();
                    break;
            }
        }
    }

    private static class DecodeHandler extends Handler {

        private final WeakReference<ScannerManager> mManager;
        private final Handler mUiHandler;
        private Map<DecodeHintType, Object> mHints;

        public DecodeHandler(ScannerManager manager, Looper looper, Handler uiHandler) {
            super(looper);
            mManager = new WeakReference<>(manager);
            mUiHandler = uiHandler;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case R.id.message_decode: {
                    String result = decode((byte[]) message.obj, message.arg1, message.arg2);
                    if (result == null) {
                        Message.obtain(mUiHandler, R.id.message_scan_failed)
                                .sendToTarget();
                    } else {
                        Message.obtain(mUiHandler, R.id.message_scan_succeeded, result)
                                .sendToTarget();
                    }
                    break;
                }
            }
        }

        public void setResultPointCallback(ResultPointCallback callback) {
            if (callback == null) {
                mHints = null;
            } else {
                mHints = new EnumMap<>(DecodeHintType.class);
                mHints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, callback);
            }
        }

        private String decode(byte[] data, int width, int height) {
            ScannerManager manager = mManager.get();
            if (manager == null) {
                return null;
            }
            Rect rect = manager.getFramingRectInPreview();
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data,
                    width, height, rect.left, rect.top, rect.right, rect.bottom, false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            try {
                Result result = reader.decode(bitmap, mHints);
                return result.getText();
            } catch (ReaderException e) {
                // Ignore as we will repeatedly decode the preview frame
                return null;
            }
        }
    }

}
