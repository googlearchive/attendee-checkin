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
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.apps.gutenberg.model.Checkin;
import com.google.android.apps.gutenberg.scanner.ScannerManager;
import com.google.android.apps.gutenberg.scanner.ViewfinderView;
import com.google.android.apps.gutenberg.util.CheckInTask;

import java.io.IOException;

public class ScannerFragment extends Fragment implements TextureView.SurfaceTextureListener,
        ScannerManager.ScanListener {

    private static final String TAG = "ScannerFragment";

    private ScannerManager mScannerManager;
    private ViewfinderView mViewfinder;

    public static ScannerFragment newInstance() {
        return new ScannerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mViewfinder = (ViewfinderView) view.findViewById(R.id.viewfinder);
        TextureView preview = (TextureView) view.findViewById(R.id.preview);
        preview.setSurfaceTextureListener(this);
    }

    @Override
    public void onStop() {
        if (mScannerManager != null) {
            mScannerManager.stop(); // Make sure that the camera is closed
        }
        super.onStop();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mScannerManager = new ScannerManager(getActivity().getApplication());
        mScannerManager.setScreenSize(width, height);
        mScannerManager.setScanListener(this);
        try {
            mScannerManager.start(surface);
            mViewfinder.setScannerManager(mScannerManager);
        } catch (IOException e) {
            Log.e(TAG, "Cannot open camera.", e);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mScannerManager.setScreenSize(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mViewfinder.setScannerManager(null);
        mScannerManager.stop();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    @Override
    public void onScan(String text) {
        final Activity activity = getActivity();
        new CheckInTask(activity, text, GutenbergApplication.from(activity).getEventId(), false,
                new CheckInTask.OnCompleteListener() {
                    @Override
                    public void onComplete(Checkin checkin, int error) {
                        if (checkin == null) {
                            if (error == CheckInTask.ERROR_ALREADY_CHECKED_IN) {
                                Toast.makeText(activity, R.string.already_checked_in,
                                        Toast.LENGTH_SHORT).show();
                            } else if (error == CheckInTask.ERROR_BAD_CHECK_IN) {
                                Toast.makeText(activity, R.string.attendee_not_found,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            if (activity instanceof Listener) {
                                ((Listener) activity).onNewCheckin(checkin);
                            }
                        }
                        mScannerManager.restart();
                    }
                }).execute();
    }

    public interface Listener {
        public void onNewCheckin(Checkin checkin);
    }

}
