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

package com.google.android.apps.gutenberg.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

public class AccountManagerCompat {

    public static AccountManagerFuture<Bundle> getAuthToken(AccountManager manager,
                                                            Account account, String authTokenType,
                                                            Bundle options,
                                                            boolean notifyAuthFailure,
                                                            AccountManagerCallback<Bundle> callback,
                                                            Handler handler) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return manager.getAuthToken(account, authTokenType, options, notifyAuthFailure,
                    callback, handler);
        } else {
            //noinspection deprecation
            return manager.getAuthToken(account, authTokenType, notifyAuthFailure, callback,
                    handler);
        }
    }

}
