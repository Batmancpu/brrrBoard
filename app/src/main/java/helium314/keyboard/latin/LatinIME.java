Warning: truncated output (original token count: 34098)
Total output lines: 2911

/*
 * Copyright (C) 2008 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.latin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Region;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.Trace;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethodSubtype;

import helium314.keyboard.accessibility.AccessibilityUtils;
import helium314.keyboard.compat.ConfigurationCompatKt;
import helium314.keyboard.compat.EditorInfoCompatUtils;
import helium314.keyboard.compat.ImeCompat;
import helium314.keyboard.event.HapticEvent;
import helium314.keyboard.keyboard.KeyboardActionListener;
import helium314.keyboard.keyboard.KeyboardActionListenerImpl;
import helium314.keyboard.keyboard.emoji.EmojiPalettesView;
import helium314.keyboard.keyboard.emoji.BrrSmartEmojiEngine;
import helium314.keyboard.keyboard.internal.KeyboardIconsSet;
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode;
import helium314.keyboard.latin.common.InsetsOutlineProvider;
import helium314.keyboard.dictionarypack.DictionaryPackConstants;
import helium314.keyboard.event.Event;
import helium314.keyboard.event.InputTransaction;
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.keyboard.KeyboardId;
import helium314.keyboard.keyboard.KeyboardLayoutSet;
import helium314.keyboard.keyboard.KeyboardSwitcher;
import helium314.keyboard.keyboard.KlipyPalettesView;
import helium314.keyboard.keyboard.MainKeyboardView;
import helium314.keyboard.keyboard.clipboard.ClipboardHistoryView;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.common.ColorType;
import helium314.keyboard.latin.common.Constants;
import helium314.keyboard.latin.common.CoordinateUtils;
import helium314.keyboard.latin.common.InputPointers;
import helium314.keyboard.latin.common.ViewOutlineProviderUtilsKt;
import helium314.keyboard.latin.define.DebugFlags;
import helium314.keyboard.latin.inputlogic.InputLogic;
import helium314.keyboard.latin.personalization.PersonalizationHelper;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.settings.SettingsValues;
import helium314.keyboard.latin.settings.Defaults;
import helium314.keyboard.latin.settings.SecureAppsPolicy;
import helium314.keyboard.latin.settings.SecureAppTemporaryOptOut;
import helium314.keyboard.latin.suggestions.SuggestionStripView;
import helium314.keyboard.latin.suggestions.AccessoryPriorityPolicy;
import helium314.keyboard.latin.suggestions.SuggestionStripViewAccessor;
import helium314.keyboard.latin.touchinputconsumer.GestureConsumer;
import helium314.keyboard.latin.utils.ColorUtilKt;
import helium314.keyboard.latin.utils.FoldableUtils;
import helium314.keyboard.latin.utils.InlineAutofillUtils;
import helium314.keyboard.latin.utils.InputMethodPickerKt;
import helium314.keyboard.latin.utils.JniUtils;
import helium314.keyboard.latin.utils.KtxKt;
import helium314.keyboard.latin.utils.LeakGuardHandlerWrapper;
import helium314.keyboard.latin.utils.Log;
import helium314.keyboard.latin.utils.RecapitalizeMode;
import helium314.keyboard.latin.utils.StatsUtils;
import helium314.keyboard.latin.utils.StatsUtilsManager;
import helium314.keyboard.latin.utils.SubtypeLocaleUtils;
import helium314.keyboard.latin.utils.SubtypeSettings;
import helium314.keyboard.latin.utils.SubtypeState;
import helium314.keyboard.latin.utils.ToolbarMode;
import helium314.keyboard.settings.SettingsActivity2;
import kotlin.Unit;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

/**
 * Input method implementation for Qwerty'ish keyboard.
 */
