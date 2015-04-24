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

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class AccountSelectionDialogFragment extends DialogFragment {

    private static final String ARG_ACCOUNTS = "accounts";

    private OnAccountSelectedListener mListener;

    public static AccountSelectionDialogFragment newInstance(Account[] accounts) {
        AccountSelectionDialogFragment fragment = new AccountSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(ARG_ACCOUNTS, accounts);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof OnAccountSelectedListener)) {
            throw new RuntimeException(activity.getClass().getSimpleName() +
                    " needs to implement " + OnAccountSelectedListener.class.getSimpleName());
        }
        mListener = (OnAccountSelectedListener) activity;
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Account[] accounts = (Account[]) getArguments().getParcelableArray(ARG_ACCOUNTS);
        return new AlertDialog.Builder(getActivity())
                .setItems(getNames(accounts), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mListener != null) {
                            mListener.onAccountSelected(accounts[i]);
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (mListener != null) {
                            mListener.onCancelAccountSelection();
                        }
                    }
                })
                .create();
    }

    public static String[] getNames(Account[] accounts) {
        String[] names = new String[accounts.length];
        for (int i = 0; i < accounts.length; ++i) {
            names[i] = accounts[i].name;
        }
        return names;
    }

    public interface OnAccountSelectedListener {
        public void onAccountSelected(Account account);
        public void onCancelAccountSelection();
    }

}
