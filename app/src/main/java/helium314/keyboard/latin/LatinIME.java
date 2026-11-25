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
import android.os.UserManager;
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
import helium314.keyboard.latin.brain.BrContextLmExperiment;
import helium314.keyboard.latin.brain.BrSuggestionBrain;
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
import helium314.keyboard.latin.suggestions.ChatGptVoiceSuggestionStability;
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
    private SuggestedWords mPendingChatGptVoiceSuggestions;
    private final Runnable mFlushChatGptVoiceSuggestions = new Runnable() {
        @Override public void run() {
            final long remaining = ChatGptVoiceSuggestionStability.remainingHoldMs(
                    android.os.SystemClock.uptimeMillis());
            if (remaining > 0L) {
                mHandler.postDelayed(this, remaining);
                return;
            }
            final SuggestedWords pending = mPendingChatGptVoiceSuggestions;
            mPendingChatGptVoiceSuggestions = null;
            if (pending != null) setSuggestions(pending);
        }
    };
    private long mEditorFocusEpoch;
    private long mVaultSelectionEpoch;
    private long mVaultUidEditorEpoch = -1;
    private int mVaultCheckedUid = -1;
    private boolean mVaultUidVerified;
    public long getVaultSelectionEpoch() { return mVaultSelectionEpoch; }
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
    private final TextToolsController mTextTools = new TextToolsController(this);
    public void requestContextualTextActions() { mTextTools.request(); }
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
        BrSuggestionBrain.warmEmojiIndex(this);
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
        if (BrContextLmExperiment.isEnabled(this)) {
            BrContextLmExperiment.setEnabled(this, true);
        }
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

    public boolean commitKlipyContent(Uri contentUri, String description, String mimeType) {
        final EditorInfo editorInfo = getCurrentInputEditorInfo();
        final InputConnection inputConnection = getCurrentInputConnection();
        if (editorInfo == null || inputConnection == null || contentUri == null
                || !"content".equals(contentUri.getScheme())) {
            showContentPasteFailedToast();
            return false;
        }

        final String[] contentMimeTypes = getKlipyContentMimeTypes(mimeType);
        final String[] supportedMimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);
        if (!isKlipyContentSupported(supportedMimeTypes, contentMimeTypes)) {
            Log.w(TAG, "Target does not advertise rich content support: package=" + editorInfo.packageName
                    + ", sentMimeTypes=" + Arrays.toString(contentMimeTypes)
                    + ", supportedMimeTypes=" + Arrays.toString(supportedMimeTypes)
                    + ", " + describeRichContentForLog(contentUri));
            showContentPasteFailedToast();
            return false;
        }

        boolean manuallyGranted = false;
        boolean committed = false;
        try {
            final InputContentInfoCompat inputContentInfo = new InputContentInfoCompat(
                    contentUri,
                    new ClipDescription(description, contentMimeTypes),
                    null
            );

            try {
                grantUriPermission(editorInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                manuallyGranted = true;
            } catch (Exception e) {
                Log.w(TAG, "Unable to grant rich content URI (" + e.getClass().getSimpleName() + ")");
            }

            committed = InputConnectionCompat.commitContent(inputConnection, editorInfo, inputContentInfo,
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null);
            if (committed) return true;
            Log.w(TAG, "Target rejected rich content: package=" + editorInfo.packageName
                    + ", sentMimeTypes=" + Arrays.toString(contentMimeTypes)
                    + ", supportedMimeTypes=" + Arrays.toString(supportedMimeTypes)
                    + ", " + describeRichContentForLog(contentUri));
        } catch (Exception e) {
            Log.e(TAG, "Failed to commit rich content: package=" + editorInfo.packageName
                    + ", sentMimeTypes=" + Arrays.toString(contentMimeTypes)
                    + ", supportedMimeTypes=" + Arrays.toString(supportedMimeTypes)
                    + ", " + describeRichContentForLog(contentUri)
                    + ", failureClass=" + e.getClass().getSimpleName());
        } finally {
            // Keep a successful grant available to the target, but do not leave
            // a rejected image exposed to that editor.
            if (manuallyGranted && !committed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    revokeUriPermission(editorInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (RuntimeException e) {
                    Log.w(TAG, "Failed to revoke rejected rich content URI (" + e.getClass().getSimpleName() + ")");
                }
            }
        }

        showContentPasteFailedToast();
        return false;
    }

    private String[] getKlipyContentMimeTypes(final String mimeType) {
        if ("image/webp.wasticker".equals(mimeType)) {
            return new String[]{"image/webp.wasticker", "image/webp"};
        }
        return new String[]{mimeType};
    }

    private boolean isKlipyContentSupported(final String[] supportedMimeTypes, final String[] contentMimeTypes) {
        if (supportedMimeTypes == null) return false;
        for (String contentMimeType : contentMimeTypes) {
            for (String supportedMimeType : supportedMimeTypes) {
                if (ClipDescription.compareMimeTypes(contentMimeType, supportedMimeType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String describeRichContentForLog(final Uri contentUri) {
        // URI paths, query parameters and provider exception messages may contain
        // clipboard data. Log only the scheme, never the raw URI.
        return "uriScheme=" + contentUri.getScheme();
    }

    private void showContentPasteFailedToast() {
        mKeyboardSwitcher.showToast(getString(R.string.toast_msg_content_paste_failed), true);
    }

    private void loadSettings() {
        final Locale locale = mRichImm.getCurrentSubtypeLocale();
        final EditorInfo editorInfo = getCurrentInputEditorInfo();
        final InputAttributes inputAttributes = new InputAttributes(
                editorInfo, isFullscreenMode(), getPackageName(),
                WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), editorInfo));
        mSettings.loadSettings(this, locale, inputAttributes);
        final SettingsValues currentSettingsValues = mSettings.getCurrent();
        AudioAndHapticFeedbackManager.getInstance().onSettingsChanged(currentSettingsValues);
        // This method is called on startup and language switch, before the new layout
        // has
        // been displayed. Opening dictionaries never affects responsivity as
        // dictionaries are
        // asynchronously loaded.
        if (!mHandler.hasPendingReopenDictionaries()) {
            resetDictionaryFacilitatorIfNecessary();
        }
        refreshPersonalizationDictionarySession(currentSettingsValues);
        mInputLogic.mSuggest.clearNextWordSuggestionsCache();
        mInputLogic.updateEmojiDictionary(locale);
        mStatsUtilsManager.onLoadSettings(this, currentSettingsValues);
    }

    private void refreshPersonalizationDictionarySession(
            final SettingsValues currentSettingsValues) {
        if (!currentSettingsValues.mUsePersonalizedDicts) {
            // Remove user history dictionaries.
            PersonalizationHelper.removeAllUserHistoryDictionaries(this);
            mDictionaryFacilitator.clearUserHistoryDictionary(this);
        }
    }

    // Note that this method is called from a non-UI thread.
    @Override
    public void onUpdateMainDictionaryAvailability(final boolean isMainDictionaryAvailable) {
        // Next-word predictions are cached by previous-word context. A result
        // computed before the main dictionary was ready must not survive the
        // transition and mask the newly available lexical candidates. This
        // callback may run off the UI thread; LruCache.evictAll is synchronized.
        mInputLogic.mSuggest.clearNextWordSuggestionsCache();
        final long generation = mHandler.editorGeneration();
        mHandler.post(() -> {
            if (!mEditorSessionGeneration.accepts(generation) || mIsDestroyed) return;
            final MainKeyboardView mainKeyboardView = mKeyboardSwitcher.getMainKeyboardView();
            if (mainKeyboardView != null) {
                mainKeyboardView.setMainDictionaryAvailability(isMainDictionaryAvailable);
            }
            if (mHandler.hasPendingWaitForDictionaryLoad()) {
                mHandler.cancelWaitForDictionaryLoad();
                mHandler.postResumeSuggestions(false /* shouldDelay */);
            }
            // An initial lookup can have returned only the typed prefix while
            // main dictionaries were cold. Re-evaluate the visible editor once
            // the dictionary becomes usable; never reuse a cached blank result.
            if (isMainDictionaryAvailable && mSettings.getCurrent().needsToLookupSuggestions()
                    && !mSettings.getCurrent().isApplicationSpecifiedCompletionsOn()
                    && onEvaluateInputViewShown()) {
                mHandler.postUpdateSuggestionStrip(SuggestedWords.INPUT_STYLE_TYPING);
            }
        });
    }

    void resetDictionaryFacilitatorIfNecessary() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final UserManager userManager = getSystemService(UserManager.class);
            if (userManager == null || !userManager.isUserUnlocked()) {
                // Direct-Boot keyboard sessions can begin before CE is unlocked.
                // Avoid opening any personalized/user dictionary from CE storage.
                return;
            }
        }
        final Locale subtypeSwitcherLocale = mRichImm.getCurrentSubtypeLocale();
        final Locale subtypeLocale;
        if (subtypeSwitcherLocale == null) {
            // This happens in very rare corner cases - for example, immediately after a
            // switch
            // to LatinIME has been requested, about a frame later another switch happens.
            // In this
            // case, we are about to go down but we still don't know it, however the system
            // tells
            // us there is no current subtype.
            Log.e(TAG, "System is reporting no current subtype.");
            subtypeLocale = ConfigurationCompatKt.locale(getResources().getConfiguration());
        } else {
            subtypeLocale = subtypeSwitcherLocale;
        }
        final ArrayList<Locale> locales = new ArrayList<>();
        locales.add(subtypeLocale);
        locales.addAll(mSettings.getCurrent().mSecondaryLocales);
        if (mDictionaryFacilitator.usesSameSettings(
                locales,
                mSettings.getCurrent().mUseContactsDictionary,
                mSettings.getCurrent().mUseAppsDictionary,
                mSettings.getCurrent().mUsePersonalizedDicts)) {
            return;
        }
        resetDictionaryFacilitator(subtypeLocale);
    }

    /**
     * Reset the facilitator by loading dictionaries for the given locale and
     * the current settings values.
     *
     * @param locale the locale
     */
    // TODO: make sure the current settings always have the right locales, and read
    // from them.
    private void resetDictionaryFacilitator(@NonNull final Locale locale) {
        final SettingsValues settingsValues = mSettings.getCurrent();
        try {
            mDictionaryFacilitator.resetDictionaries(this, locale,
                    settingsValues.mUseContactsDictionary, settingsValues.mUseAppsDictionary,
                    settingsValues.mUsePersonalizedDicts, false, "", this);
        } catch (Throwable e) {
            // this should not happen, but in case it does we at least want to show a
            // keyboard
            Log.e(TAG, "Could not reset dictionary facilitator, please fix ASAP", e);
        }
        mInputLogic.mSuggest.setAutoCorrectionThreshold(settingsValues.mAutoCorrectionThreshold);
    }

    /**
     * Reset suggest by loading the main dictionary of the current locale.
     */
    /* package private */ void resetSuggestMainDict() {
        final SettingsValues settingsValues = mSettings.getCurrent();
        mInputLogic.mSuggest.clearNextWordSuggestionsCache();
        mDictionaryFacilitator.resetDictionaries(this, mDictionaryFacilitator.getMainLocale(),
                settingsValues.mUseContactsDictionary, settingsValues.mUseAppsDictionary,
                settingsValues.mUsePersonalizedDicts, true, "", this);
        mKeyboardSwitcher.setThemeNeedsReload(); // necessary for emoji search
        EmojiPalettesView.closeDictionaryFacilitator();
    }

    // used for debug
    public String getLocaleAndConfidenceInfo() {
        return mDictionaryFacilitator.localesAndConfidences();
    }

    @Override
    public void onDestroy() {
        mInlineDictation.cancel();
        mOfflineDictation.cancel();
        mIsDestroyed = true;
        sInstance = null;
        mHandler.removeCallbacksAndMessages(null);
        mInputLogic.onDestroy();
        mClipboardHistoryManager.onDestroy();
        mDictionaryFacilitator.closeDictionaries();
        mSettings.onDestroy();
        if (foldableObserver != null)
            foldableObserver.unregister(this);
        unregisterReceiver(mRingerModeChangeReceiver);
        unregisterReceiver(mPowerSaveModeChangeReceiver);
        unregisterReceiver(mDictionaryPackInstallReceiver);
        unregisterReceiver(mDictionaryDumpBroadcastReceiver);
        unregisterReceiver(mRestartAfterDeviceUnlockReceiver);
        mStatsUtilsManager.onDestroy(this /* context */);
        KtxKt.prefs(this).unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        mFrostedPrefsHandler.removeCallbacksAndMessages(null);
        mFrostedLiveRedrawScheduled = false;
        mFrostedLiveRedrawDirty = false;
        // Proposed safe-check handling for SDK 37 view extraction bugs
        try {
            if (getWindow() != null && getWindow().getWindow() != null) {
                final android.view.View decorView = getWindow().getWindow().getDecorView();
                if (decorView == null || decorView.getParent() == null) {
                    // If the decorator view is already unparented or orphaned, 
                    // manually bypass standard dialog layout cascading triggers if possible
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Pre-empted window lifecycle crash during cleanup", e);
        }

        // Wrap super execution to catch unhandled framework lifecycle exceptions on Android 17
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        mViewModelStore.clear();
        try {
            super.onDestroy();
        } catch (NullPointerException npe) {
            Log.e(TAG, "Swallowed framework NullPointerException during onDestroy view dismissal", npe);
        } catch (Throwable t) {
            Log.e(TAG, "Swallowed framework error during onDestroy view dismissal", t);
        }
        helium314.keyboard.keyboard.KeyboardTheme.setThemeOverride(null);
        deallocateMemory();
    }

    private boolean isImeSuppressedByHardwareKeyboard() {
        final KeyboardSwitcher switcher = KeyboardSwitcher.getInstance();
        return !onEvaluateInputViewShown() && switcher.isImeSuppressedByHardwareKeyboard(
                mSettings.getCurrent(), switcher.getKeyboardSwitchState());
    }

    @Override
    public void onConfigurationChanged(final Configuration conf) {
        SettingsValues settingsValues = mSettings.getCurrent();
        Log.i(TAG, "onConfigurationChanged");
        // Loophole fix: if we are in live preview mode, ignore system configuration changes to avoid flickers or theme fighting
        if (helium314.keyboard.keyboard.KeyboardTheme.getThemeOverride() != null) return;
        SubtypeSettings.INSTANCE.reloadSystemLocales(this);
        if (settingsValues.mDisplayOrientation != conf.orientation) {
            mHandler.startOrientationChanging();
            mInputLogic.onOrientationChange(mSettings.getCurrent());
        }
        if (settingsValues.mHasHardwareKeyboard != Settings.readHasHardwareKeyboard(conf)) {
            // If the state of having a hardware keyboard changed, then we want to reload
            // the
            // settings to adjust for that.
            // TODO: we should probably do this unconditionally here, rather than only when
            // we
            // have a change in hardware keyboard configuration.
            loadSettings();
            if (isImeSuppressedByHardwareKeyboard()) {
                // We call cleanupInternalStateForFinishInput() because it's the right thing to
                // do;
                // however, it seems at the moment the framework is passing us a seemingly valid
                // but actually non-functional InputConnection object. So if this bug ever gets
                // fixed we'll be able to remove the composition, but until it is this code is
                // actually not doing much.
                cleanupInternalStateForFinishInput();
            }
        }
        // KeyboardSwitcher will check by itself if theme update is necessary
        mKeyboardSwitcher.updateKeyboardTheme(KtxKt.getDisplayContext(this));
        super.onConfigurationChanged(conf);
    }

    @Override
    public void onInitializeInterface() {
        mDisplayContext = KtxKt.getDisplayContext(this);
        Log.d(TAG, "onInitializeInterface");
        mKeyboardSwitcher.updateKeyboardTheme(mDisplayContext);
    }

    @Override
    public View onCreateInputView() {
        StatsUtils.onCreateInputView();
        return mKeyboardSwitcher.onCreateInputView(KtxKt.getDisplayContext(this), mIsHardwareAcceleratedDrawingEnabled);
    }

    @Override
    public void setInputView(final View view) {
        mInlineDictation.cancel();
        mOfflineDictation.onEditorLost();
        ViewTreeLifecycleOwner.set(view, this);
        ViewTreeSavedStateRegistryOwner.set(view, this);
        ViewTreeViewModelStoreOwner.set(view, this);
        try {
            if (getWindow() != null && getWindow().getWindow() != null) {
                final View decorView = getWindow().getWindow().getDecorView();
                if (decorView != null) {
                    ViewTreeLifecycleOwner.set(decorView, this);
                    ViewTreeSavedStateRegistryOwner.set(decorView, this);
                    ViewTreeViewModelStoreOwner.set(decorView, this);
                    final int parentPanelId = getResources().getIdentifier("parentPanel", "id", "android");
                    if (parentPanelId != 0) {
                        final View parentPanel = decorView.findViewById(parentPanelId);
                        if (parentPanel != null) {
                            ViewTreeLifecycleOwner.set(parentPanel, this);
                            ViewTreeSavedStateRegistryOwner.set(parentPanel, this);
                            ViewTreeViewModelStoreOwner.set(parentPanel, this);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to set view tree owners on decorView or parentPanel", e);
        }
        super.setInputView(view);
        mInputView = view;
        if (view instanceof InputView) ((InputView) view).bindBoardControls(this);
        mInsetsUpdater = ViewOutlineProviderUtilsKt.setInsetsOutlineProvider(view);
        FrostedGlassHelper.configureFrostedGlass(this, view, FrostedGlassHelper.isFrostedTheme(this));
        updateSuggestionStripView(view);
        // Ensure navigation bar insets are dispatched immediately to the new view tree,
        // preventing the first-frame race condition where the keyboard draws behind the nav bar.
        view.requestApplyInsets();
    }

    public void updateSuggestionStripView(View view) {
        mSuggestionStripView = mSettings.getCurrent().mToolbarMode == ToolbarMode.HIDDEN ? null
                : view.findViewById(R.id.suggestion_strip_view);
        if (hasSuggestionStripView()) {
            mSuggestionStripView.setRtl(mRichImm.getCurrentSubtype().isRtlSubtype());
            mSuggestionStripView.setListener(this, view);
        }
    }

    @Override
    public void setCandidatesView(final View view) {
        // To ensure that CandidatesView will never be set.
    }

    @Override
    public void onStartInput(final EditorInfo editorInfo, final boolean restarting) {
        helium314.keyboard.latin.brain.BrainPerformanceMetrics
                .cancelPendingKeyToSuggestionPreDraw();
        mEditorFocusEpoch++;
        mVaultSelectionEpoch++;
        // Clear transient words at the actual editor boundary, even when
        // settings are reused by a restart or delayed orientation handling.
        mTextTools.dismiss();
        mClipboardHistoryManager.dismissExtraction();
        mClipboardHistoryManager.clearDecryptedHistory();
        mKeyboardActionListener.onCancelDeletePointer();
        mInputLogic.clearVerifiedEdits();
        mInputLogic.clearCorrectionRecall();
        mInputLogic.clearTransientCorrectionMemory();
        mInputLogic.mSuggest.clearNextWordSuggestionsCache();
        mInlineProviderViewActive = false;
        mInlineRequestEpoch = -1;
        mInlineDictation.cancel();
        // Entering a protected editor is a privacy boundary, not a recoverable
        // destination change. Do not keep an old audio decode running there.
        if (WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), editorInfo)
                || helium314.keyboard.latin.settings.AppProfileStore.get(this, editorInfo == null ? null : editorInfo.packageName).getPrivateMode()
                || ClipboardPrivacyPolicy.isPrivateEditor(editorInfo,
                mSettings.getCurrent().mIncognitoModeEnabled)
                || SecureAppsPolicy.isSecure(KtxKt.prefs(this),
                        editorInfo == null ? null : editorInfo.packageName)) {
            if (editorInfo != null) mClipboardHistoryManager.clearRecoveredTranscripts();
            mOfflineDictation.cancel();
        } else mOfflineDictation.onEditorLost();
        mHandler.onStartInput(editorInfo, restarting);
    }

    @Override
    public void onStartInputView(final EditorInfo editorInfo, final boolean restarting) {
        mVaultSelectionEpoch++;
        if (mInputView instanceof InputView) ((InputView) mInputView).resetBoardGesture();
        mHandler.onStartInputView(editorInfo, restarting);
        mStatsUtilsManager.onStartInputView();
        FrostedGlassHelper.configureFrostedGlass(this, mInputView, FrostedGlassHelper.isFrostedTheme(this));
    }

    @Override
    public void onFinishInputView(final boolean finishingInput) {
        mTextTools.dismiss();
        mClipboardHistoryManager.dismissExtraction();
        mClipboardHistoryManager.clearDecryptedHistory();
        mKeyboardActionListener.onCancelDeletePointer();
        mInputLogic.clearVerifiedEdits();
        mInputLogic.clearCorrectionRecall();
        mInputLogic.clearTransientCorrectionMemory();
        mVaultSelectionEpoch++;
        mInlineProviderViewActive = false;
        mInlineRequestEpoch = -1;
        mInlineDictation.cancel();
        mOfflineDictation.onEditorLost();
        if (mInputView instanceof InputView) ((InputView) mInputView).resetBoardGesture();
        StatsUtils.onFinishInputView();
        mHandler.onFinishInputView(finishingInput);
        mStatsUtilsManager.onFinishInputView();
        mGestureConsumer = GestureConsumer.NULL_GESTURE_CONSUMER;
    }

    @Override
    public void onFinishInput() {
        mTextTools.dismiss();
        mClipboardHistoryManager.dismissExtraction();
        mClipboardHistoryManager.clearDecryptedHistory();
        mKeyboardActionListener.onCancelDeletePointer();
        mInputLogic.clearVerifiedEdits();
        mInputLogic.clearCorrectionRecall();
        mInputLogic.clearTransientCorrectionMemory();
        mEditorFocusEpoch++;
        mVaultSelectionEpoch++;
        mInputLogic.mSuggest.clearNextWordSuggestionsCache();
        mInlineProviderViewActive = false;
        mInlineRequestEpoch = -1;
        mInlineDictation.cancel();
        mOfflineDictation.onEditorLost();
        mHandler.onFinishInput();
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(final InputMethodSubtype subtype) {
        // Note that the calling sequence of onCreate() and
        // onCurrentInputMethodSubtypeChanged()
        // is not guaranteed. It may even be called at the same time on a different
        // thread.
        if (subtype.hashCode() == 0x7000000f) {
            // For some reason sometimes the system wants to set the dummy subtype, which
            // messes with the currently enabled subtype.
            // Now that the dummy subtype has a fixed id, we can easily avoid enabling it.
            return;
        }
        InputMethodSubtype oldSubtype = mRichImm.getCurrentSubtype().getRawSubtype();
        if (subtype.equals(oldSubtype)) {
            // onStartInput may be called more than once, resulting in duplicate subtype
            // switches
            return;
        }

        mSubtypeState.onSubtypeChanged(oldSubtype, subtype);
        StatsUtils.onSubtypeChanged(oldSubtype, subtype);
        mRichImm.onSubtypeChanged(subtype);
        mInputLogic.onSubtypeChanged(SubtypeLocaleUtils.getCombiningRulesExtraValue(subtype),
                mSettings.getCurrent());
        loadKeyboard();
        if (hasSuggestionStripView()) {
            mSuggestionStripView.setRtl(mRichImm.getCurrentSubtype().isRtlSubtype());
        }
        mSettings.saveSubtypeForApp(mRichImm.getCurrentSubtype(), getCurrentInputEditorInfo().packageName);
    }

    /**
     * alias to onCurrentInputMethodSubtypeChanged with a better name, as it's also
     * used for internal switching
     */
    public void switchToSubtype(final InputMethodSubtype subtype) {
        if (subtype.hashCode() == 0x7000000f) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                final String imiId = mRichImm.getInputMethodInfoOfThisIme().getId();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchInputMethod(imiId, subtype);
                } else {
                    final android.os.IBinder token = getWindow().getWindow().getAttributes().token;
                    if (token != null) {
                        mRichImm.getInputMethodManager().setInputMethodAndSubtype(token, imiId, subtype);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to switch subtype on system", e);
            }
        }
        onCurrentInputMethodSubtypeChanged(subtype);
    }

    private void onStartInputInternal(final EditorInfo editorInfo, final boolean restarting) {
        SecureAppTemporaryOptOut.clear();
        mEditorSessionGeneration.invalidate();
        mHandler.cancelUpdateSuggestionStrip();
        mHandler.removeCallbacks(mFlushChatGptVoiceSuggestions);
        mPendingChatGptVoiceSuggestions = null;
        mHandler.cancelResumeSuggestions();
        super.onStartInput(editorInfo, restarting);
        // A booted IME may have been created before user unlock; retry the
        // encrypted Emoji-profile load on the first real post-unlock editor.
        BrSuggestionBrain.warmEmojiIndex(this);

        final RichInputMethodSubtype subtypeForApp = editorInfo == null
                ? null
                : mSettings.getSubtypeForApp(editorInfo.packageName);
        final List<Locale> hintLocales = EditorInfoCompatUtils.getHintLocales(editorInfo);
        final InputMethodSubtype subtypeForLocales = mSubtypeState.getSubtypeForLocales(mRichImm, hintLocales,
                subtypeForApp);
        if (subtypeForLocales != null) {
            // found a better subtype using hint locales and saved-per-app subtype, that we
            // should switch to.
            mHandler.postSwitchLanguage(subtypeForLocales);
        }
    }

    void onStartInputViewInternal(final EditorInfo editorInfo, final boolean restarting) {
        // A same-package EditorInfo restart can skip the ordinary inputTypeChanged
        // branch. Revoke RAM-only consent here too, even when the system never
        // sends onStartInputInternal between two visible editor sessions.
        SecureAppTemporaryOptOut.clear();
        mStartupSuggestionSeedVisible = false;
        cancelPendingEmptyPredictionClear();
        mKeyboardSwitcher.cancelPanelTransition();
        super.onStartInputView(editorInfo, restarting);

        // Never switch an editor into the retired gesture-trace research mode.
        mDictionaryFacilitator.onStartInput();
        // Switch to the null consumer to handle cases leading to early exit below, for
        // which we
        // also wouldn't be consuming gesture data.
        mGestureConsumer = GestureConsumer.NULL_GESTURE_CONSUMER;
        mRichImm.refreshSubtypeCaches();
        final KeyboardSwitcher switcher = mKeyboardSwitcher;
        // A new EditorInfo/restart is a new Emoji navigation session. Do not
        // clear Recents; ABC<->Emoji taps inside this session still retain rows.
        switcher.beginEmojiEditorSession();

        // If we are starting input in a different text field from before, we'll have to
        // reload
        // settings, so currentSettingsValues can't be final.
        SettingsValues currentSettingsValues = mSettings.getCurrent();
        final boolean secureEditor = SecureAppsPolicy.isSecure(KtxKt.prefs(this),
                editorInfo == null ? null : editorInfo.packageName);
        if (secureEditor && !currentSettingsValues.mIncognitoModeEnabled)
            mInputLogic.clearPendingPrivateLearning();
        boolean inputTypeChanged = !currentSettingsValues.isSameInputType(editorInfo)
                // Two apps can focus identical editor types during an IME
                // restart. Secure-app membership must be recomputed for each
                // target package, not inherited from the previous editor.
                || !java.util.Objects.equals(
                        currentSettingsValues.mInputAttributes.mTargetApplicationPackageName,
                        editorInfo == null ? null : editorInfo.packageName);
        if (inputTypeChanged) mInputLogic.clearPendingPrivateLearning();
        boolean isDifferentTextField = !restarting || inputTypeChanged || secureEditor
                || currentSettingsValues.mInputAttributes.mVaultPrivateEditor
                    != WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), editorInfo);

        // we want to reload the settings before calling updateKeyboardTheme, because
        // updateKeyboardTheme reads SettingsValues.mToolbarMode
        if (isDifferentTextField || currentSettingsValues.mAppProfileRevision != helium314.keyboard.latin.settings.AppProfileStore.revision()
                || !currentSettingsValues.hasSameOrientation(getResources().getConfiguration())) {
            loadSettings();
            if (hasSuggestionStripView())
                mSuggestionStripView.updateVoiceKey();
        }

        switcher.updateKeyboardTheme(mDisplayContext);
        // The input frame can outlive a focused editor. Reapply the freshly loaded
        // theme surface so a recycled background never keeps a stale color filter.
        if (mInputView instanceof InputView inputView) {
            inputView.refreshBoardFrameBackground();
        }
        MainKeyboardView mainKeyboardView = switcher.getMainKeyboardView();
        currentSettingsValues = mSettings.getCurrent(); // settingsValues may have been reloaded
        if (mInputView instanceof InputView inputView) inputView.refreshBoardPrivacy();

        if (editorInfo == null) {
            Log.e(TAG, "Null EditorInfo in onStartInputView()");
            if (DebugFlags.DEBUG_ENABLED) {
                throw new NullPointerException("Null EditorInfo in onStartInputView()");
            }
            return;
        }
        // Load the target app's secure/incognito settings BEFORE reading any
        // system clipboard data. The previous editor's privacy state is stale.
        mClipboardHistoryManager.updatePrimaryClip();
        Log.i(TAG, (restarting ? "Res" : "S") + "tarting input. Cursor position = " + editorInfo.initialSelStart + ","
                + editorInfo.initialSelEnd);
        if (DebugFlags.DEBUG_ENABLED) {
            EditorInfoCompatUtils.INSTANCE.debugLog(editorInfo, TAG);
        }

        // In landscape mode, this method gets called without the input view being
        // created.
        if (mainKeyboardView == null) {
            return;
        }

        // Update to a gesture consumer with the current editor and IME state.
        mGestureConsumer = GestureConsumer.newInstance(editorInfo,
                mInputLogic.getPrivateCommandPerformer(),
                mRichImm.getCurrentSubtypeLocale(),
                switcher.getKeyboard());

        // Forward this event to the accessibility utilities, if enabled.
        final AccessibilityUtils accessUtils = AccessibilityUtils.Companion.getInstance();
        if (accessUtils.isTouchExplorationEnabled()) {
            accessUtils.onStartInputViewInternal(mainKeyboardView, editorInfo, restarting);
        }

        StatsUtils.onStartInputView(editorInfo.inputType,
                Settings.getValues().mDisplayOrientation,
                !isDifferentTextField);

        // The EditorInfo might have a flag that affects fullscreen mode.
        // Note: This call should be done by InputMethodService?
        updateFullscreenMode();

        // ALERT: settings have not been reloaded and there is a chance they may be
        // stale.
        // In the practice, if it is, we should have gotten onConfigurationChanged so it
        // should
        // be fine, but this is horribly confusing and must be fixed AS SOON AS
        // POSSIBLE.

        // In some cases the input connection has not been reset yet and we can't access
        // it. In
        // this case we will need to call loadKeyboard() later, when it's accessible, so
        // that we
        // can go into the correct mode, so we need to do some housekeeping here.
        final boolean needToCallLoadKeyboardLater;
        final Suggest suggest = mInputLogic.mSuggest;
        if (!isImeSuppressedByHardwareKeyboard()) {
            // The app calling setText() has the effect of clearing the composing
            // span, so we should reset our state unconditionally, even if restarting is
            // true.
            // We also tell the input logic about the combining rules for the current
            // subtype, so
            // it can adjust its combiners if needed.
            mInputLogic.startInput(mRichImm.getCombiningRulesExtraValueOfCurrentSubtype(), currentSettingsValues);

            resetDictionaryFacilitatorIfNecessary();

            // TODO[IL]: Can the following be moved to InputLogic#startInput?
            if (!mInputLogic.mConnection.resetCachesUponCursorMoveAndReturnSuccess(
                    editorInfo.initialSelStart, editorInfo.initialSelEnd,
                    false /* shouldFinishComposition */)) {
                // Sometimes, while rotating, for some reason the framework tells the app we are
                // not
                // connected to it and that means we can't refresh the cache. In this case,
                // schedule
                // a refresh later.
                // We try resetting the caches up to 5 times before giving up.
                mHandler.postResetCaches(isDifferentTextField, 5 /* remainingTries */);
                // mLastSelection{Start,End} are reset later in this method, no need to do it
                // here
                needToCallLoadKeyboardLater = true;
            } else {
                // When rotating, and when input is starting again in a field from where the
                // focus
                // didn't move (the keyboard having been closed with the back key),
                // initialSelStart and initialSelEnd sometimes are lying. Make a best effort to
                // work around this bug.
                mInputLogic.mConnection.tryFixIncorrectCursorPosition();
                if (mInputLogic.mConnection.isCursorTouchingWord(currentSettingsValues.mSpacingAndPunctuations, true)) {
                    mHandler.postResumeSuggestions(true /* shouldDelay */);
                }
                needToCallLoadKeyboardLater = false;
            }
        } else {
            // If we have a hardware keyboard we don't need to call loadKeyboard later
            // anyway.
            needToCallLoadKeyboardLater = false;
        }

        if (isDifferentTextField) {
            mainKeyboardView.closing();
            suggest.setAutoCorrectionThreshold(currentSettingsValues.mAutoCorrectionThreshold);
            switcher.reloadMainKeyboard();
            if (needToCallLoadKeyboardLater) {
                // If we need to call loadKeyboard again later, we need to save its state now.
                // The
                // later call will be done in #retryResetCaches.
                switcher.saveKeyboardState();
            }
        } else if (restarting) {
            // TODO: Come up with a more comprehensive way to reset the keyboard layout when
            // a keyboard layout set doesn't get reloaded in this method.
            switcher.resetKeyboardStateToAlphabet(getCurrentAutoCapsState(), getCurrentRecapitalizeState());
            // In apps like Talk, we come here when the text is sent and the field gets
            // emptied and
            // we need to re-evaluate the shift state, but not the whole layout which would
            // be
            // disruptive.
            // Space state must be updated before calling updateShiftState
            switcher.requestUpdatingShiftState(getCurrentAutoCapsState(), getCurrentRecapitalizeState());
        }
        if (hasSuggestionStripView()) mSuggestionStripView.onNewEditorSession();
        // Set neutral suggestions and show the toolbar if the "Auto show toolbar"
        // setting is enabled.
        if (!mHandler.hasPendingResumeSuggestions()) {
            mHandler.cancelUpdateSuggestionStrip();
            // Publish an initial row synchronously, BEFORE the keyboard's first
            // draw. A posted dictionary callback cannot populate the entry frame.
            // Clipboard/inline autofill, privacy and application completions win.
            final boolean shouldSeed = helium314.keyboard.latin.suggestions.StartupSuggestionPolicy
                    .eligible(mRichImm.getCurrentSubtypeLocale(),
                            currentSettingsValues.mInputAttributes.mIsGeneralTextInput,
                            currentSettingsValues.needsToLookupSuggestions(),
                            currentSettingsValues.mBigramPredictionEnabled,
                            currentSettingsValues.isSuggestionsEnabledPerUserSettings(),
                            secureEditor || currentSettingsValues.mIncognitoModeEnabled,
                            currentSettingsValues.isApplicationSpecifiedCompletionsOn(),
                            mInlineProviderViewActive, isImeSuppressedByHardwareKeyboard(),
                            needToCallLoadKeyboardLater,
                            editorInfo.initialSelStart, editorInfo.initialSelEnd);
            if (shouldSeed && hasSuggestionStripView() && onEvaluateInputViewShown()
                    && !tryShowClipboardSuggestion()) {
                mStartupSuggestionSeedVisible = true;
                setSuggestedWords(helium314.keyboard.latin.suggestions.StartupSuggestionPolicy
                        .initialEnglishWords());
            } else {
                setNeutralSuggestionStrip();
                if (hasSuggestionStripView() && currentSettingsValues.mAutoShowToolbar
                        && !tryShowClipboardSuggestion()) {
                    mSuggestionStripView.setToolbarVisibility(true);
                }
            }
            // Warm the very first context candidates while the keyboard is
            // already visible, instead of waiting for a second character.
            if (currentSettingsValues.needsToLookupSuggestions()
                    && currentSettingsValues.mBigramPredictionEnabled
                    && !currentSettingsValues.isApplicationSpecifiedCompletionsOn()
                    && !needToCallLoadKeyboardLater
                    && !isImeSuppressedByHardwareKeyboard()) {
                mHandler.postUpdateSuggestionStrip(SuggestedWords.INPUT_STYLE_PREDICTION);
            }
        }

        mainKeyboardView.setMainDictionaryAvailability(mDictionaryFacilitator.hasAtLeastOneInitializedMainDictionary());
        mainKeyboardView.setKeyPreviewPopupEnabled(currentSettingsValues.mKeyPreviewPopupOn);
        mainKeyboardView.setSlidingKeyInputPreviewEnabled(currentSettingsValues.mSlidingKeyInputPreviewEnabled);
        mainKeyboardView.setGestureHandlingEnabledByUser(
                currentSettingsValues.mGestureInputEnabled,
                currentSettingsValues.mGestureTrailEnabled,
                currentSettingsValues.mGestureFloatingPreviewTextEnabled);

        if (TRACE)
            Debug.startMethodTracing("/data/trace/latinime");
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        if (isInputViewShown()) {
            setNavigationBarColor();
            workaroundForHuaweiStatusBarIssue();
            FrostedGlassHelper.configureFrostedGlass(this, mInputView, FrostedGlassHelper.isFrostedTheme(this));
        }
    }

    @Override
    public void onWindowHidden() {
        mTextTools.dismiss();
        mClipboardHistoryManager.dismissExtraction();
        mClipboardHistoryManager.clearDecryptedHistory();
        mKeyboardActionListener.onCancelDeletePointer();
        mInputLogic.clearVerifiedEdits();
        mInputLogic.clearCorrectionRecall();
        mInputLogic.clearTransientCorrectionMemory();
        SecureAppTemporaryOptOut.clear();
        if (mInputView instanceof InputView) ((InputView) mInputView).cancelSlideConfirmation();
        mEditorSessionGeneration.invalidate();
        mKeyboardSwitcher.cancelPanelTransition();
        // Hiding the IME also ends the visible typing session, including OEMs
        // that reopen without a fresh onStartInputView callback.
        mKeyboardSwitcher.beginEmojiEditorSession();
        mInlineDictation.cancel();
        mOfflineDictation.onEditorLost();
        super.onWindowHidden();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        Log.i(TAG, "onWindowHidden");

        mKeyboardSwitcher.clearCachedPersistentEmojis();

        // Completely disable background blur composition, layout attributes and clear SemBlurInfo/window flags to save GPU context
        FrostedGlassHelper.configureFrostedGlass(this, mInputView, false);

        final MainKeyboardView mainKeyboardView = mKeyboardSwitcher.getMainKeyboardView();
        if (mainKeyboardView != null) {
            mainKeyboardView.closing();
        }
        clearNavigationBarColor();

        // Release keyboard buffers and stop active panels without forcing a process-wide GC.
        deallocateMemory();

        // Reset to the main alphabet keyboard so that reopening always shows the ABC view
        // instead of whatever panel (emoji, clipboard, AI tools, klipy) was active.
        mKeyboardSwitcher.setAlphabetKeyboard();
    }

    void onFinishInputInternal() {
        SecureAppTemporaryOptOut.clear();
        mEditorSessionGeneration.invalidate();
        mKeyboardSwitcher.cancelPanelTransition();
        super.onFinishInput();
        Log.i(TAG, "onFinishInput");

        mDictionaryFacilitator.onFinishInput();
        final MainKeyboardView mainKeyboardView = mKeyboardSwitcher.getMainKeyboardView();
        if (mainKeyboardView != null) {
            mainKeyboardView.closing();
        }
    }

    void onFinishInputViewInternal(final boolean finishingInput) {
        mKeyboardSwitcher.cancelPanelTransition();
        super.onFinishInputView(finishingInput);
        Log.i(TAG, "onFinishInputView");
        cleanupInternalStateForFinishInput();
    }

    private void cleanupInternalStateForFinishInput() {
        mClipboardHistoryManager.releaseClipboardAccessory();
        SecureAppTemporaryOptOut.clear();
        mEditorSessionGeneration.invalidate();
        // Remove pending messages related to update suggestions
        mHandler.cancelUpdateSuggestionStrip();
        // Should do the following in onFinishInputInternal but until JB MR2 it's not
        // called :(
        mInputLogic.finishInput();
        mKeyboardActionListener.resetMetaState();
    }

    protected void deallocateMemory() {
        mKeyboardSwitcher.deallocateMemory();
    }

    @Override
    public void onUpdateSelection(final int oldSelStart, final int oldSelEnd,
            final int newSelStart, final int newSelEnd,
            final int composingSpanStart, final int composingSpanEnd) {
        if (oldSelStart != newSelStart || oldSelEnd != newSelEnd) mVaultSelectionEpoch++;
        mTextTools.selectionChanged(newSelStart, newSelEnd);
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                composingSpanStart, composingSpanEnd);
        final SettingsValues selectionSettings = mSettings.getCurrent();
        final EditorInfo selectionEditor = getCurrentInputEditorInfo();
        if (!selectionSettings.mIncognitoModeEnabled
                && selectionSettings.mInputAttributes.mIsGeneralTextInput
                && selectionSettings.mInputAttributes.mIsPasswordField == false
                && !ClipboardPrivacyPolicy.isPrivateEditor(selectionEditor,
                        selectionSettings.mIncognitoModeEnabled)
                && ChatGptVoiceSuggestionStability.noteSelectionUpdate(selectionEditor,
                        oldSelEnd, newSelStart, newSelEnd, android.os.SystemClock.uptimeMillis())) {
            mHandler.removeCallbacks(mFlushChatGptVoiceSuggestions);
            mHandler.postDelayed(mFlushChatGptVoiceSuggestions,
                    ChatGptVoiceSuggestionStability.SETTLE_MS);
        }
        if (DebugFlags.DEBUG_ENABLED) {
            Log.i(TAG, "onUpdateSelection: oss=" + oldSelStart + ", ose=" + oldSelEnd
                    + ", nss=" + newSelStart + ", nse=" + newSelEnd
                    + ", cs=" + composingSpanStart + ", ce=" + composingSpanEnd);
        }

        // This call happens whether our view is displayed or not, but if it's not then
        final SettingsValues settingsValues = mSettings.getCurrent();
        if (isInputViewShown()
                && mInputLogic.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                        composingSpanStart, composingSpanEnd, settingsValues)) {
            // we don't want to update a manually set shift state if selection changed
            // towards one side
            // because this may end the manual shift, which is unwanted in case of shift +
            // arrow keys for changing selection
            // todo: this is not fully implemented yet, and maybe should be behind a setting
            if (mKeyboardSwitcher.getKeyboard() != null
                    && mKeyboardSwitcher.getKeyboard().mId.isAlphabetShiftedManually()
                    && ((oldSelEnd == newSelEnd && oldSelStart != newSelStart)
                            || (oldSelEnd != newSelEnd && oldSelStart == newSelStart)))
                return;
            mKeyboardSwitcher.requestUpdatingShiftState(getCurrentAutoCapsState(), getCurrentRecapitalizeState());
        }
    }

    /**
     * This is called when the user has clicked on the extracted text view,
     * when running in fullscreen mode. The default implementation hides
     * the suggestions view when this happens, but only if the extracted text
     * editor has a vertical scroll bar because its text doesn't fit.
     * Here we override the behavior due to the possibility that a re-correction
     * could
     * cause the suggestions strip to disappear and re-appear.
     */
    @Override
    public void onExtractedTextClicked() {
        if (mSettings.getCurrent().needsToLookupSuggestions()) {
            return;
        }

        super.onExtractedTextClicked();
    }

    /**
     * This is called when the user has performed a cursor movement in the
     * extracted text view, when it is running in fullscreen mode. The default
     * implementation hides the suggestions view when a vertical movement
     * happens, but only if the extracted text editor has a vertical scroll bar
     * because its text doesn't fit.
     * Here we override the behavior due to the possibility that a re-correction
     * could
     * cause the suggestions strip to disappear and re-appear.
     */
    @Override
    public void onExtractedCursorMovement(final int dx, final int dy) {
        if (mSettings.getCurrent().needsToLookupSuggestions()) {
            return;
        }

        super.onExtractedCursorMovement(dx, dy);
    }

    @Override
    public void onViewClicked(final boolean focusChanged) {
        exitEmojiSearchModeFromAppClick();
        super.onViewClicked(focusChanged);
    }

    private boolean exitEmojiSearchModeFromAppClick() {
        final EmojiPalettesView emojiPalettesView = mKeyboardSwitcher.getEmojiPalettesView();
        if (emojiPalettesView == null || !emojiPalettesView.isSearchMode()) {
            return false;
        }
        emojiPalettesView.exitSearchMode(false);
        return true;
    }

    @Override
    public void hideWindow() {
        Log.i(TAG, "hideWindow");
        if (hasSuggestionStripView() && mSettings.getCurrent().mToolbarMode == ToolbarMode.EXPANDABLE)
            mSuggestionStripView.setToolbarVisibility(false);
        mKeyboardSwitcher.onHideWindow();

        if (TRACE)
            Debug.stopMethodTracing();
        if (isShowingOptionDialog()) {
            mOptionsDialog.dismiss();
            mOptionsDialog = null;
        }
        super.hideWindow();
    }

    @Override
    public void requestHideSelf(int flags) {
        super.requestHideSelf(flags);
        Log.i(TAG, "requestHideSelf: " + flags);
    }

    void cancelBoardDictation() {
        mInlineDictation.cancel();
        mOfflineDictation.cancel();
    }

    void startBoardOfflineDictation(boolean holdToTalk) {
        mInlineDictation.cancel();
        mOfflineDictation.start(mInputView, holdToTalk);
    }

    void stopBoardOfflineDictation() {
        mOfflineDictation.stop();
    }

    void cancelBoardOfflineDictation() {
        mOfflineDictation.cancelFromUser();
    }

    void toggleBoardOfflineDictation() {
        mInlineDictation.cancel();
        mOfflineDictation.toggle(mInputView);
    }

    void toggleBoardDictation() {
        // All mic/VOICE_INPUT entry points are private and offline. Preserve the
        // old method name for compatibility; never invoke the brrSpeak transport.
        toggleBoardOfflineDictation();
    }

    @Override
    public void onSwipeDownOnToolbar() {
        requestHideSelf(0);
    }

    @Override
    public void onDisplayCompletions(final CompletionInfo[] applicationSpecifiedCompletions) {
        if (WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), getCurrentInputEditorInfo())) return;
        if (DebugFlags.DEBUG_ENABLED) {
            Log.i(TAG, "Received completions:");
            if (applicationSpecifiedCompletions != null) {
                for (int i = 0; i < applicationSpecifiedCompletions.length; i++) {
                    Log.i(TAG, "  #" + i + ": " + applicationSpecifiedCompletions[i]);
                }
            }
        }
        if (!mSettings.getCurrent().isApplicationSpecifiedCompletionsOn()) {
            return;
        }
        // If we have an update request in flight, we need to cancel it so it does not
        // override
        // these completions.
        mHandler.cancelUpdateSuggestionStrip();
        if (applicationSpecifiedCompletions == null) {
            setNeutralSuggestionStrip();
            return;
        }

        final ArrayList<SuggestedWords.SuggestedWordInfo> applicationSuggestedWords = SuggestedWords
                .getFromApplicationSpecifiedCompletions(
                        applicationSpecifiedCompletions);
        final SuggestedWords suggestedWords = new SuggestedWords(applicationSuggestedWords,
                null /* rawSuggestions */,
                null /* typedWord */,
                false /* typedWordValid */,
                false /* willAutoCorrect */,
                false /* isObsoleteSuggestions */,
                SuggestedWords.INPUT_STYLE_APPLICATION_SPECIFIED /* inputStyle */,
                SuggestedWords.NOT_A_SEQUENCE_NUMBER);
        // When in fullscreen mode, show completions generated by the application
        // forcibly
        setSuggestedWords(suggestedWords);
    }

    @Override
    public void onComputeInsets(final InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (mInputView == null) return;

        View visibleKeyboardView = mKeyboardSwitcher.getWrapperView();
        if (visibleKeyboardView == null || visibleKeyboardView.getVisibility() != View.VISIBLE) {
            if (mKeyboardSwitcher.getEmojiPalettesView() != null && mKeyboardSwitcher.getEmojiPalettesView().getVisibility() == View.VISIBLE) {
                visibleKeyboardView = mKeyboardSwitcher.getEmojiPalettesView();
            } else if (mKeyboardSwitcher.getClipboardHistoryView() != null && mKeyboardSwitcher.getClipboardHistoryView().getVisibility() == View.VISIBLE) {
                visibleKeyboardView = mKeyboardSwitcher.getClipboardHistoryView();
            } else if (mKeyboardSwitcher.getAccessPointMenuView() != null && mKeyboardSwitcher.getAccessPointMenuView().getVisibility() == View.VISIBLE) {
                visibleKeyboardView = mKeyboardSwitcher.getAccessPointMenuView();
            }
        }

        final int inputHeight = mInputView.getHeight();
        final int stripHeight = mKeyboardSwitcher.isShowingStripContainer() ? mKeyboardSwitcher.getStripContainer().getHeight() : 0;
        final int persistentEmojiRowHeight = mKeyboardSwitcher.isShowingPersistentEmojiRow() ? mKeyboardSwitcher.getPersistentEmojiRowHeight() : 0;
        final View footer = mInputView.findViewById(R.id.board_footer);
        final int footerHeight = footer != null && footer.isShown() ? footer.getHeight() : 0;
        final View keyboardFrame = mInputView.findViewById(R.id.main_keyboard_frame);
        // RoundedKeyboardFrameView puts the system navigation-bar inset into its
        // bottom padding. That padding is part of the IME window, so subtract it
        // when deriving the residual top inset; otherwise host apps under-resize
        // by the navigation inset and their last content row sits behind brrBoard.
        final int navigationInset = keyboardFrame == null ? 0 : keyboardFrame.getPaddingBottom();

        int keyboardHeight = 0;
        if (visibleKeyboardView != null && visibleKeyboardView.getHeight() > 0) {
            keyboardHeight = visibleKeyboardView.getHeight();
        } else if (mLastKeyboardHeight > 0) {
            // Fallback to last known height to prevent the gap from closing during transitions
            keyboardHeight = mLastKeyboardHeight - stripHeight - persistentEmojiRowHeight - footerHeight;
            if (keyboardHeight < 0) keyboardHeight = 0;
        }

        if (keyboardHeight == 0 && visibleKeyboardView == null) return;

        int visibleTopY = inputHeight - keyboardHeight - stripHeight - persistentEmojiRowHeight
                - footerHeight - navigationInset;
        if (visibleTopY < 0) visibleTopY = 0;

        outInsets.contentTopInsets = visibleTopY;
        outInsets.visibleTopInsets = visibleTopY;

        if (visibleKeyboardView != null && visibleKeyboardView.isShown()) {
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_CONTENT;
        }

        if (mInsetsUpdater != null) mInsetsUpdater.setInsets(outInsets);

        if (keyboardHeight > 0) {
            mLastKeyboardHeight = keyboardHeight + stripHeight + persistentEmojiRowHeight + footerHeight;
        }
    }

    public void startShowingInputView(final boolean needsToLoadKeyboard) {
        mIsExecutingStartShowingInputView = true;
        // This {@link #showWindow(boolean)} will eventually call back
        // {@link #onEvaluateInputViewShown()}.
        showWindow(true /* showInput */);
        mIsExecutingStartShowingInputView = false;
        if (needsToLoadKeyboard) {
            loadKeyboard();
        }
    }

    public void stopShowingInputView() {
        showWindow(false /* showInput */);
    }

    @Override
    public boolean onShowInputRequested(final int flags, final boolean configChange) {
        if (isImeSuppressedByHardwareKeyboard()) {
            return true;
        }
        return super.onShowInputRequested(flags, configChange);
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        if (mIsExecutingStartShowingInputView) {
            return true;
        }
        return super.onEvaluateInputViewShown();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void updateFullscreenMode() {
        super.updateFullscreenMode();
        FrostedGlassHelper.configureFrostedGlass(this, mInputView, FrostedGlassHelper.isFrostedTheme(this));
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public InlineSuggestionsRequest onCreateInlineSuggestionsRequest(@NonNull Bundle uiExtras) {
        Log.d(TAG, "onCreateInlineSuggestionsRequest called");
        if (WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), getCurrentInputEditorInfo())
                || Settings.getValues().mSuggestionStripHiddenPerUserSettings) {
            return null;
        }
        mInlineRequestEpoch = mEditorFocusEpoch;
        return InlineAutofillUtils.createInlineSuggestionRequest(mDisplayContext);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.R)
    public boolean onInlineSuggestionsResponse(InlineSuggestionsResponse response) {
        Log.d(TAG, "onInlineSuggestionsResponse called");
        if (WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), getCurrentInputEditorInfo())
                || Settings.getValues().mSuggestionStripHiddenPerUserSettings
                || !AccessoryPriorityPolicy.responseBelongsToVisibleEditor(
                        mInlineRequestEpoch, mEditorFocusEpoch, isInputViewShown())) {
            return false;
        }

        final List<InlineSuggestion> inlineSuggestions = response.getInlineSuggestions();
        if (inlineSuggestions.isEmpty()) {
            if (mInlineProviderViewActive) removeExternalSuggestions();
            return false;
        }

        final View inlineSuggestionView = InlineAutofillUtils.createView(inlineSuggestions, mDisplayContext);
        if (inlineSuggestionView == null || mSuggestionStripView == null) {
            mInlineProviderViewActive = false;
            return false;
        }

        // Without this function the inline autofill suggestions will not be visible
        mHandler.cancelResumeSuggestions();
        mInlineProviderViewActive = true;
        mSuggestionStripView.setExternalSuggestionView(inlineSuggestionView, true);

        return true;
    }

    public int getCurrentAutoCapsState() {
        return mInputLogic.getCurrentAutoCapsState(mSettings.getCurrent());
    }

    @Nullable
    public RecapitalizeMode getCurrentRecapitalizeState() {
        return mInputLogic.getCurrentRecapitalizeState();
    }

    /**
     * @param codePoints code points to get coordinates for.
     * @return x,y coordinates for this keyboard, as a flattened array.
     */
    public int[] getCoordinatesForCurrentKeyboard(final int[] codePoints) {
        final Keyboard keyboard = mKeyboardSwitcher.getKeyboard();
        if (null == keyboard) {
            return CoordinateUtils.newCoordinateArray(codePoints.length,
                    Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE);
        }
        return keyboard.getCoordinates(codePoints);
    }

    public void displaySettingsDialog() {
        launchSettings();
    }

    public boolean showInputPickerDialog() {
        if (isShowingOptionDialog())
            return false;
        if (mRichImm.hasMultipleEnabledIMEsOrSubtypes(true)) {
            mOptionsDialog = InputMethodPickerKt.createInputMethodPickerDialog(this, mRichImm,
                    mKeyboardSwitcher.getMainKeyboardView().getWindowToken());
            mOptionsDialog.show();
            return true;
        }
        return false;
    }

    private boolean isShowingOptionDialog() {
        return mOptionsDialog != null && mOptionsDialog.isShowing();
    }

    // called when language switch key is pressed (either the keyboard key, or
    // long-press comma)
    public void switchBoardInputMethod() {
        mInlineDictation.cancel();
        mOfflineDictation.cancel();
        if (!ImeCompat.INSTANCE.switchInputMethod(this)) showInputPickerDialog();
    }

    public void cycleBoardLanguage() {
        if (mRichImm.hasMultipleEnabledSubtypesInThisIme(true)) {
            final InputMethodSubtype next = mRichImm.getNextSubtypeInThisIme(true);
            if (next != null) switchToSubtype(next);
        } else {
            showInputPickerDialog();
        }
    }

    public void switchToNextSubtype() {
        final boolean switchSubtype = mSettings.getCurrent().mLanguageSwitchKeyToOtherSubtypes;
        final boolean switchIme = mSettings.getCurrent().mLanguageSwitchKeyToOtherImes;

        // switch IME if wanted and possible
        if (switchIme && !switchSubtype && ImeCompat.INSTANCE.switchInputMethod(this))
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
        if (LockedKeyboardActionPolicy.isRestricted(event.getKeyCode())
                && helium314.keyboard.compat.IsLockedCompatKt.isDeviceLocked(this)) return;
        ChatGptVoiceSuggestionStability.noteKeyboardInput(android.os.SystemClock.uptimeMillis());
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
        if (rawText == null) return;
        ChatGptVoiceSuggestionStability.noteKeyboardInput(android.os.SystemClock.uptimeMillis());
        // TODO: have the keyboard pass the correct key code when we need it.
        final Event event = Event.createSoftwareTextEvent(rawText, KeyCode.MULTIPLE_CODE_POINTS, null);
        final InputTransaction completeInputTransaction = mInputLogic.onTextInput(mSettings.getCurrent(), event,
                mKeyboardSwitcher.getKeyboardShiftMode(), mHandler);
        updateStateAfterInputTransaction(completeInputTransaction);
        mInputLogic.restartSuggestionsOnWordTouchedByCursor(mSettings.getCurrent(),
                mKeyboardSwitcher.getCurrentKeyboardScript());
        mKeyboardSwitcher.onEvent(event, getCurrentAutoCapsState(), getCurrentRecapitalizeState());
    }

    /** Actual final dictation only; never called by credential or recovery paste paths. */
    void onFinalizedVoiceTextInput(final String text, final boolean replaceVoiceDraft) {
        // Capture before dispatch: a host editor can disappear during commitText.
        mClipboardHistoryManager.rememberFinalVoiceTranscript(text);
        onPrivateVoiceTextInput(text, replaceVoiceDraft);
    }

    /** Inserts a dictated phrase with native suggestion refresh but without transcript learning. */
    void onPrivateVoiceTextInput(final String text) {
        onPrivateVoiceTextInput(text, false);
    }

    void onPrivateVoiceTextInput(final String text, final boolean replaceVoiceDraft) {
        final Event event = Event.createSoftwareTextEvent(text, KeyCode.MULTIPLE_CODE_POINTS, null);
        final InputTransaction transaction = mInputLogic.onPrivateVoiceTextInput(
                mSettings.getCurrent(), event, mKeyboardSwitcher.getKeyboardShiftMode(), mHandler,
                replaceVoiceDraft);
        updateStateAfterInputTransaction(transaction);
        mInputLogic.restartSuggestionsOnWordTouchedByCursor(mSettings.getCurrent(),
                mKeyboardSwitcher.getCurrentKeyboardScript());
        mKeyboardSwitcher.onEvent(event, getCurrentAutoCapsState(), getCurrentRecapitalizeState());
    }

    /**
     * Consume the one-use, short-lived vault authorization only in an ordinary prose field.
     * This path uses the normal editor transaction but never feeds the secret to personalization.
     */
    public boolean insertArmedPrivateGestureSecret() {
        if (!isInputViewShown() || getCurrentInputConnection() == null) return false;
        final SettingsValues current = mSettings.getCurrent();
        if (current == null) return false;
        final var attributes = current.mInputAttributes;
        final String targetPackage = attributes.mTargetApplicationPackageName;
        if (!attributes.mIsGeneralTextInput || attributes.mNoLearning || attributes.mIsPasswordField
                || current.mIncognitoModeEnabled
                || AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled()
                || SecureAppsPolicy.isSecure(KtxKt.prefs(this), targetPackage)
                || !KtxKt.prefs(this).getBoolean(PrivateGestureSession.PREF_ENABLED, false)) {
            return false;
        }
        final String privateText = PrivateGestureSession.consume();
        if (privateText == null || privateText.isEmpty()) return false;
        onPrivateVoiceTextInput(privateText);
        return true;
    }

    /** Credential insertion deliberately bypasses voice, gesture decoding and suggestion events. */
    public boolean isVaultPasswordTarget() {
        final EditorInfo editor = getCurrentInputEditorInfo();
        // Android validates EditorInfo.packageName against the focused IME client.
        // Recheck the actual binding UID as defense in depth, never an app label or prefix.
        final android.view.inputmethod.InputBinding binding = getCurrentInputBinding();
        if (!WhatsAppVaultPolicy.eligible(KtxKt.prefs(this), editor) || binding == null
                || AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled()) return false;
        final int uid = binding.getUid();
        if (mVaultUidEditorEpoch != mEditorFocusEpoch || mVaultCheckedUid != uid) {
            mVaultUidEditorEpoch = mEditorFocusEpoch;
            mVaultCheckedUid = uid;
            mVaultUidVerified = false;
            final String[] packages = getPackageManager().getPackagesForUid(uid);
            if (packages != null) for (String pkg : packages)
                if (WhatsAppVaultPolicy.PACKAGE.equals(pkg)) { mVaultUidVerified = true; break; }
        }
        return mVaultUidVerified;
    }

    public void insertVaultGesture(final float[] trace, final java.util.function.BooleanSupplier stillEligible) {
        if (!isInputViewShown() || !isVaultPasswordTarget() ||
                !KtxKt.prefs(this).getBoolean(VaultGestureStore.PREF_ENABLED, false)) return;
        final android.view.inputmethod.InputConnection connection = getCurrentInputConnection();
        if (connection == null) return;
        final long editor = mEditorFocusEpoch;
        final long selection = mVaultSelectionEpoch;
        VaultGestureStore.matchAsync(this, trace, secret -> {
            if (secret == null) {
                if (editor == mEditorFocusEpoch && selection == mVaultSelectionEpoch && isInputViewShown() && isVaultPasswordTarget()) {
                    AudioAndHapticFeedbackManager.getInstance().performSecureAppIncognitoWarning();
                    // Private gesture failure is haptic only: no public vault-identifying toast.
                }
                return;
            }
            try {
                if (mIsDestroyed || editor != mEditorFocusEpoch || selection != mVaultSelectionEpoch
                        || !isInputViewShown() || !isVaultPasswordTarget() || !stillEligible.getAsBoolean()
                        || connection != getCurrentInputConnection()
                        || !VaultGestureStore.isDeviceUnlocked(this)
                        || !KtxKt.prefs(this).getBoolean(VaultGestureStore.PREF_ENABLED, false)) return;
                // Store only metadata guard, never the code. Guard survives restart/refocus,
                // including the case where an ordinary WhatsApp editor has no stable ID.
                if (!WhatsAppVaultPolicy.protectBeforeInsert(KtxKt.prefs(this), getCurrentInputEditorInfo())) return;
                clearPendingPrivateLearning();
                mHandler.removeCallbacks(mFlushChatGptVoiceSuggestions);
                mPendingChatGptVoiceSuggestions = null;
                mInlineDictation.cancel();
                mOfflineDictation.cancel();
                mInlineProviderViewActive = false;
                mInlineRequestEpoch = -1;
                mInputLogic.preparePrivateCredential();
                loadSettings();
                if (!mSettings.getCurrent().mInputAttributes.mVaultPrivateEditor) return;
                final MainKeyboardView mainKeyboard = mKeyboardSwitcher.getMainKeyboardView();
                if (mainKeyboard != null) mainKeyboard.setGestureHandlingEnabledByUser(false, false, false);
                if (mInputView instanceof InputView inputView) inputView.refreshBoardPrivacy();
                if (hasSuggestionStripView()) mSuggestionStripView.onNewEditorSession();
                setSuggestedWords(SuggestedWords.getEmptyInstance());
                // Recheck ownership after preparation; no Enter, Send, clipboard or paste path.
                if (editor != mEditorFocusEpoch || selection != mVaultSelectionEpoch
                        || connection != getCurrentInputConnection() || !isVaultPasswordTarget()) return;
                if (connection.commitText(new String(secret), 1))
                    AudioAndHapticFeedbackManager.getInstance().performImportantActionConfirmation();
            } finally {
                java.util.Arrays.fill(secret, (char) 0);
            }
        });
    }

    /** Explicit extracted text insertion; never train the dictionaries or add history. */
    public boolean insertExtractedClipboardText(final String text, final long epoch,
            final android.view.inputmethod.InputConnection connection) {
        if (text == null || text.isEmpty() || text.length() > 512 || !isInputViewShown()
                || connection == null || connection != getCurrentInputConnection()
                || epoch != editorGeneration() || !mayUseTransientCorrectionMemory(mSettings.getCurrent())) return false;
        mInputLogic.preparePrivateCredential();
        if (connection != getCurrentInputConnection() || epoch != editorGeneration()
                || !mayUseTransientCorrectionMemory(mSettings.getCurrent())) return false;
        mInputLogic.mConnection.beginBatchEdit();
        try {
            mInputLogic.mConnection.commitText(text, 1);
        } finally {
            mInputLogic.mConnection.endBatchEdit();
        }
        setSuggestedWords(SuggestedWords.getEmptyInstance());
        return true;
    }

    /** Only a deliberate tap in Clipboard may place a temporary voice transcript. */
    public boolean insertRecoveredVoiceText(final String text) {
        if (text == null || text.isEmpty() || !isInputViewShown()
                || !mayUseTransientCorrectionMemory(mSettings.getCurrent())) return false;
        onPrivateVoiceTextInput(text);
        return true;
    }

    /** Additional learning/recall must never bypass the protected editor policy. */
    public boolean mayUseTransientCorrectionMemory(final SettingsValues values) {
        final EditorInfo editor = getCurrentInputEditorInfo();
        return values != null && editor != null && getCurrentInputConnection() != null
                && values.mInputAttributes.mIsGeneralTextInput
                && !values.mInputAttributes.mNoLearning && !values.mInputAttributes.mIsPasswordField
                && !values.mInputAttributes.mVaultPrivateEditor
                && !ClipboardPrivacyPolicy.isPrivateEditor(editor, values.mIncognitoModeEnabled)
                && !WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), editor)
                && !SecureAppsPolicy.isSecure(KtxKt.prefs(this), editor.packageName)
                && !helium314.keyboard.compat.IsLockedCompatKt.isDeviceLocked(this);
    }

    /** Manual incognito switch: forget any unfinished cross-mode swipe receipts. */
    void clearPendingPrivateLearning() {
        mClipboardHistoryManager.clearRecoveredTranscripts();
        mTextTools.dismiss();
        mClipboardHistoryManager.dismissExtraction();
        mClipboardHistoryManager.clearDecryptedHistory();
        mKeyboardActionListener.onCancelDeletePointer();
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
        for (int i=0;i<suggestedWords.size();i++) {
            final SuggestedWordInfo info = suggestedWords.getInfo(i);
            if (info.isKindOf(SuggestedWordInfo.KIND_SESSION_RECALL)
                    && !mInputLogic.isCorrectionRecallCurrent(info)) return;
        }
        cancelPendingEmptyPredictionClear();
        final SettingsValues currentSettingsValues = mSettings.getCurrent();
        if (currentSettingsValues.mInputAttributes.mVaultPrivateEditor && !suggestedWords.isEmpty()) return;
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
        for (int i=0; i<suggestedWords.size(); i++) {
            final SuggestedWordInfo info = suggestedWords.getInfo(i);
            if (info.isKindOf(SuggestedWordInfo.KIND_SESSION_RECALL)
                    && !mInputLogic.isCorrectionRecallCurrent(info)) {
                // A deferred or asynchronous result must never resurrect an expired receipt.
                return;
            }
        }
        if (ChatGptVoiceSuggestionStability.shouldHold(getCurrentInputEditorInfo(),
                android.os.SystemClock.uptimeMillis())) {
            // ChatGPT's voice dictation streams host edits through the text connection. Keep
            // the visible strip steady during the stream while retaining the newest real
            // candidate internally and revoking stale tap targets until the stream settles.
            mPendingChatGptVoiceSuggestions = suggestedWords;
            mInputLogic.setSuggestedWords(suggestedWords);
            ChatGptVoiceSuggestionStability.recordDeferredSuggestion();
            if (!mInlineProviderViewActive && mSuggestionStripView != null)
                mSuggestionStripView.holdUnselectablePredictions();
            mHandler.removeCallbacks(mFlushChatGptVoiceSuggestions);
            mHandler.postDelayed(mFlushChatGptVoiceSuggestions,
                    ChatGptVoiceSuggestionStability.remainingHoldMs(
                            android.os.SystemClock.uptimeMillis()));
            AccessibilityUtils.Companion.getInstance().setAutoCorrection(suggestedWords);
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
                    BrSuggestionBrain.learnEmoji(this, this, getCurrentInputEditorInfo(),
                            mSettings.getCurrent(), preceding, suggestionInfo.mWord);
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
        if (!hasSuggestionStripView() || helium314.keyboard.compat.IsLockedCompatKt.isDeviceLocked(this)) {
            mClipboardHistoryManager.releaseClipboardAccessory();
            return false;
        }
        if (WhatsAppVaultPolicy.isPrivateEditor(KtxKt.prefs(this), getCurrentInputEditorInfo())) {
            mClipboardHistoryManager.releaseClipboardAccessory();
            return false;
        }
        if (!AccessoryPriorityPolicy.canReplaceVisible(mInlineProviderViewActive,
                AccessoryPriorityPolicy.Incoming.CLIPBOARD)) {
            mClipboardHistoryManager.releaseClipboardAccessory();
            return false;
        }
        final View clipboardView = mClipboardHistoryManager.getClipboardSuggestionView(getCurrentInputEditorInfo(),
                mSuggestionStripView);
        if (clipboardView != null) {
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
        mInputLogic.mSuggest.clearNextWordSuggestionsCache();
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
            // Fail closed: if Android cannot tell us the current interruption
            // filter, suppress ordinary key haptics until the next state update.
            Log.w(TAG, "zen_mode setting not found, assuming DND is active");
            AudioAndHapticFeedbackManager.getInstance().onRingerModeChanged(true);
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
        if (helium314.keyboard.compat.IsLockedCompatKt.isDeviceLocked(this)) return;
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
        mKeyboardSwitcher.openEmojiSearch();
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