public class LatinIME extends InputMethodService implements
        SuggestionStripView.Listener, SuggestionStripViewAccessor,
        DictionaryFacilitator.DictionaryInitializationListener,
        LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    static final String TAG = LatinIME.class.getSimpleName();
    private static final boolean TRACE = false;

    private static LatinIME sInstance;

    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    private final SavedStateRegistryController mSavedStateRegistryController = SavedStateRegistryController.create(this);
    private final ViewModelStore mViewModelStore = new ViewModelStore();

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    @NonNull
    @Override
    public SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistryController.getSavedStateRegistry();
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return mViewModelStore;
    }

    @Nullable
    public static LatinIME getInstance() {
        return sInstance;
    }

    private static final int EXTENDED_TOUCHABLE_REGION_HEIGHT = 100;
    private static final int PERIOD_FOR_AUDIO_AND_HAPTIC_FEEDBACK_IN_KEY_REPEAT = 2;
    private static final int PENDING_IMS_CALLBACK_DURATION_MILLIS = 800;
    static final long DELAY_WAIT_FOR_DICTIONARY_LOAD_MILLIS = TimeUnit.SECONDS.toMillis(2);
    static final long DELAY_DEALLOCATE_MEMORY_MILLIS = TimeUnit.SECONDS.toMillis(10);

    /**
     * The name of the scheme used by the Package Manager to warn of a new package
     * installation,
     * replacement or removal.
     */
    private static final String SCHEME_PACKAGE = "package";

    final Settings mSettings;
    public final KeyboardActionListener mKeyboardActionListener;
    private int mOriginalNavBarColor = 0;
    private int mOriginalNavBarFlags = 0;

    // UIHandler is needed when creating InputLogic
    public final UIHandler mHandler = new UIHandler(this);
    // The old NLNet research collector is retired. GestureConsumer remains the
    // local swipe recognizer and keeps the same production dictionary instance.
    private final DictionaryFacilitator mDictionaryFacilitator =
            DictionaryFacilitatorProvider.getDictionaryFacilitator(false);
    final InputLogic mInputLogic = new InputLogic(this, this, mDictionaryFacilitator);

    // TODO: Move these {@link View}s to {@link KeyboardSwitcher}.
    private View mInputView;
    private final InlineDictation mInlineDictation = new InlineDictation(this);
    private final OfflineBoardDictation mOfflineDictation = new OfflineBoardDictation(this);
    private InsetsOutlineProvider mInsetsUpdater;
    private SuggestionStripView mSuggestionStripView;
    private boolean mInlineProviderViewActive;
    // Only a synchronous first-frame placeholder, never an input/correction source.
    private boolean mStartupSuggestionSeedVisible;
    // A transient visual hold, not a candidate cache or a valid tap target.
    private Runnable mPendingEmptyPredictionClear;
    private long mEditorFocusEpoch;
    private long mInlineRequestEpoch = -1;

    private RichInputMethodManager mRichImm;
    final KeyboardSwitcher mKeyboardSwitcher;
    private final SubtypeState mSubtypeState = new SubtypeState((InputMethodSubtype subtype) -> {
        switchToSubtype(subtype);
        return Unit.INSTANCE;
    });
    private final StatsUtilsManager mStatsUtilsManager;
    // Working variable for {@link #startShowingInputView()} and
    // {@link #onEvaluateInputViewShown()}.
    private boolean mIsExecutingStartShowingInputView;
    private boolean mIsDestroyed = false;

    // Used for re-initialize keyboard layout after onConfigurationChange.
    @Nullable
    private Context mDisplayContext;

    // Object for reacting to adding/removing a dictionary pack.
    private final BroadcastReceiver mDictionaryPackInstallReceiver = new DictionaryPackInstallBroadcastReceiver(this);

    private final BroadcastReceiver mDictionaryDumpBroadcastReceiver = new DictionaryDumpBroadcastReceiver(this);

    FoldableUtils.FoldableObserver foldableObserver;

    final static class RestartAfterDeviceUnlockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // Restart the keyboard if credential encrypted storage is unlocked. This
            // reloads the
            // dictionary and other data from credential-encrypted storage (with the
            // onCreate()
            // method).
            if (Intent.ACTION_USER_UNLOCKED.equals(action)) {
                final int myPid = Process.myPid();
                Log.i(TAG, "Killing my process: pid=" + myPid);
                Process.killProcess(myPid);
            } else {
                Log.e(TAG, "Unexpected intent " + intent);
            }
        }
    }

    final RestartAfterDeviceUnlockReceiver mRestartAfterDeviceUnlockReceiver = new RestartAfterDeviceUnlockReceiver();

    private AlertDialog mOptionsDialog;

    private final boolean mIsHardwareAcceleratedDrawingEnabled;

    private GestureConsumer mGestureConsumer = GestureConsumer.NULL_GESTURE_CONSUMER;

    private final ClipboardHistoryManager mClipboardHistoryManager = new ClipboardHistoryManager(this);
    private final EditorSessionGeneration mEditorSessionGeneration = new EditorSessionGeneration();

    public static final class UIHandler extends LeakGuardHandlerWrapper<LatinIME> {
        private static final int MSG_UPDATE_SHIFT_STATE = 0;
        private static final int MSG_PENDING_IMS_CALLBACK = 1;
        private static final int MSG_UPDATE_SUGGESTION_STRIP = 2;
        private static final int MSG_SHOW_GESTURE_PREVIEW_AND_SET_SUGGESTIONS = 3;
        private static final int MSG_RESUME_SUGGESTIONS = 4;
        private static final int MSG_REOPEN_DICTIONARIES = 5;
        private static final int MSG_UPDATE_TAIL_BATCH_INPUT_COMPLETED = 6;
        private static final int MSG_RESET_CACHES = 7;
        private static final int MSG_WAIT_FOR_DICTIONARY_LOAD = 8;
        private static final int MSG_DEALLOCATE_MEMORY = 9;
        private static final int MSG_SWITCH_LANGUAGE_AUTOMATICALLY = 10;
        // Update this when adding new messages
        private static final int MSG_LAST = MSG_SWITCH_LANGUAGE_AUTOMATICALLY;

        private static final int ARG1_NOT_GESTURE_INPUT = 0;
        private static final int ARG1_DISMISS_GESTURE_FLOATING_PREVIEW_TEXT = 1;
        private static final int ARG1_SHOW_GESTURE_FLOATING_PREVIEW_TEXT = 2;
        private static final int ARG2_UNUSED = 0;
        private static final int ARG1_TRUE = 1;

        private int mDelayInMillisecondsToUpdateSuggestions;
        private int mDelayInMillisecondsToUpdateShiftState;

        public UIHandler(@NonNull final LatinIME ownerInstance) {
            super(ownerInstance);
        }

        public void onCreate() {
            final LatinIME latinIme = getOwnerInstance();
            if (latinIme == null) {
                return;
            }
            final Resources res = latinIme.getResources();
            mDelayInMillisecondsToUpdateSuggestions = res.getInteger(
                    R.integer.config_delay_in_milliseconds_to_update_suggestions);
            mDelayInMillisecondsToUpdateShiftState = res.getInteger(
                    R.integer.config_delay_in_milliseconds_to_update_shift_state);
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            final LatinIME latinIme = getOwnerInstance();
            if (latinIme == null) {
                return;
            }
            switch (msg.what) {
                case MSG_UPDATE_SUGGESTION_STRIP:
                    cancelUpdateSuggestionStrip();
                    if (!(msg.obj instanceof Long)
                            || !latinIme.mEditorSessionGeneration.accepts((Long) msg.obj)) break;
                    latinIme.mInputLogic.performUpdateSuggestionStripAsync(
                            latinIme.mSettings.getCurrent(), msg.arg1 /* inputStyle */);
                    break;
                case MSG_UPDATE_SHIFT_STATE:
                    latinIme.mKeyboardSwitcher.requestUpdatingShiftState(latinIme.getCurrentAutoCapsState(),
                            latinIme.getCurrentRecapitalizeState());
                    break;
                case MSG_SHOW_GESTURE_PREVIEW_AND_SET_SUGGESTIONS:
                    if (msg.arg1 == ARG1_NOT_GESTURE_INPUT) {
                        final SuggestedWords suggestedWords = (SuggestedWords) msg.obj;
                        latinIme.setSuggestedWords(suggestedWords);
                    } else {
                        latinIme.showGesturePreviewAndSetSuggestions((SuggestedWords) msg.obj,
                                msg.arg1 == ARG1_DISMISS_GESTURE_FLOATING_PREVIEW_TEXT);
                    }
                    break;
                case MSG_RESUME_SUGGESTIONS:
                    if (!(msg.obj instanceof Long)
                            || !latinIme.mEditorSessionGeneration.accepts((Long) msg.obj)) break;
                    latinIme.mInputLogic.restartSuggestionsOnWordTouchedByCursor(
                            latinIme.mSettings.getCurrent(),
                            latinIme.mKeyboardSwitcher.getCurrentKeyboardScript());
                    break;
                case MSG_REOPEN_DICTIONARIES:
                    // We need to re-evaluate the currently composing word in case the script has
                    // changed.
                    postWaitForDictionaryLoad();
                    latinIme.resetDictionaryFacilitatorIfNecessary();
                    break;
                case MSG_UPDATE_TAIL_BATCH_INPUT_COMPLETED:
                    final SuggestedWords suggestedWords = (SuggestedWords) msg.obj;
                    latinIme.mInputLogic.onUpdateTailBatchInputCompleted(
                            latinIme.mSettings.getCurrent(),
                            suggestedWords, latinIme.mKeyboardSwitcher);
                    latinIme.onTailBatchInputResultShown(suggestedWords);
                    break;
                case MSG_RESET_CACHES:
                    final SettingsValues settingsValues = latinIme.mSettings.getCurrent();
                    if (latinIme.mInputLogic.retryResetCachesAndReturnSuccess(
                            msg.arg1 == ARG1_TRUE /* tryResumeSuggestions */,
                            msg.arg2 /* remainingTries */, this /* handler */)) {
                        // If we were able to reset the caches, then we can reload the keyboard.
                        // Otherwise, we'll do it when we can.
                        latinIme.mKeyboardSwitcher.reloadMainKeyboard();
                    }
                    break;
                case MSG_WAIT_FOR_DICTIONARY_LOAD:
                    if (msg.obj instanceof Long
                            && latinIme.mEditorSessionGeneration.accepts((Long) msg.obj)) {
                        // No editor content or package identity enters diagnostics.
                        Log.i(TAG, "Timeout waiting for dictionary load");
                    }
                    break;
                case MSG_DEALLOCATE_MEMORY:
                    latinIme.deallocateMemory();
                    break;
                case MSG_SWITCH_LANGUAGE_AUTOMATICALLY:
                    latinIme.switchToSubtype((InputMethodSubtype) msg.obj);
                    break;
            }
        }

        public void postUpdateSuggestionStrip(final int inputStyle) {
            final LatinIME ime = getOwnerInstance();
            if (ime == null) return;
            ime.mInputLogic.invalidateSuggestionRequests();
            if (inputStyle == SuggestedWords.INPUT_STYLE_TYPING) {
                ime.mInputLogic.showImmediateTypedWordIfNeeded(ime.mSettings.getCurrent());
            }
            removeMessages(MSG_UPDATE_SUGGESTION_STRIP);
            // The frame already contains a synchronous prefix-filtered preview.
            // Request the actual dictionary candidates immediately for typing;
            // retain the normal coalescing delay for other update styles.
            sendMessageDelayed(obtainMessage(MSG_UPDATE_SUGGESTION_STRIP, inputStyle,
                    0 /* ignored */, ime.mEditorSessionGeneration.current()),
                    inputStyle == SuggestedWords.INPUT_STYLE_TYPING
                            ? 0 : mDelayInMillisecondsToUpdateSuggestions);
        }

        public void postReopenDictionaries() {
            sendMessage(obtainMessage(MSG_REOPEN_DICTIONARIES));
        }

        public void postResumeSuggestions(final boolean shouldDelay) {
            final LatinIME latinIme = getOwnerInstance();
            if (latinIme == null) {
                return;
            }
            if (!latinIme.mSettings.getCurrent().needsToLookupSuggestions()) {
                return;
            }
            removeMessages(MSG_RESUME_SUGGESTIONS);
            final int message = MSG_RESUME_SUGGESTIONS;
            if (shouldDelay) {
                sendMessageDelayed(obtainMessage(message, latinIme.mEditorSessionGeneration.current()),
                        mDelayInMillisecondsToUpdateSuggestions);
            } else {
                sendMessage(obtainMessage(message, latinIme.mEditorSessionGeneration.current()));
            }
        }

        public long editorGeneration() {
            final LatinIME ime = getOwnerInstance();
            return ime == null ? -1L : ime.mEditorSessionGeneration.current();
        }

        public void postResetCaches(final boolean tryResumeSuggestions, final int remainingTries) {
            removeMessages(MSG_RESET_CACHES);
            sendMessageDelayed(obtainMessage(MSG_RESET_CACHES, tryResumeSuggestions ? 1 : 0,
                    remainingTries, null), 50); // 50ms debounce delay
        }

        public void postWaitForDictionaryLoad() {
            final LatinIME ime = getOwnerInstance();
            if (ime == null) return;
            removeMessages(MSG_WAIT_FOR_DICTIONARY_LOAD);
            sendMessageDelayed(obtainMessage(MSG_WAIT_FOR_DICTIONARY_LOAD,
                    ime.mEditorSessionGeneration.current()), DELAY_WAIT_FOR_DICTIONARY_LOAD_MILLIS);
        }

        public void cancelWaitForDictionaryLoad() {
            removeMessages(MSG_WAIT_FOR_DICTIONARY_LOAD);
        }

        public boolean hasPendingWaitForDictionaryLoad() {
            return hasMessages(MSG_WAIT_FOR_DICTIONARY_LOAD);
        }

        public void cancelUpdateSuggestionStrip() {
            removeMessages(MSG_UPDATE_SUGGESTION_STRIP);
            final LatinIME ime = getOwnerInstance();
            if (ime != null && ime.mInputLogic != null) {
                ime.mInputLogic.invalidateSuggestionRequests();
            }
        }

        public void cancelResumeSuggestions() {
            removeMessages(MSG_RESUME_SUGGESTIONS);
        }

        public boolean hasPendingUpdateSuggestions() {
            return hasMessages(MSG_UPDATE_SUGGESTION_STRIP);
        }

        public boolean hasPendingResumeSuggestions() {
            return hasMessages(MSG_RESUME_SUGGESTIONS);
        }

        public boolean hasPendingReopenDictionaries() {
            return hasMessages(MSG_REOPEN_DICTIONARIES);
        }

        public void postUpdateShiftState() {
            removeMessages(MSG_UPDATE_SHIFT_STATE);
            sendMessageDelayed(obtainMessage(MSG_UPDATE_SHIFT_STATE),
                    mDelayInMillisecondsToUpdateShiftState);
        }

        public void postDeallocateMemory() {
            sendMessageDelayed(obtainMessage(MSG_DEALLOCATE_MEMORY),
                    DELAY_DEALLOCATE_MEMORY_MILLIS);
        }

        public void cancelDeallocateMemory() {
            removeMessages(MSG_DEALLOCATE_MEMORY);
        }

        public boolean hasPendingDeallocateMemory() {
            return hasMessages(MSG_DEALLOCATE_MEMORY);
        }

        public void removeAllMessages() {
            for (int i = 0; i <= MSG_LAST; ++i) {
                removeMessages(i);
            }
        }

        public void showGesturePreviewAndSetSuggestions(final SuggestedWords suggestedWords,
                final boolean dismissGestureFloatingPreviewText) {
            removeMessages(MSG_SHOW_GESTURE_PREVIEW_AND_SET_SUGGESTIONS);
            final int arg1 = dismissGestureFloatingPreviewText
                    ? ARG1_DISMISS_GESTURE_FLOATING_PREVIEW_TEXT
                    : ARG1_SHOW_GESTURE_FLOATING_PREVIEW_TEXT;
            obtainMessage(MSG_SHOW_GESTURE_PREVIEW_AND_SET_SUGGESTIONS, arg1,
                    ARG2_UNUSED, suggestedWords).sendToTarget();
        }

        public void setSuggestions(final SuggestedWords suggestedWords) {
            removeMessages(MSG_SHOW_GESTURE_PREVIEW_AND_SET_SUGGESTIONS);
            obtainMessage(MSG_SHOW_GESTURE_PREVIEW_AND_SET_SUGGESTIONS,
                    ARG1_NOT_GESTURE_INPUT, ARG2_UNUSED, suggestedWords).sendToTarget();
        }

        public void showTailBatchInputResult(final SuggestedWords suggestedWords) {
            obtainMessage(MSG_UPDATE_TAIL_BATCH_INPUT_COMPLETED, suggestedWords).sendToTarget();
        }

        public void postSwitchLanguage(final InputMethodSubtype subtype) {
            obtainMessage(MSG_SWITCH_LANGUAGE_AUTOMATICALLY, subtype).sendToTarget();
        }

        // Working variables for the following methods.
        private boolean mIsOrientationChanging;
        private boolean mPendingSuccessiveImsCallback;
        private boolean mHasPendingStartInput;
        private boolean mHasPendingFinishInputView;
        private boolean mHasPendingFinishInput;
        private EditorInfo mAppliedEditorInfo;

        public void startOrientationChanging() {
            removeMessages(MSG_PENDING_IMS_CALLBACK);
            resetPendingImsCallback();
            mIsOrientationChanging = true;
            final LatinIME latinIme = getOwnerInstance();
            if (latinIme == null) {
                return;
            }
            if (latinIme.isInputViewShown()) {
                latinIme.mKeyboardSwitcher.saveKeyboardState();
            }
        }

        private void resetPendingImsCallback() {
            mHasPendingFinishInputView = false;
            mHasPendingFinishInput = false;
            mHasPendingStartInput = false;
        }

        private void executePendingImsCallback(final LatinIME latinIme, final EditorInfo editorInfo,
                boolean restarting) {
            if (mHasPendingFinishInputView) {
                latinIme.onFinishInputViewInternal(mHasPendingFinishInput);
            }
            if (mHasPendingFinishInput) {
                latinIme.onFinishInputInternal();
            }
            if (mHasPendingStartInput) {
                latinIme.onStartInputInternal(editorInfo, restarting);
            }
            resetPendingImsCallback();
        }

        public void onStartInput(final EditorInfo editorInfo, final boolean restarting) {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                // Typically this is the second onStartInput after orientation changed.
                mHasPendingStartInput = true;
            } else {
                if (mIsOrientationChanging && restarting) {
                    // This is the first onStartInput after orientation changed.
                    mIsOrientationChanging = false;
                    mPendingSuccessiveImsCallback = true;
                }
                final LatinIME latinIme = getOwnerInstance();
                if (latinIme != null) {
                    executePendingImsCallback(latinIme, editorInfo, restarting);
                    latinIme.onStartInputInternal(editorInfo, restarting);
                }
            }
        }

        public void onStartInputView(final EditorInfo editorInfo, final boolean restarting) {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)
                    && KeyboardId.equivalentEditorInfoForKeyboard(editorInfo, mAppliedEditorInfo)) {
                // Typically this is the second onStartInputView after orientation changed.
                resetPendingImsCallback();
            } else {
                if (mPendingSuccessiveImsCallback) {
                    // This is the first onStartInputView after orientation changed.
                    mPendingSuccessiveImsCallback = false;
                    resetPendingImsCallback();
                    sendMessageDelayed(obtainMessage(MSG_PENDING_IMS_CALLBACK),
                            PENDING_IMS_CALLBACK_DURATION_MILLIS);
                }
                final LatinIME latinIme = getOwnerInstance();
                if (latinIme != null) {
                    executePendingImsCallback(latinIme, editorInfo, restarting);
                    latinIme.onStartInputViewInternal(editorInfo, restarting);
                    mAppliedEditorInfo = editorInfo;
                }
                cancelDeallocateMemory();
            }
        }

        public void onFinishInputView(final boolean finishingInput) {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                // Typically this is the first onFinishInputView after orientation changed.
                mHasPendingFinishInputView = true;
            } else {
                final LatinIME latinIme = getOwnerInstance();
                if (latinIme != null) {
                    latinIme.onFinishInputViewInternal(finishingInput);
                    mAppliedEditorInfo = null;
                }
                if (!hasPendingDeallocateMemory()) {
                    postDeallocateMemory();
                }
            }
        }

        public void onFinishInput() {
            if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                // Typically this is the first onFinishInput after orientation changed.
                mHasPendingFinishInput = true;
            } else {
                final LatinIME latinIme = getOwnerInstance();
                if (latinIme != null) {
                    executePendingImsCallback(latinIme, null, false);
                    latinIme.onFinishInputInternal();
                }
            }
        }
    }

    // Loading the native library eagerly to avoid unexpected UnsatisfiedLinkError
    // at the initial
    // JNI call as much as possible.
    static {
        JniUtils.loadNativeLibrary();
    }

    public LatinIME() {
        super();
        mSettings = Settings.getInstance();
        mKeyboardSwitcher = KeyboardSwitcher.getInstance();
        mStatsUtilsManager = StatsUtilsManager.getInstance();
        mKeyboardActionListener = new KeyboardActionListenerImpl(this, mInputLogic);
        mIsHardwareAcceleratedDrawingEnabled = this.enableHardwareAcceleration();
        Log.i(TAG, "Hardware accelerated drawing: " + mIsHardwareAcceleratedDrawingEnabled);
    }

    private static final long FROSTED_LIVE_REDRAW_THROTTLE_MS = 32L;
    private static final long FROSTED_HARD_RESET_DEBOUNCE_MS = 120L;
    private final android.os.Handler mFrostedPrefsHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean mFrostedLiveRedrawScheduled;
    private boolean mFrostedLiveRedrawDirty;
    private long mLastFrostedLiveRedrawUptimeMs;
    private final Runnable mApplyLiveFrostedRedraw = new Runnable() {
        @Override
        public void run() {
            mFrostedLiveRedrawScheduled = false;
            mLastFrostedLiveRedrawUptimeMs = android.os.SystemClock.uptimeMillis();
            mKeyboardSwitcher.updateLiveFrostedGlassColors();
            if (mFrostedLiveRedrawDirty) {
                mFrostedLiveRedrawDirty = false;
                requestFrostedLivePreviewRefresh();
            }
        }
    };
    private final Runnable mApplyHardFrostedReset = new Runnable() {
        @Override
        public void run() {
            forceHardThemeReset();
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key == null) return;
                // Loophole Fix: Differentiate between live redraw and hard reset
                if (helium314.keyboard.latin.settings.Settings.PREF_FROSTED_GLASS_TRIGGER.equals(key)) {
                    Log.d(TAG, "Hard Reset trigger queued.");
                    requestFrostedHardThemeReset();
                } else if (key.startsWith("pref_frosted_")) {
                    Log.d(TAG, "Live Redraw trigger queued for: " + key);
                    requestFrostedLivePreviewRefresh();
                }
            }
        };

    public void requestFrostedLivePreviewRefresh() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mFrostedPrefsHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestFrostedLivePreviewRefresh();
                }
            });
            return;
        }
        if (mFrostedLiveRedrawScheduled) {
            mFrostedLiveRedrawDirty = true;
            return;
        }
        final long now = android.os.SystemClock.uptimeMillis();
        final long elapsed = now - mLastFrostedLiveRedrawUptimeMs;
        final long delay = Math.max(0L, FROSTED_LIVE_REDRAW_THROTTLE_MS - elapsed);
        mFrostedLiveRedrawScheduled = true;
        mFrostedPrefsHandler.postDelayed(mApplyLiveFrostedRedraw, delay);
    }

    public void requestFrostedHardThemeReset() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mFrostedPrefsHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestFrostedHardThemeReset();
                }
            });
            return;
        }
        mFrostedPrefsHandler.removeCallbacks(mApplyLiveFrostedRedraw);
        mFrostedPrefsHandler.removeCallbacks(mApplyHardFrostedReset);
        mFrostedLiveRedrawScheduled = false;
        mFrostedLiveRedrawDirty = false;
        mFrostedPrefsHandler.postDelayed(mApplyHardFrostedReset,
                FROSTED_HARD_RESET_DEBOUNCE_MS);
    }

    public void forceHardThemeReset() {
        // 1. Instantly hide the window to stop user interaction
        hideWindow();

        // 2. Run on Main UI Thread to prevent ConcurrentModificationExceptions
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    // --- PHASE 1: TARGET THE ABSOLUTE BOTTOM WINDOW LAYERS ---
                    android.view.Window window = getWindow().getWindow();
                    if (window != null) {
                        // A. Force the absolute bottom Dialog Window to be 100% Transparent
                        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                        // B. Strip the DecorView (System wrapper layer)
                        android.view.View decorView = window.getDecorView();
                        if (decorView != null) {
                            decorView.setBackground(null);
                        }

                        // C. Strip the Content View (IME root layout wrapper layer)
                        android.view.View contentView = window.findViewById(android.R.id.content);
                        if (contentView != null) {
                            contentView.setBackground(null);
                        }
                    }

                    // --- PHASE 2: SCORCHED EARTH STRIP & GUT (INPUT FRAME) ---
                    // Use internal mInputView for backward compatibility (replaces API 33+ getInputView())
                    if (mInputView != null) {
                        // Recursively crawl upwards and strip backgrounds from EVERY parent frame
                        android.view.View traverseView = mInputView;
                        while (traverseView != null) {
                            traverseView.setBackground(null);
                            if (traverseView.getParent() instanceof android.view.View) {
                                traverseView = (android.view.View) traverseView.getParent();
                            } else {
                                break;
                            }
                        }

                        // Precision removal of the old views from the direct parent container
                        android.view.ViewParent parent = mInputView.getParent();
                        if (parent instanceof android.view.ViewGroup) {
                            android.view.ViewGroup inputContainer = (android.view.ViewGroup) parent;
                            inputContainer.setBackground(null);
                            inputContainer.removeAllViews();
                        }
                    }

                    // --- PHASE 3: PURGE MEMORY AND MOUNT FRESH THEME ---
                    // Flush the old colors from the memory cache
                    helium314.keyboard.keyboard.KeyboardSwitcher.getInstance().forceUpdateKeyboardTheme(LatinIME.this);

                    // Mount the fresh, uncontaminated views
                    // HeliBoard includes suggestions inside onCreateInputView()
                    setInputView(onCreateInputView());
                    updateInputViewShown();

                    // Re-open the keyboard cleanly
                    showWindow(true);

                    // Safety net: force inset re-dispatch after the new view tree is attached,
                    // ensuring nav bar padding is applied on the very first visible frame.
                    if (mInputView != null) {
                        mInputView.post(() -> {
                            if (mInputView != null) {
                                mInputView.requestApplyInsets();
                            }
                        });
                    }

                } catch (Exception e) {
                    android.util.Log.e("LatinIME", "Error during hard theme reset", e);
                }
            }
        });
    }

    @Override
    public void onCreate() {
        sInstance = this;
        mSettings.startListener();
        EmojiPalettesView.AdaptiveEmojiEngine.warmUsageCache(this);
        BrrSmartEmojiEngine.warm(this);
        KeyboardIconsSet.Companion.getInstance().loadIcons(this);
        mRichImm = RichInputMethodManager.getInstance();
        AudioAndHapticFeedbackManager.init(this);
        AccessibilityUtils.init(this);
        mStatsUtilsManager.onCreate(this, mDictionaryFacilitator);
        mDisplayContext = KtxKt.getDisplayContext(this);
        KeyboardSwitcher.init(this);
        super.onCreate();
        mSavedStateRegistryController.performRestore(null);
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        loadSettings();
        mClipboardHistoryManager.onCreate();
        mHandler.onCreate();
        if (FoldableUtils.INSTANCE.isFoldable())
            foldableObserver = new FoldableUtils.FoldableObserver(this);

        // Register to receive ringer mode change.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
        registerReceiver(mRingerModeChangeReceiver, filter);
        // The keyboard may be created while DND is already active, before any
        // broadcast arrives. Seed the feedback policy from the current state.
        updateDoNotDisturbState(this);

        final IntentFilter powerSaveFilter = new IntentFilter();
        powerSaveFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        ContextCompat.registerReceiver(this, mPowerSaveModeChangeReceiver, powerSaveFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);

        // Register to receive installation and removal of a dictionary pack.
        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme(SCHEME_PACKAGE);
        registerReceiver(mDictionaryPackInstallReceiver, packageFilter);

        final IntentFilter newDictFilter = new IntentFilter();
        newDictFilter.addAction(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION);
        // RECEIVER_EXPORTED is necessary because apparently Android 15 (and others?)
        // don't recognize if the sender and receiver are the same app, see
        // https://github.com/HeliBorg/HeliBoard/pull/1756
        ContextCompat.registerReceiver(this, mDictionaryPackInstallReceiver, newDictFilter,
                ContextCompat.RECEIVER_EXPORTED);

        final IntentFilter dictDumpFilter = new IntentFilter();
        dictDumpFilter.addAction(DictionaryDumpBroadcastReceiver.DICTIONARY_DUMP_INTENT_ACTION);
        ContextCompat.registerReceiver(this, mDictionaryDumpBroadcastReceiver, dictDumpFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);

        final IntentFilter restartAfterUnlockFilter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            restartAfterUnlockFilter.addAction(Intent.ACTION_USER_UNLOCKED);
        registerReceiver(mRestartAfterDeviceUnlockReceiver, restartAfterUnlockFilter);

        StatsUtils.onCreate(mSettings.getCurrent(), mRichImm);

        // Register the preference change listener
        KtxKt.prefs(this).registerOnSharedPreferenceChangeListener(mPrefsListener);
    }
