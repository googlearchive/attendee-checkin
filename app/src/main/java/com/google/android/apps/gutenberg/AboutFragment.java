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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AboutFragment extends DialogFragment implements View.OnClickListener {

    private static final String FRAGMENT_LICENSES = "fragment_licenses";
    private static final String FRAGMENT_EULA = "fragment_eula";

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.terms_of_services).setOnClickListener(this);
        view.findViewById(R.id.privacy_policy).setOnClickListener(this);
        view.findViewById(R.id.licenses).setOnClickListener(this);
        view.findViewById(R.id.eula).setOnClickListener(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.app_name);
        return dialog;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.terms_of_services:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/policies/terms/")));
                break;
            case R.id.privacy_policy:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/policies/privacy/")));
                break;
            case R.id.licenses:
                LicensesFragment.newInstance().show(getChildFragmentManager(), FRAGMENT_LICENSES);
                break;
            case R.id.eula:
                EulaFragment.newInstance().show(getChildFragmentManager(), FRAGMENT_EULA);
                break;
        }
    }

}
