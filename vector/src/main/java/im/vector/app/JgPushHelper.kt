/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app


import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import cn.jpush.android.api.JPushInterface
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.DefaultSharedPreferences
import im.vector.app.core.pushers.PushersManager
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber

/**
 * This class store the FCM token in SharedPrefs and ensure this token is retrieved.
 * It has an alter ego in the fdroid variant.
 */
object JgPushHelper {
    private val PREFS_KEY_JPUSH_TOKEN = "JPUSH_TOKEN"

    fun isPushSupported(): Boolean = true

    /**
     * Retrieves the FCM registration token.
     *
     * @return the FCM token or null if not received from FCM
     */
    fun getFcmToken(context: Context): String? {
        return DefaultSharedPreferences.getInstance(context).getString(PREFS_KEY_JPUSH_TOKEN, null)
    }

    /**
     * Store FCM token to the SharedPrefs
     * TODO Store in realm
     *
     * @param context android context
     * @param token   the token to store
     */
    fun storeFcmToken(context: Context,
                      token: String?) {
        DefaultSharedPreferences.getInstance(context).edit {
            putString(PREFS_KEY_JPUSH_TOKEN, token)
        }
    }

    /**
     * onNewToken may not be called on application upgrade, so ensure my shared pref is set
     *
     * @param activity the first launch Activity
     */
    fun ensureFcmTokenIsRetrieved(activity: Activity, pushersManager: PushersManager, registerPusher: Boolean) {
        //        if (TextUtils.isEmpty(getFcmToken(activity))) {
        // 'app should always check the device for a compatible Google Play services APK before accessing Google Play services features'

        var token = JPushInterface.getRegistrationID(activity.applicationContext)
        if(token != null) {
            JgPushHelper.storeFcmToken(activity, token)
            if (registerPusher) {
                pushersManager.enqueueRegisterPusherWithFcmKey(token)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onEnterForeground(context: Context, activeSessionHolder: ActiveSessionHolder) {
        // No op
    }

    @Suppress("UNUSED_PARAMETER")
    fun onEnterBackground(context: Context, vectorPreferences: VectorPreferences, activeSessionHolder: ActiveSessionHolder) {
        // TODO FCM fallback
    }
}