…14098 tokens truncated….INSTANCE.switchInputMethod(this))
            return;
        final boolean hasMoreThanOneSubtype = mRichImm.hasMultipleEnabledSubtypesInThisIme(true);
        // switch subtype if wanted, do nothing if no other subtype is available
        if (switchSubtype && !switchIme) {
            if (hasMoreThanOneSubtype)
                // switch to previous subtype if current one was used, otherwise cycle through
                // list
                mSubtypeState.switchSubtype(mRichImm);
            return;
        }
        // language key set to switch both, or language key is not shown on keyboard ->
        // switch both
        if (hasMoreThanOneSubtype && mSubtypeState.getCurrentSubtypeHasBeenUsed()) {
            mSubtypeState.switchSubtype(mRichImm);
            return;
        }
        if (ImeCompat.INSTANCE.shouldSwitchToOtherInputMethods(this)) {
            final InputMethodSubtype nextSubtype = mRichImm.getNextSubtypeInThisIme(false);
            if (nextSubtype != null) {
                switchToSubtype(nextSubtype);
                return;
            } else if (ImeCompat.INSTANCE.switchInputMethod(this)) {
                return;
            }
        }
        mSubtypeState.switchSubtype(mRichImm);
    }

    // Implementation of {@link SuggestionStripView.Listener}.
    @Override
    public void onCodeInput(final int codePoint, final int x, final int y, final boolean isKeyRepeat) {
        mKeyboardActionListener.onCodeInput(codePoint, x, y, isKeyRepeat);
    }

    // This method is public for testability of LatinIME, but also in the future it
    // should
    // completely replace #onCodeInput.
    public void onEvent(@NonNull final Event event) {
        // Keep only the time value: never retain the entered character, editor,
        // package, or language. Private/protected fields are excluded.
        final boolean measureKeyLatency = !event.isFunctionalKeyEvent()
                && !event.isKeyRepeat() && !event.isDead()
                && Character.isLetter(event.getCodePoint())
                && !ClipboardPrivacyPolicy.isPrivateEditor(getCurrentInputEditorInfo(),
                        mSettings.getCurrent().mIncognitoModeEnabled)
                && !SecureAppsPolicy.isSecure(KtxKt.prefs(this),
                        getCurrentInputEditorInfo() == null ? null
                                : getCurrentInputEditorInfo().packageName);
        if (measureKeyLatency) {
            helium314.keyboard.latin.brain.BrainPerformanceMetrics
                    .beginKeyToSuggestionPreDraw();
        }
        if (event.getKeyCode() == KeyCode.ALPHA) {
            final KeyboardSwitcher switcher = KeyboardSwitcher.getInstance();
            // A normal keyboard switch deliberately preserves an active inline
            // Emoji Search panel. Explicit ABC must first leave Search and
            // reparent the typing view, then dismiss the Emoji panel.
            final EmojiPalettesView emojiPanel = switcher.getEmojiPalettesView();
            if (emojiPanel != null && emojiPanel.isSearchMode()) {
                emojiPanel.exitSearchMode(false, false);
            }
            if (switcher.isShowingEmojiPalettes() || switcher.isShowingKlipyPalettes()
                    || switcher.isShowingClipboardHistory() || switcher.isShowingAiWritingTools()) {
                switcher.setAlphabetKeyboard();
                return;
            }
        }
        if (KeyCode.VOICE_INPUT == event.getKeyCode()) {
            toggleBoardDictation();
            return;
        }
        if (event.getKeyCode() == KeyCode.AI_TOOLS || event.getKeyCode() == -214
                || event.getKeyCode() == KeyCode.GIFS || event.getKeyCode() == KeyCode.STICKERS) {
            return;
        }
        final long transactionStarted = measureKeyLatency
                ? helium314.keyboard.latin.brain.BrainPerformanceMetrics.begin() : 0L;
        final InputTransaction completeInputTransaction = mInputLogic.onCodeInput(mSettings.getCurrent(), event,
                mKeyboardSwitcher.getKeyboardShiftMode(),
                mKeyboardSwitcher.getCurrentKeyboardScript(), mHandler);
        if (transactionStarted > 0) {
            helium314.keyboard.latin.brain.BrainPerformanceMetrics.end(
                    helium314.keyboard.latin.brain.BrainPerformanceMetrics.Stage.KEY_TO_INPUT_TRANSACTION,
                    transactionStarted);
        }
        updateStateAfterInputTransaction(completeInputTransaction);
        mKeyboardSwitcher.onEvent(event, getCurrentAutoCapsState(), getCurrentRecapitalizeState());
    }

    public void onTextInput(final String rawText) {
        // TODO: have the keyboard pass the correct key code when we need it.
        final Event event = Event.createSoftwareTextEvent(rawText, KeyCode.MULTIPLE_CODE_POINTS, null);
        final InputTransaction completeInputTransaction = mInputLogic.onTextInput(mSettings.getCurrent(), event,
                mKeyboardSwitcher.getKeyboardShiftMode(), mHandler);
        updateStateAfterInputTransaction(completeInputTransaction);
        mInputLogic.restartSuggestionsOnWordTouchedByCursor(mSettings.getCurrent(),
                mKeyboardSwitcher.getCurrentKeyboardScript());
        mKeyboardSwitcher.onEvent(event, getCurrentAutoCapsState(), getCurrentRecapitalizeState());
    }

    /** Inserts a dictated phrase with native suggestion refresh but without transcript learning. */
    void onPrivateVoiceTextInput(final String text) {
        final Event event = Event.createSoftwareTextEvent(text, KeyCode.MULTIPLE_CODE_POINTS, null);
        final InputTransaction transaction = mInputLogic.onPrivateVoiceTextInput(
                mSettings.getCurrent(), event, mKeyboardSwitcher.getKeyboardShiftMode(), mHandler);
        updateStateAfterInputTransaction(transaction);
        mInputLogic.restartSuggestionsOnWordTouchedByCursor(mSettings.getCurrent(),
                mKeyboardSwitcher.getCurrentKeyboardScript());
        mKeyboardSwitcher.onEvent(event, getCurrentAutoCapsState(), getCurrentRecapitalizeState());
    }

    /** Only a deliberate tap in Clipboard may place an undelivered transcript. */
    public boolean insertRecoveredVoiceText(final String text) {
        if (text == null || text.isEmpty() || !isInputViewShown()
                || getCurrentInputConnection() == null
                || ClipboardPrivacyPolicy.isPrivateEditor(getCurrentInputEditorInfo(),
                        mSettings.getCurrent().mIncognitoModeEnabled)) return false;
        onPrivateVoiceTextInput(text);
        return true;
    }

    /** Manual incognito switch: forget any unfinished cross-mode swipe receipts. */
    void clearPendingPrivateLearning() {
        mEditorSessionGeneration.invalidate();
        mHandler.cancelUpdateSuggestionStrip();
        mHandler.cancelResumeSuggestions();
        mInputLogic.clearPendingPrivateLearning();
    }

    /** Ask for explicit, per-editor consent; never change the saved secure-app list. */
    public void requestSecureAppIncognitoOffConfirmation() {
        final SettingsValues current = Settings.getValues();
        if (current == null || current.mInputAttributes.mNoLearning
                || current.mInputAttributes.mIsPasswordField) return;
        final String packageName = current.mInputAttributes.mTargetApplicationPackageName;
        final SharedPreferences prefs = KtxKt.prefs(this);
        if (!SecureAppsPolicy.isSecure(prefs, packageName)
                || prefs.getBoolean(Settings.PREF_ALWAYS_INCOGNITO_MODE,
                        Defaults.PREF_ALWAYS_INCOGNITO_MODE)) return;
        AudioAndHapticFeedbackManager.getInstance().performSecureAppIncognitoWarning();
        final long generation = editorGeneration();
        if (!(mInputView instanceof InputView inputView)) return;
        // Emoji or a toolbar may be open on the second tap. Mount the normal
        // spacebar first; the consent pill must never float over Emoji cells.
        mKeyboardSwitcher.setAlphabetKeyboard();
        inputView.post(() -> {
            if (generation != editorGeneration() || !isInputViewShown()) return;
            final MainKeyboardView keyboard = mKeyboardSwitcher.getMainKeyboardView();
            inputView.showSlideConfirmation(keyboard, "slide to leave private mode",
                    0xFFE7B557, () -> completeSecureAppOptOut(packageName, generation));
        });
    }

    /** The clipboard's own scroll-end action owns the confirmation request. */
    public void requestClipboardClearConfirmation(final MainKeyboardView keyboard,
            final Runnable onConfirmed) {
        if (!(mInputView instanceof InputView inputView)) return;
        final ClipboardHistoryView panel = inputView.findViewById(R.id.clipboard_history_view);
        if (panel == null || !panel.isShown() || panel.isSearchMode()) return;
        final long generation = editorGeneration();
        inputView.showSlideConfirmation(keyboard, "slide to clear clips", 0xFFCD6975,
                () -> {
                    if (generation == editorGeneration() && isInputViewShown()
                            && panel.isShown() && !panel.isSearchMode()) onConfirmed.run();
                });
    }

    /** Route the older toolbar Clear shortcut through the same consent, too. */
    public void requestClipboardClearFromShortcut() {
        if (!(mInputView instanceof InputView inputView)) return;
        final ClipboardHistoryView panel = inputView.findViewById(R.id.clipboard_history_view);
        if (panel != null && panel.isShown() && !panel.isSearchMode()) {
            panel.requestClearWithSlide();
            return;
        }
        final List<Long> initial = unpinnedClipboardIds();
        if (initial.isEmpty()) return;
        final long generation = editorGeneration();
        mKeyboardSwitcher.setAlphabetKeyboard();
        inputView.post(() -> {
            if (generation != editorGeneration() || !isInputViewShown()) return;
            inputView.showSlideConfirmation(mKeyboardSwitcher.getMainKeyboardView(),
                    "slide to clear clips", 0xFFCD6975, () -> {
                        if (generation == editorGeneration() && isInputViewShown()
                                && initial.equals(unpinnedClipboardIds()))
                            mClipboardHistoryManager.clearHistory();
                    });
        });
    }

    /** IDs only, no private clipboard content copied to telemetry or a second cache. */
    private List<Long> unpinnedClipboardIds() {
        final ArrayList<Long> ids = new ArrayList<>();
        for (int i = 0; i < mClipboardHistoryManager.getHistorySize(); i++) {
            if (mClipboardHistoryManager.canRemove(i)) {
                final ClipboardHistoryEntry clip = mClipboardHistoryManager.getHistoryEntry(i);
                if (clip != null) ids.add(clip.getId());
            }
        }
        return ids;
    }

    private void completeSecureAppOptOut(final String packageName, final long generation) {
        if (generation != editorGeneration() || !isInputViewShown()) return;
        final SettingsValues current = Settings.getValues();
        final SharedPreferences prefs = KtxKt.prefs(this);
        if (current == null || current.mInputAttributes.mNoLearning
                || current.mInputAttributes.mIsPasswordField
                || !packageName.equals(current.mInputAttributes.mTargetApplicationPackageName)
                || !SecureAppsPolicy.isSecure(prefs, packageName)
                || prefs.getBoolean(Settings.PREF_ALWAYS_INCOGNITO_MODE,
                    Defaults.PREF_ALWAYS_INCOGNITO_MODE)) return;
        clearPendingPrivateLearning();
        SecureAppTemporaryOptOut.enableForEditor(packageName);
        loadSettings();
        if (mInputView instanceof InputView inputView) inputView.refreshBoardPrivacy();
        AudioAndHapticFeedbackManager.getInstance().performPrivacyModeConfirmation();
    }

    /** An explicit second toggle re-enters incognito without touching global preferences. */
    public void resumeSecureAppIncognito() {
        if (!SecureAppTemporaryOptOut.applies(
                Settings.getValues().mInputAttributes.mTargetApplicationPackageName)) return;
        clearPendingPrivateLearning();
        SecureAppTemporaryOptOut.clear();
        loadSettings();
        if (mInputView instanceof InputView inputView) inputView.refreshBoardPrivacy();
        AudioAndHapticFeedbackManager.getInstance().performPrivacyModeConfirmation();
    }

    public long editorGeneration() { return mEditorSessionGeneration.current(); }

    public void onStartBatchInput() {
        mInputLogic.onStartBatchInput(mSettings.getCurrent(), mKeyboardSwitcher, mHandler);
        mGestureConsumer.onGestureStarted(mRichImm.getCurrentSubtypeLocale(), mKeyboardSwitcher.getKeyboard());
    }

    public void onUpdateBatchInput(final InputPointers batchPointers) {
        mInputLogic.onUpdateBatchInput(batchPointers);
    }

    public void onEndBatchInput(final InputPointers batchPointers) {
        mInputLogic.onEndBatchInput(batchPointers);
        mGestureConsumer.onGestureCompleted(batchPointers);
    }

    public void onCancelBatchInput() {
        mInputLogic.onCancelBatchInput(mHandler);
        mGestureConsumer.onGestureCanceled();
    }

    public void getKlipySearchGestureSuggestion(final InputPointers batchPointers,
            final Keyboard keyboard, final Suggest.OnGetSuggestedWordsCallback callback) {
        mInputLogic.getGestureSuggestedWordsForExternalInput(batchPointers, keyboard, callback);
    }

    /**
     * To be called after the InputLogic has gotten a chance to act on the suggested
     * words by the
     * IME for the full gesture, possibly updating the TextView to reflect the first
     * suggestion.
     * <p>
     * This method must be run on the UI Thread.
     *
     * @param suggestedWords suggested words by the IME for the full gesture.
     */
    public void onTailBatchInputResultShown(final SuggestedWords suggestedWords) {
        mGestureConsumer.onImeSuggestionsProcessed(suggestedWords,
                mInputLogic.getComposingStart(), mInputLogic.getComposingLength(),
                mDictionaryFacilitator);
    }

    // This method must run on the UI Thread.
    private void showGesturePreviewAndSetSuggestions(@NonNull final SuggestedWords suggestedWords,
            final boolean dismissGestureFloatingPreviewText) {
        if (mIsDestroyed) {
            return;
        }
        setSuggestions(suggestedWords);
        final MainKeyboardView mainKeyboardView = mKeyboardSwitcher.getMainKeyboardView();
        mainKeyboardView.showGestureFloatingPreviewText(suggestedWords,
                dismissGestureFloatingPreviewText /* dismissDelayed */);
    }

    public boolean hasSuggestionStripView() {
        return null != mSuggestionStripView;
    }

    /** Returns only the bounded ranked Emoji snapshot from this editor session. */
    public java.util.List<String> currentContextualEmojiCandidates(
            final EditorInfo editorInfo) {
        if (editorInfo == null || mSuggestionStripView == null) return java.util.Collections.emptyList();
        return mSuggestionStripView.contextualEmojiCandidates(editorInfo);
    }

    private void setSuggestedWords(final SuggestedWords suggestedWords) {
        if (mIsDestroyed) {
            return;
        }
        cancelPendingEmptyPredictionClear();
        final SettingsValues currentSettingsValues = mSettings.getCurrent();
        mInputLogic.setSuggestedWords(suggestedWords);
        // Dictation-independent credentials come from the Android provider. A
        // delayed dictionary refresh must not evict an active autofill chip.
        if (!AccessoryPriorityPolicy.canReplaceVisible(mInlineProviderViewActive,
                AccessoryPriorityPolicy.Incoming.WORDS)) return;
        // TODO: Modify this when we support suggestions with hard keyboard
        if (!hasSuggestionStripView()) {
            return;
        }
        if (!onEvaluateInputViewShown()) {
            return;
        }

        final boolean isEmptyApplicationSpecifiedCompletions = currentSettingsValues
                .isApplicationSpecifiedCompletionsOn()
                && suggestedWords.isEmpty();
        final boolean noSuggestionsFromDictionaries = suggestedWords.isEmpty()
                || suggestedWords.isPunctuationSuggestions()
                || isEmptyApplicationSpecifiedCompletions;

        if (currentSettingsValues.isSuggestionsEnabledPerUserSettings()
                || currentSettingsValues.isApplicationSpecifiedCompletionsOn()
                // We should clear the contextual strip if there is no suggestion from
                // dictionaries.
                || noSuggestionsFromDictionaries) {
            mSuggestionStripView.setSuggestions(suggestedWords,
                    mRichImm.getCurrentSubtype().isRtlSubtype());
            // Auto hide the toolbar if dictionary suggestions are available
            if (currentSettingsValues.mAutoHideToolbar && !noSuggestionsFromDictionaries) {
                mSuggestionStripView.setToolbarVisibility(false);
            }
        }
    }

    @Override
    public void setSuggestions(final SuggestedWords suggestedWords) {
        if (mIsDestroyed) {
            return;
        }
        // A dictionary warming up may emit an empty prediction callback. Keep
        // the already-rendered first-frame row until a real update or typing;
        // never keep it over a completed word or a typing-state transition.
        if (mStartupSuggestionSeedVisible && mInputLogic.getComposingLength() == 0
                && suggestedWords.isEmpty()
                && (suggestedWords.mInputStyle == SuggestedWords.INPUT_STYLE_PREDICTION
                || suggestedWords.mInputStyle ==
                        SuggestedWords.INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION)) return;
        mStartupSuggestionSeedVisible = false;
        if (suggestedWords.isEmpty()) {
            // The decoder has no contextual next word yet. Do not tear down
            // the three-slot row for a transient lookup gap after Space.
            // Old labels are DISPLAY ONLY: their tap tags are revoked now, and
            // they disappear after one bounded settle if no new result arrives.
            final SettingsValues values = mSettings.getCurrent();
            if (mInputLogic.getComposingLength() == 0
                    && (suggestedWords.mInputStyle == SuggestedWords.INPUT_STYLE_PREDICTION
                    || suggestedWords.mInputStyle ==
                            SuggestedWords.INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION)
                    && values.needsToLookupSuggestions()
                    && values.mInputAttributes.mIsGeneralTextInput
                    && !values.mIncognitoModeEnabled
                    && !values.isApplicationSpecifiedCompletionsOn()
                    && mSuggestionStripView != null
                    && mSuggestionStripView.holdUnselectablePredictions()) {
                cancelPendingEmptyPredictionClear();
                mInputLogic.setSuggestedWords(suggestedWords);
                final long generation = mEditorSessionGeneration.current();
                mPendingEmptyPredictionClear = () -> {
                    if (generation != mEditorSessionGeneration.current()) return;
                    mPendingEmptyPredictionClear = null;
                    setNeutralSuggestionStrip();
                };
                mHandler.postDelayed(mPendingEmptyPredictionClear, 164L);
                return;
            }
            // avoids showing clipboard suggestion when starting gesture typing
            // should be fine, as there will be another suggestion in a few ms
            // (but not a great style to avoid this visual glitch, maybe revert this commit
            // and replace with sth better)
            if (suggestedWords.mInputStyle != SuggestedWords.INPUT_STYLE_UPDATE_BATCH)
                setNeutralSuggestionStrip();
        } else {
            setSuggestedWords(suggestedWords);
        }
        // Cache the auto-correction in accessibility code so we can speak it if the
        // user
        // touches a key that will insert it.
        AccessibilityUtils.Companion.getInstance().setAutoCorrection(suggestedWords);
    }

    private void cancelPendingEmptyPredictionClear() {
        if (mPendingEmptyPredictionClear == null) return;
        mHandler.removeCallbacks(mPendingEmptyPredictionClear);
        mPendingEmptyPredictionClear = null;
    }

    @Override
    public void showSuggestionStrip() {
        if (hasSuggestionStripView()) {
            mSuggestionStripView.setToolbarVisibility(false);
        }
    }

    // Called from {@link SuggestionStripView} through the {@link
    // SuggestionStripView#Listener}
    // interface
    @Override
    public void pickSuggestionManually(final SuggestedWordInfo suggestionInfo) {
        // A deliberate tap is observable; it is NOT proof that the prediction was correct.
        // Never count interactions in incognito, password or non-prose editor fields.
        if (helium314.keyboard.keyboard.emoji.EmojiContextEligibility.isEligible(
                getCurrentInputEditorInfo(), mSettings.getCurrent().mIncognitoModeEnabled)) {
            helium314.keyboard.latin.brain.BrainPerformanceMetrics.recordManualTap(
                    suggestionInfo.isEmoji());
        }
        if (suggestionInfo.isEmoji()) {
            final InputTransaction emojiTransaction = mInputLogic.onInsertEmojiSuggestion(
                    mSettings.getCurrent(), suggestionInfo.mWord,
                    mKeyboardSwitcher.getKeyboardShiftMode(), mHandler);
            updateStateAfterInputTransaction(emojiTransaction);
            if (helium314.keyboard.keyboard.emoji.EmojiContextEligibility.isEligible(
                    getCurrentInputEditorInfo(), mSettings.getCurrent().mIncognitoModeEnabled)) {
                final EmojiPalettesView emojiPalettesView = mKeyboardSwitcher.getEmojiPalettesView();
                if (emojiPalettesView != null) {
                    emojiPalettesView.addRecentEmoji(suggestionInfo.mWord);
                } else {
                    // The Emoji panel is lazy. A strip-only Emoji selection
                    // still teaches the SAME context model as an Emoji-page tap.
                    CharSequence preceding = null;
                    try {
                        final android.view.inputmethod.InputConnection connection =
                                getCurrentInputConnection();
                        if (connection != null) preceding = connection.getTextBeforeCursor(160, 0);
                    } catch (RuntimeException ignored) { // Never interrupt the insertion.
                    }
                    BrrSmartEmojiEngine.learn(this, getCurrentInputEditorInfo(),
                            preceding, suggestionInfo.mWord);
                    EmojiPalettesView.AdaptiveEmojiEngine.recordEmojiUsage(this, suggestionInfo.mWord);
                }
            }
            return;
        }
        final InputTransaction completeInputTransaction = mInputLogic.onPickSuggestionManually(
                mSettings.getCurrent(), suggestionInfo,
                mKeyboardSwitcher.getKeyboardShiftMode(),
                mKeyboardSwitcher.getCurrentKeyboardScript(),
                mHandler);
        updateStateAfterInputTransaction(completeInputTransaction);
    }

    /**
     * Checks if a recent clipboard suggestion is available. If available, it is set
     * in suggestion strip.
     * returns whether a clipboard suggestion has been set.
     */
    public boolean tryShowClipboardSuggestion() {
        if (!AccessoryPriorityPolicy.canReplaceVisible(mInlineProviderViewActive,
                AccessoryPriorityPolicy.Incoming.CLIPBOARD)) return false;
        final View clipboardView = mClipboardHistoryManager.getClipboardSuggestionView(getCurrentInputEditorInfo(),
                mSuggestionStripView);
        if (clipboardView != null && hasSuggestionStripView()) {
            mSuggestionStripView.setExternalSuggestionView(clipboardView, false);
            return true;
        }
        return false;
    }

    // This will first try showing a clipboard suggestion. On success, the toolbar
    // will be hidden
    // if the "Auto hide toolbar" is enabled. Otherwise, an empty suggestion strip
    // (if prediction
    // is enabled) or punctuation suggestions (if it's disabled) will be set.
    // Then, the toolbar will be shown automatically if the relevant setting is
    // enabled
    // and there is a selection of text or it's the start of a line.
    @Override
    public void setNeutralSuggestionStrip() {
        if (mInlineProviderViewActive) return;
        final SettingsValues currentSettings = mSettings.getCurrent();
        if (tryShowClipboardSuggestion()) {
            // clipboard suggestion has been set
            if (hasSuggestionStripView() && currentSettings.mAutoHideToolbar)
                mSuggestionStripView.setToolbarVisibility(false);
            return;
        }
        final SuggestedWords neutralSuggestions = currentSettings.mSuggestPunctuation
                ? currentSettings.mPunctuationSuggestions
                : SuggestedWords.getEmptyInstance();
        setSuggestedWords(neutralSuggestions);
        if (hasSuggestionStripView() && currentSettings.mAutoShowToolbar) {
            final int codePointBeforeCursor = mInputLogic.mConnection.getCodePointBeforeCursor();
            if (mInputLogic.mConnection.hasSelection()
                    || codePointBeforeCursor == Constants.NOT_A_CODE
                    || codePointBeforeCursor == Constants.CODE_ENTER) {
                mSuggestionStripView.setToolbarVisibility(true);
            }
        }
    }

    @Override
    public void removeSuggestion(final String word) {
        mDictionaryFacilitator.removeWord(word);
    }

    @Override
    public void removeExternalSuggestions() {
        mInlineProviderViewActive = false;
        setNeutralSuggestionStrip();
        mHandler.postResumeSuggestions(false);
    }

    private void loadKeyboard() {
        // Since we are switching languages, the most urgent thing is to let the
        // keyboard graphics
        // update. LoadKeyboard does that, but we need to wait for buffer flip for it to
        // be on
        // the screen. Anything we do right now will delay this, so wait until the next
        // frame
        // before we do the rest, like reopening dictionaries and updating suggestions.
        // So we
        // post a message.
        mHandler.postReopenDictionaries();
        loadSettings();
        if (mKeyboardSwitcher.getMainKeyboardView() != null) {
            // Reload keyboard because the current language has been changed.
            mKeyboardSwitcher.reloadMainKeyboard();
        }
    }

    /**
     * After an input transaction has been executed, some state must be updated.
     * This includes
     * the shift state of the keyboard and suggestions. This method looks at the
     * finished
     * inputTransaction to find out what is necessary and updates the state
     * accordingly.
     *
     * @param inputTransaction The transaction that has been executed.
     */
    private void updateStateAfterInputTransaction(final InputTransaction inputTransaction) {
        Trace.beginSection("LatinIME#updateStateAfterInput");
        try {
            switch (inputTransaction.getRequiredShiftUpdate()) {
                case InputTransaction.SHIFT_UPDATE_LATER -> mHandler.postUpdateShiftState();
                case InputTransaction.SHIFT_UPDATE_NOW -> mKeyboardSwitcher
                        .requestUpdatingShiftState(getCurrentAutoCapsState(), getCurrentRecapitalizeState());
                default -> {
                } // SHIFT_NO_UPDATE
            }
            if (inputTransaction.requiresUpdateSuggestions()) {
                final int inputStyle;
                if (inputTransaction.getEvent().isSuggestionStripPress()) {
                    // Suggestion strip press: no input.
                    inputStyle = SuggestedWords.INPUT_STYLE_NONE;
                } else if (inputTransaction.getEvent().isGesture()) {
                    inputStyle = SuggestedWords.INPUT_STYLE_TAIL_BATCH;
                } else {
                    inputStyle = SuggestedWords.INPUT_STYLE_TYPING;
                }
                mHandler.postUpdateSuggestionStrip(inputStyle);
            }
            if (inputTransaction.didAffectContents()) {
                mSubtypeState.setCurrentSubtypeHasBeenUsed();
            }
        } finally {
            Trace.endSection();
        }
    }

    public void hapticAndAudioFeedback(final int code, final int repeatCount,
            final HapticEvent hapticEvent) {
        final MainKeyboardView keyboardView = mKeyboardSwitcher.getMainKeyboardView();
        if (keyboardView != null && keyboardView.isInDraggingFinger()) {
            // No need to feedback while finger is dragging.
            return;
        }
        if (repeatCount > 0) {
            // No need to feedback when repeat delete/cursor keys will have no effect.
            switch (code) {
                case KeyCode.DELETE, KeyCode.ARROW_LEFT, KeyCode.ARROW_UP, KeyCode.WORD_LEFT, KeyCode.PAGE_UP:
                    if (!mInputLogic.mConnection.canDeleteCharacters())
                        return;
                    break;
                case KeyCode.ARROW_RIGHT, KeyCode.ARROW_DOWN, KeyCode.WORD_RIGHT, KeyCode.PAGE_DOWN:
                    if (!mInputLogic.mConnection.hasTextAfterCursor())
                        return;
                    break;
            }
            // TODO: Use event time that the last feedback has been generated instead of
            // relying on
            // a repeat count to thin out feedback.
            if (repeatCount % PERIOD_FOR_AUDIO_AND_HAPTIC_FEEDBACK_IN_KEY_REPEAT == 0) {
                return;
            }
        }
        final AudioAndHapticFeedbackManager feedbackManager = AudioAndHapticFeedbackManager.getInstance();

        feedbackManager.performHapticFeedback(keyboardView, hapticEvent);
        feedbackManager.performAudioFeedback(code, hapticEvent);
    }

    // Hooks for hardware keyboard
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mKeyboardSwitcher.isResizeModeActive()) {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK
                && (mKeyboardSwitcher.isShowingKlipyPalettes() || mKeyboardSwitcher.isShowingEmojiPalettes())) {
            return true;
        }
        if (mKeyboardActionListener.onKeyDown(keyCode, keyEvent))
            return true;
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mKeyboardSwitcher.handleResizeOverlayBack()) {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && mKeyboardSwitcher.isShowingEmojiPalettes()) {
            final EmojiPalettesView emojiView = mKeyboardSwitcher.getEmojiPalettesView();
            if (emojiView != null && emojiView.handleBackPress()) {
                return true;
            }
            mKeyboardSwitcher.setAlphabetKeyboard();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && mKeyboardSwitcher.isShowingKlipyPalettes()) {
            final KlipyPalettesView klipyView = mKeyboardSwitcher.getKlipyPalettesView();
            if (klipyView != null) {
                if (!klipyView.handleBackPress()) {
                    mKeyboardSwitcher.setAlphabetKeyboard();
                }
                return true;
            }
        }
        if (mKeyboardActionListener.onKeyUp(keyCode, keyEvent))
            return true;
        return super.onKeyUp(keyCode, keyEvent);
    }

    // onKeyDown and onKeyUp are the main events we are interested in. There are two
    // more events
    // related to handling of hardware key events that we may want to implement in
    // the future:
    // boolean onKeyLongPress(final int keyCode, final KeyEvent event);
    // boolean onKeyMultiple(final int keyCode, final int count, final KeyEvent
    // event);

    // receive ringer mode change.
    private final BroadcastReceiver mRingerModeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action) ||
                    android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED.equals(action)) {
                updateDoNotDisturbState(context);
            }
        }
    };

    private static void updateDoNotDisturbState(final Context context) {
        final boolean dnd;
        try {
            dnd = android.provider.Settings.Global.getInt(
                    context.getContentResolver(), "zen_mode") != 0;
        } catch (android.provider.Settings.SettingNotFoundException e) {
            Log.w(TAG, "zen_mode setting not found, assuming disabled");
            AudioAndHapticFeedbackManager.getInstance().onRingerModeChanged(false);
            return;
        }
        Log.i(TAG, "zen_mode active: " + dnd);
        AudioAndHapticFeedbackManager.getInstance().onRingerModeChanged(dnd);
    }

    private final BroadcastReceiver mPowerSaveModeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (!PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(intent.getAction())) {
                return;
            }
            if (mInputView == null || !isInputViewShown()) {
                return;
            }
            FrostedGlassHelper.configureFrostedGlass(
                    LatinIME.this,
                    mInputView,
                    FrostedGlassHelper.isFrostedTheme(LatinIME.this));
        }
    };

    public ClipboardHistoryManager getClipboardHistoryManager() {
        return mClipboardHistoryManager;
    }

    void launchSettings() {
        mInputLogic.commitTyped(mSettings.getCurrent(), LastComposedWord.NOT_A_SEPARATOR);
        requestHideSelf(0);
        final MainKeyboardView mainKeyboardView = mKeyboardSwitcher.getMainKeyboardView();
        if (mainKeyboardView != null) {
            mainKeyboardView.closing();
        }
        final Intent intent = new Intent();
        intent.setClass(LatinIME.this, SettingsActivity2.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private int mLastKeyboardHeight = 0;

    public void launchEmojiSearch() {
        final EmojiPalettesView emojiPalettesView = mKeyboardSwitcher.getEmojiPalettesView();
        if (emojiPalettesView != null && mKeyboardSwitcher.isShowingEmojiPalettes()) {
            emojiPalettesView.enterSearchMode();
            return;
        }
        mKeyboardSwitcher.setEmojiKeyboard();
        mHandler.postDelayed(() -> {
            final EmojiPalettesView view = mKeyboardSwitcher.getEmojiPalettesView();
            if (view != null) {
                view.enterSearchMode();
            }
        }, 50);
    }

    public boolean isEmojiSearch() {
        final EmojiPalettesView emojiPalettesView = mKeyboardSwitcher.getEmojiPalettesView();
        return emojiPalettesView != null && emojiPalettesView.isSearchMode();
    }

    public void dumpDictionaryForDebug(final String dictName) {
        if (!mDictionaryFacilitator.isActive()) {
            resetDictionaryFacilitatorIfNecessary();
        }
        mDictionaryFacilitator.dumpDictionaryForDebug(dictName);
    }

    public void debugDumpStateAndCrashWithException(final String context) {
        final SettingsValues settingsValues = mSettings.getCurrent();
        String s = settingsValues.toString() + "\nAttributes : " + settingsValues.mInputAttributes +
                "\nContext : " + context;
        throw new RuntimeException(s);
    }

    @Override
    protected void dump(final FileDescriptor fd, final PrintWriter fout, final String[] args) {
        super.dump(fd, fout, args);

        final Printer p = new PrintWriterPrinter(fout);
        p.println("LatinIME state :");
        p.println("  VersionCode = " + BuildConfig.VERSION_CODE);
        p.println("  VersionName = " + BuildConfig.VERSION_NAME);
        final Keyboard keyboard = mKeyboardSwitcher.getKeyboard();
        final int keyboardMode = keyboard != null ? keyboard.mId.mMode : -1;
        p.println("  Keyboard mode = " + keyboardMode);
        final SettingsValues settingsValues = mSettings.getCurrent();
        p.println(settingsValues.dump());
        p.println(mDictionaryFacilitator.dump(this));
    }

    // slightly modified from Simple Keyboard:
    // https://github.com/rkkr/simple-keyboard/blob/master/app/src/main/java/rkr/simplekeyboard/inputmethod/latin/LatinIME.java
    @SuppressWarnings("deprecation")
    private void setNavigationBarColor() {
        final SettingsValues settingsValues = mSettings.getCurrent();
        if (!settingsValues.mCustomNavBarColor)
            return;
        final int color = settingsValues.mColors.get(ColorType.NAVIGATION_BAR);
        final Window window = getWindow().getWindow();
        if (window == null)
            return;
        mOriginalNavBarColor = window.getNavigationBarColor();
        window.setNavigationBarColor(color);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        final View view = window.getDecorView();
        mOriginalNavBarFlags = view.getSystemUiVisibility();
        if (ColorUtilKt.isBrightColor(color)) {
            view.setSystemUiVisibility(mOriginalNavBarFlags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            view.setSystemUiVisibility(mOriginalNavBarFlags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    @SuppressWarnings("deprecation")
    private void clearNavigationBarColor() {
        final SettingsValues settingsValues = mSettings.getCurrent();
        if (!settingsValues.mCustomNavBarColor)
            return;
        final Window window = getWindow().getWindow();
        if (window == null) {
            return;
        }
        window.setNavigationBarColor(mOriginalNavBarColor);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        final View view = window.getDecorView();
        view.setSystemUiVisibility(mOriginalNavBarFlags);
    }

    // On HUAWEI devices with Android 12: a white bar may appear in landscape mode
    // (issue #231)
    // We therefore need to make the color of the status bar transparent
    private void workaroundForHuaweiStatusBarIssue() {
        final Window window = getWindow().getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S && Build.MANUFACTURER.equals("HUAWEI")) {
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @SuppressLint("SwitchIntDef")
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {
            case TRIM_MEMORY_RUNNING_LOW, TRIM_MEMORY_RUNNING_CRITICAL, TRIM_MEMORY_COMPLETE -> {
                KeyboardLayoutSet.onSystemLocaleChanged(); // clears caches, nothing else
                mKeyboardSwitcher.trimMemory();
            }
            // deallocateMemory always called on hiding, and should not be called when
            // showing
        }
    }

}
