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
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.gutenberg.util.AccountManagerCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;

public class BaseActivity extends ActionBarActivity implements
        AccountSelectionDialogFragment.OnAccountSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "BaseActivity";

    private static final String FRAGMENT_ACCOUNT_SELECTION_DIALOG = "account_selection_dialog";

    private static final int REQUEST_AUTHENTICATE = 0x101;
    private static final int REQUEST_CODE_RESOLVE_ERROR = 0x102;

    private GoogleApiClient mApiClient;
    private ConnectionResult mConnectionResult;
    private ProgressDialog mConnectionProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Google+ Sign in
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage(getString(R.string.signing_in));
        GutenbergApplication app = GutenbergApplication.from(this);
        if (!app.isUserLoggedIn()) {
            selectAccount(false);
        } else {
            app.requestSync(false);
        }
        adjustTaskDescription();
    }

    private void adjustTaskDescription() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(
                    getTitle().toString(),
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_task),
                    getResources().getColor(R.color.primary)));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_AUTHENTICATE: {
                if (RESULT_CANCELED == resultCode) {
                    Toast.makeText(this, getString(R.string.need_to_login),
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else if (RESULT_OK == resultCode) {
                    fetchTokenForAccount(GutenbergApplication.from(this).getAccount());
                }
                break;
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mApiClient.isConnected()) {
            mApiClient.disconnect();
        }
        super.onStop();
    }

    private Account[] getAccounts() {
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        return accountManager.getAccountsByType("com.google");
    }

    protected void selectAccount(boolean force) {
        Account[] accounts = getAccounts();
        if (0 == accounts.length) {
            Toast.makeText(this, getString(R.string.account_required), Toast.LENGTH_SHORT).show();
            finish();
        } else if (1 == accounts.length && !force) {
            fetchTokenForAccount(accounts[0]);
        } else {
            AccountSelectionDialogFragment.newInstance(accounts)
                    .show(getSupportFragmentManager(), FRAGMENT_ACCOUNT_SELECTION_DIALOG);
        }
    }

    private void fetchTokenForAccount(Account account) {
        GutenbergApplication.from(this).setAccount(account);
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        AccountManagerCompat.getAuthToken(accountManager, account, "ah", null, false,
                mAccountManagerCallback, null);
    }

    private AccountManagerCallback<Bundle> mAccountManagerCallback
            = new AccountManagerCallback<Bundle>() {
        @Override
        public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
            try {
                Bundle result = bundleAccountManagerFuture.getResult();
                Intent intent = (Intent) result.get(AccountManager.KEY_INTENT);
                if (intent == null) {
                    String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                    GutenbergApplication app = GutenbergApplication.from(BaseActivity.this);
                    app.setAuthToken(authToken);
                    app.requestSync(false);
                } else {
                    startActivityForResult(intent, REQUEST_AUTHENTICATE);
                }
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                Log.e(TAG, "Error authenticating.", e);
            }
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        mConnectionProgressDialog.dismiss();
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mConnectionProgressDialog.isShowing()) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    mApiClient.connect();
                }
            }
        }
        mConnectionResult = connectionResult;
        signIn();
    }

    @Override
    public void onAccountSelected(Account account) {
        fetchTokenForAccount(account);
    }

    @Override
    public void onCancelAccountSelection() {
        GutenbergApplication app = GutenbergApplication.from(this);
        if (!app.isUserLoggedIn()) {
            finish();
        }
    }

    private void signIn() {
        if (mApiClient.isConnected()) {
            return;
        }
        if (mConnectionResult == null) {
            mConnectionProgressDialog.show();
        } else {
            try {
                mConnectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // Retry connecting
                mConnectionResult = null;
                mApiClient.connect();
            }
        }
    }

}
