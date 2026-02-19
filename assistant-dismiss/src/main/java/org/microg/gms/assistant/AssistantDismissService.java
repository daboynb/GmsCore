/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.assistant;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AssistantDismissService extends AccessibilityService {

    private static final String TAG = "AssistantDismiss";
    private static final String ASSISTANT_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final long INITIAL_DELAY_MS = 2000;
    private static final long RETRY_INTERVAL_MS = 2000;
    private static final int MAX_RETRIES = 5;
    private static final long COOLDOWN_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean assistantVisible = false;
    private long lastDismissTime = 0;
    private int retryCount = 0;

    private final Runnable dismissRunnable = this::tryDismissAssistant;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence packageName = event.getPackageName();
        if (packageName == null) {
            return;
        }

        String pkg = packageName.toString();

        if (ASSISTANT_PACKAGE.equals(pkg)) {
            if (!assistantVisible) {
                assistantVisible = true;
                retryCount = 0;
                Log.d(TAG, "Assistant window detected, starting dismiss sequence");
                handler.removeCallbacks(dismissRunnable);
                handler.postDelayed(dismissRunnable, INITIAL_DELAY_MS);
            }
        } else {
            if (assistantVisible) {
                assistantVisible = false;
                retryCount = 0;
                handler.removeCallbacks(dismissRunnable);
                Log.d(TAG, "Assistant window gone (switched to " + pkg + ")");
            }
        }
    }

    private void tryDismissAssistant() {
        if (!assistantVisible) {
            Log.d(TAG, "Assistant no longer visible, skipping dismiss");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastDismissTime < COOLDOWN_MS) {
            Log.d(TAG, "Cooldown active, skipping dismiss");
            return;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        boolean musicActive = audioManager != null && audioManager.isMusicActive();

        if (!musicActive) {
            retryCount++;
            Log.d(TAG, "Music not playing yet, retry " + retryCount + "/" + MAX_RETRIES);
            if (retryCount < MAX_RETRIES) {
                handler.postDelayed(dismissRunnable, RETRY_INTERVAL_MS);
            } else {
                Log.d(TAG, "Max retries reached, giving up");
                retryCount = 0;
            }
            return;
        }

        Log.i(TAG, "Dismissing Assistant overlay (music is playing)");
        performGlobalAction(GLOBAL_ACTION_BACK);
        lastDismissTime = now;
        assistantVisible = false;
        retryCount = 0;
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(dismissRunnable);
        assistantVisible = false;
        retryCount = 0;
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(dismissRunnable);
        super.onDestroy();
    }
}
