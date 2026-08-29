/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.hamp.inputmethod.keyboard;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.hamp.inputmethod.accessibility.AccessibilityUtils;
import com.hamp.inputmethod.accessibility.MainKeyboardAccessibilityDelegate;
import com.hamp.inputmethod.annotations.ExternallyReferenced;
import com.hamp.inputmethod.engine.StateHint;
import com.hamp.inputmethod.keyboard.internal.DrawingPreviewPlacerView;
import com.hamp.inputmethod.keyboard.internal.DrawingProxy;
import com.hamp.inputmethod.keyboard.internal.GestureFloatingTextDrawingPreview;
import com.hamp.inputmethod.keyboard.internal.GestureTrailsDrawingPreview;
import com.hamp.inputmethod.keyboard.internal.KeyDrawParams;
import com.hamp.inputmethod.keyboard.internal.KeyPreviewChoreographer;
import com.hamp.inputmethod.keyboard.internal.KeyPreviewDrawParams;
import com.hamp.inputmethod.keyboard.internal.KeyPreviewView;
import com.hamp.inputmethod.keyboard.internal.MoreKeySpec;
import com.hamp.inputmethod.keyboard.internal.NonDistinctMultitouchHelper;
import com.hamp.inputmethod.keyboard.internal.SlidingKeyInputDrawingPreview;
import com.hamp.inputmethod.keyboard.internal.TimerHandler;
import com.hamp.inputmethod.latin.AudioAndHapticFeedbackManager;
import com.hamp.inputmethod.latin.Subtypes;
import com.hamp.inputmethod.latin.uix.DynamicThemeProvider;
import com.hamp.inputmethod.latin.R;
import com.hamp.inputmethod.latin.SuggestedWords;
import com.hamp.inputmethod.latin.common.Constants;
import com.hamp.inputmethod.latin.common.CoordinateUtils;
import com.hamp.inputmethod.latin.settings.Settings;
import com.hamp.inputmethod.latin.settings.SettingsValues;
import com.hamp.inputmethod.latin.uix.theme.KeyDrawingConfiguration;
import com.hamp.inputmethod.latin.utils.LanguageOnSpacebarUtils;
import com.hamp.inputmethod.latin.utils.SubtypeLocaleUtils;
import com.hamp.inputmethod.latin.utils.TypefaceUtils;
import com.hamp.inputmethod.v2keyboard.LayoutManager;

import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import kotlin.Pair;

/**
 * A view that is responsible for detecting key presses and touch movements.
 *
 * @attr ref R.styleable#MainKeyboardView_languageOnSpacebarTextRatio
 * @attr ref R.styleable#MainKeyboardView_languageOnSpacebarTextColor
 * @attr ref R.styleable#MainKeyboardView_languageOnSpacebarTextShadowRadius
 * @attr ref R.styleable#MainKeyboardView_languageOnSpacebarTextShadowColor
 * @attr ref R.styleable#MainKeyboardView_languageOnSpacebarFinalAlpha
 * @attr ref R.styleable#MainKeyboardView_languageOnSpacebarFadeoutAnimator
 * @attr ref R.styleable#MainKeyboardView_altCodeKeyWhileTypingFadeoutAnimator
 * @attr ref R.styleable#MainKeyboardView_altCodeKeyWhileTypingFadeinAnimator
 * @attr ref R.styleable#MainKeyboardView_keyHysteresisDistance
 * @attr ref R.styleable#MainKeyboardView_touchNoiseThresholdTime
 * @attr ref R.styleable#MainKeyboardView_touchNoiseThresholdDistance
 * @attr ref R.styleable#MainKeyboardView_keySelectionByDraggingFinger
 * @attr ref R.styleable#MainKeyboardView_keyRepeatStartTimeout
 * @attr ref R.styleable#MainKeyboardView_keyRepeatInterval
 * @attr ref R.styleable#MainKeyboardView_longPressKeyTimeout
 * @attr ref R.styleable#MainKeyboardView_longPressShiftKeyTimeout
 * @attr ref R.styleable#MainKeyboardView_ignoreAltCodeKeyTimeout
 * @attr ref R.styleable#MainKeyboardView_keyPreviewLayout
 * @attr ref R.styleable#MainKeyboardView_keyPreviewOffset
 * @attr ref R.styleable#MainKeyboardView_keyPreviewHeight
 * @attr ref R.styleable#MainKeyboardView_keyPreviewLingerTimeout
 * @attr ref R.styleable#MainKeyboardView_keyPreviewShowUpAnimator
 * @attr ref R.styleable#MainKeyboardView_keyPreviewDismissAnimator
 * @attr ref R.styleable#MainKeyboardView_moreKeysKeyboardLayout
 * @attr ref R.styleable#MainKeyboardView_moreKeysKeyboardForActionLayout
 * @attr ref R.styleable#MainKeyboardView_backgroundDimAlpha
 * @attr ref R.styleable#MainKeyboardView_showMoreKeysKeyboardAtTouchPoint
 * @attr ref R.styleable#MainKeyboardView_gestureFloatingPreviewTextLingerTimeout
 * @attr ref R.styleable#MainKeyboardView_gestureStaticTimeThresholdAfterFastTyping
 * @attr ref R.styleable#MainKeyboardView_gestureDetectFastMoveSpeedThreshold
 * @attr ref R.styleable#MainKeyboardView_gestureDynamicThresholdDecayDuration
 * @attr ref R.styleable#MainKeyboardView_gestureDynamicTimeThresholdFrom
 * @attr ref R.styleable#MainKeyboardView_gestureDynamicTimeThresholdTo
 * @attr ref R.styleable#MainKeyboardView_gestureDynamicDistanceThresholdFrom
 * @attr ref R.styleable#MainKeyboardView_gestureDynamicDistanceThresholdTo
 * @attr ref R.styleable#MainKeyboardView_gestureSamplingMinimumDistance
 * @attr ref R.styleable#MainKeyboardView_gestureRecognitionMinimumTime
 * @attr ref R.styleable#MainKeyboardView_gestureRecognitionSpeedThreshold
 * @attr ref R.styleable#MainKeyboardView_suppressKeyPreviewAfterBatchInputDuration
 */
public final class MainKeyboardView extends KeyboardView implements DrawingProxy,
        MoreKeysPanel.Controller {
    private static final String TAG = MainKeyboardView.class.getSimpleName();

    /** Listener for {@link KeyboardActionListener}. */
    private KeyboardActionListener mKeyboardActionListener;

    /* Space key and its icon and background. */
    private Key mSpaceKey;
    // Stuff to draw language name on spacebar.
    private final int mLanguageOnSpacebarFinalAlpha;
    private ObjectAnimator mLanguageOnSpacebarFadeoutAnimator;
    private int mLanguageOnSpacebarFormatType;
    private boolean mHasMultipleEnabledIMEsOrSubtypes;
    private int mLanguageOnSpacebarAnimAlpha = Constants.Color.ALPHA_OPAQUE;
    private final float mLanguageOnSpacebarTextRatio;
    private float mLanguageOnSpacebarTextSize;
    private final int mLanguageOnSpacebarTextColor;
    private final float mLanguageOnSpacebarTextShadowRadius;
    private final int mLanguageOnSpacebarTextShadowColor;
    private static final float LANGUAGE_ON_SPACEBAR_TEXT_SHADOW_RADIUS_DISABLED = -1.0f;
    // The minimum x-scale to fit the language name on spacebar.
    private static final float MINIMUM_XSCALE_OF_LANGUAGE_NAME = 0.8f;

    // Spacebar swipe-affordance indicators (Hamp). All ratios are of the spacebar's
    // shorter dimension so the glyphs scale with the keyboard instead of being fixed px.
    private static final float INDICATOR_SIZE_RATIO = 0.11f;    // chevron arm length
    private static final float INDICATOR_STROKE_RATIO = 0.035f; // line thickness
    private static final float INDICATOR_END_INSET_RATIO = 0.035f; // inset from the bar's ends
    private static final int INDICATOR_ALPHA = 90;              // faint: affordance, not label

    // Reused across draw calls; allocating a Path on the draw path would churn the heap.
    private Path mSpacebarIndicatorPath;

    // Stuff to draw altCodeWhileTyping keys.
    private final ObjectAnimator mAltCodeKeyWhileTypingFadeoutAnimator;
    private final ObjectAnimator mAltCodeKeyWhileTypingFadeinAnimator;
    private int mAltCodeKeyWhileTypingAnimAlpha = Constants.Color.ALPHA_OPAQUE;

    // Drawing preview placer view
    private final DrawingPreviewPlacerView mDrawingPreviewPlacerView;
    private final int[] mOriginCoords = CoordinateUtils.newInstance();
    private final GestureFloatingTextDrawingPreview mGestureFloatingTextDrawingPreview;
    private final GestureTrailsDrawingPreview mGestureTrailsDrawingPreview;
    private final SlidingKeyInputDrawingPreview mSlidingKeyInputDrawingPreview;

    // Key preview
    private final KeyPreviewDrawParams mKeyPreviewDrawParams;
    private final KeyPreviewChoreographer mKeyPreviewChoreographer;

    // More keys keyboard
    private final Paint mBackgroundDimAlphaPaint = new Paint();
    private final View mMoreKeysKeyboardContainer;
    private final WeakHashMap<Key, Keyboard> mMoreKeysKeyboardCache = new WeakHashMap<>();
    private final boolean mConfigShowMoreKeysKeyboardAtTouchedPoint;
    // More keys panel (used by both more keys keyboard and more suggestions view)
    // TODO: Consider extending to support multiple more keys panels
    private MoreKeysPanel mMoreKeysPanel;

    // Gesture floating preview text
    // TODO: Make this parameter customizable by user via settings.
    private int mGestureFloatingPreviewTextLingerTimeout;

    public final KeyDetector mKeyDetector;
    private final NonDistinctMultitouchHelper mNonDistinctMultitouchHelper;

    private float mLanguageSwipeProgress;
    private Pair<String, String> mSurroundingLanguages;

    private final TimerHandler mTimerHandler;
    private final int mLanguageOnSpacebarHorizontalMargin;

    private MainKeyboardAccessibilityDelegate mAccessibilityDelegate;

    public MainKeyboardView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.mainKeyboardViewStyle);
    }

    public MainKeyboardView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final DrawingPreviewPlacerView drawingPreviewPlacerView =
                new DrawingPreviewPlacerView(context, attrs);

        final TypedArray mainKeyboardViewAttr = context.obtainStyledAttributes(
                attrs, R.styleable.MainKeyboardView, defStyle, R.style.MainKeyboardView);
        final int ignoreAltCodeKeyTimeout = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_ignoreAltCodeKeyTimeout, 0);
        final int gestureRecognitionUpdateTime = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_gestureRecognitionUpdateTime, 0);
        mTimerHandler = new TimerHandler(
                this, ignoreAltCodeKeyTimeout, gestureRecognitionUpdateTime);

        final float keyHysteresisDistance = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_keyHysteresisDistance, 0.0f);
        final float keyHysteresisDistanceForSlidingModifier = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_keyHysteresisDistanceForSlidingModifier, 0.0f);
        mKeyDetector = new KeyDetector(
                keyHysteresisDistance, keyHysteresisDistanceForSlidingModifier);

        PointerTracker.init(mainKeyboardViewAttr, mTimerHandler, this /* DrawingProxy */);

        final boolean forceNonDistinctMultitouch = false;
        final boolean hasDistinctMultitouch = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)
                && !forceNonDistinctMultitouch;
        mNonDistinctMultitouchHelper = hasDistinctMultitouch ? null
                : new NonDistinctMultitouchHelper();

        final int backgroundDimAlpha = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_backgroundDimAlpha, 0);
        mBackgroundDimAlphaPaint.setColor(Color.BLACK);
        mBackgroundDimAlphaPaint.setAlpha(backgroundDimAlpha);
        mLanguageOnSpacebarTextRatio = mainKeyboardViewAttr.getFraction(
                R.styleable.MainKeyboardView_languageOnSpacebarTextRatio, 1, 1, 1.0f);
        mLanguageOnSpacebarTextColor = DynamicThemeProvider.Companion.getColorOrDefault(
                R.styleable.MainKeyboardView_languageOnSpacebarTextColor, 0,
                mainKeyboardViewAttr, mDrawableProvider
        );
        mLanguageOnSpacebarTextShadowRadius = mainKeyboardViewAttr.getFloat(
                R.styleable.MainKeyboardView_languageOnSpacebarTextShadowRadius,
                LANGUAGE_ON_SPACEBAR_TEXT_SHADOW_RADIUS_DISABLED);
        mLanguageOnSpacebarTextShadowColor = mainKeyboardViewAttr.getColor(
                R.styleable.MainKeyboardView_languageOnSpacebarTextShadowColor, 0);
        mLanguageOnSpacebarFinalAlpha = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_languageOnSpacebarFinalAlpha,
                Constants.Color.ALPHA_OPAQUE);
        final int languageOnSpacebarFadeoutAnimatorResId = mainKeyboardViewAttr.getResourceId(
                R.styleable.MainKeyboardView_languageOnSpacebarFadeoutAnimator, 0);
        final int altCodeKeyWhileTypingFadeoutAnimatorResId = mainKeyboardViewAttr.getResourceId(
                R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeoutAnimator, 0);
        final int altCodeKeyWhileTypingFadeinAnimatorResId = mainKeyboardViewAttr.getResourceId(
                R.styleable.MainKeyboardView_altCodeKeyWhileTypingFadeinAnimator, 0);

        mKeyPreviewDrawParams = new KeyPreviewDrawParams(mainKeyboardViewAttr, mDrawableProvider);
        mKeyPreviewChoreographer = new KeyPreviewChoreographer(mKeyPreviewDrawParams);

        final int moreKeysKeyboardLayoutId = mainKeyboardViewAttr.getResourceId(
                R.styleable.MainKeyboardView_moreKeysKeyboardLayout, 0);
        mConfigShowMoreKeysKeyboardAtTouchedPoint = mainKeyboardViewAttr.getBoolean(
                R.styleable.MainKeyboardView_showMoreKeysKeyboardAtTouchedPoint, false);

        mGestureFloatingPreviewTextLingerTimeout = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_gestureFloatingPreviewTextLingerTimeout, 0);

        mGestureFloatingTextDrawingPreview = new GestureFloatingTextDrawingPreview(
                mainKeyboardViewAttr);
        mGestureFloatingTextDrawingPreview.setDrawingView(drawingPreviewPlacerView);

        mGestureTrailsDrawingPreview = new GestureTrailsDrawingPreview(mainKeyboardViewAttr, mDrawableProvider);
        mGestureTrailsDrawingPreview.setDrawingView(drawingPreviewPlacerView);

        mSlidingKeyInputDrawingPreview = new SlidingKeyInputDrawingPreview(mainKeyboardViewAttr, mDrawableProvider);
        mSlidingKeyInputDrawingPreview.setDrawingView(drawingPreviewPlacerView);
        mainKeyboardViewAttr.recycle();

        mDrawingPreviewPlacerView = drawingPreviewPlacerView;

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        mMoreKeysKeyboardContainer = inflater.inflate(moreKeysKeyboardLayoutId, null);
        mLanguageOnSpacebarFadeoutAnimator = loadObjectAnimator(
                languageOnSpacebarFadeoutAnimatorResId, this);
        mAltCodeKeyWhileTypingFadeoutAnimator = loadObjectAnimator(
                altCodeKeyWhileTypingFadeoutAnimatorResId, this);
        mAltCodeKeyWhileTypingFadeinAnimator = loadObjectAnimator(
                altCodeKeyWhileTypingFadeinAnimatorResId, this);

        mKeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER;

        mLanguageOnSpacebarHorizontalMargin = (int)getResources().getDimension(
                R.dimen.config_language_on_spacebar_horizontal_margin);
    }

    @Override
    public void setHardwareAcceleratedDrawingEnabled(final boolean enabled) {
        super.setHardwareAcceleratedDrawingEnabled(enabled);
        mDrawingPreviewPlacerView.setHardwareAcceleratedDrawingEnabled(enabled);
    }

    private ObjectAnimator loadObjectAnimator(final int resId, final Object target) {
        if (resId == 0) {
            // TODO: Stop returning null.
            return null;
        }
        final ObjectAnimator animator = (ObjectAnimator)AnimatorInflater.loadAnimator(
                getContext(), resId);
        if (animator != null) {
            animator.setTarget(target);
        }
        return animator;
    }

    private static void cancelAndStartAnimators(final ObjectAnimator animatorToCancel,
            final ObjectAnimator animatorToStart) {
        if (animatorToCancel == null || animatorToStart == null) {
            // TODO: Stop using null as a no-operation animator.
            return;
        }
        float startFraction = 0.0f;
        if (animatorToCancel.isStarted()) {
            animatorToCancel.cancel();
            startFraction = 1.0f - animatorToCancel.getAnimatedFraction();
        }
        final long startTime = (long)(animatorToStart.getDuration() * startFraction);
        animatorToStart.start();
        animatorToStart.setCurrentPlayTime(startTime);
    }

    // Implements {@link DrawingProxy#startWhileTypingAnimation(int)}.
    /**
     * Called when a while-typing-animation should be started.
     * @param fadeInOrOut {@link DrawingProxy#FADE_IN} starts while-typing-fade-in animation.
     * {@link DrawingProxy#FADE_OUT} starts while-typing-fade-out animation.
     */
    @Override
    public void startWhileTypingAnimation(final int fadeInOrOut) {
        switch (fadeInOrOut) {
        case DrawingProxy.FADE_IN:
            cancelAndStartAnimators(
                    mAltCodeKeyWhileTypingFadeoutAnimator, mAltCodeKeyWhileTypingFadeinAnimator);
            break;
        case DrawingProxy.FADE_OUT:
            cancelAndStartAnimators(
                    mAltCodeKeyWhileTypingFadeinAnimator, mAltCodeKeyWhileTypingFadeoutAnimator);
            break;
        }
    }

    @ExternallyReferenced
    public int getLanguageOnSpacebarAnimAlpha() {
        return mLanguageOnSpacebarAnimAlpha;
    }

    @ExternallyReferenced
    public void setLanguageOnSpacebarAnimAlpha(final int alpha) {
        mLanguageOnSpacebarAnimAlpha = alpha;
        invalidateKey(mSpaceKey);
    }

    @ExternallyReferenced
    public int getAltCodeKeyWhileTypingAnimAlpha() {
        return mAltCodeKeyWhileTypingAnimAlpha;
    }

    @ExternallyReferenced
    public void setAltCodeKeyWhileTypingAnimAlpha(final int alpha) {
        if (mAltCodeKeyWhileTypingAnimAlpha == alpha) {
            return;
        }
        // Update the visual of alt-code-key-while-typing.
        mAltCodeKeyWhileTypingAnimAlpha = alpha;
        final Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            return;
        }
        for (final Key key : keyboard.mAltCodeKeysWhileTyping) {
            invalidateKey(key);
        }
    }

    public void setKeyboardActionListener(final KeyboardActionListener listener) {
        mKeyboardActionListener = listener;
        PointerTracker.setKeyboardActionListener(listener);
    }

    // TODO: We should reconsider which coordinate system should be used to represent keyboard
    // event.
    public int getKeyX(final int x) {
        return Constants.isValidCoordinate(x) ? mKeyDetector.getTouchX(x) : x;
    }

    // TODO: We should reconsider which coordinate system should be used to represent keyboard
    // event.
    public int getKeyY(final int y) {
        return Constants.isValidCoordinate(y) ? mKeyDetector.getTouchY(y) : y;
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     * @see Keyboard
     * @see #getKeyboard()
     * @param keyboard the keyboard to display in this view
     */
    @Override
    public void setKeyboard(final Keyboard keyboard) {
        // Remove any pending messages, except dismissing preview and key repeat.
        mTimerHandler.cancelLongPressTimers();
        super.setKeyboard(keyboard);
        mKeyDetector.setKeyboard(
                keyboard, -getPaddingLeft(), -getPaddingTop() + getVerticalCorrection());
        PointerTracker.setKeyDetector(mKeyDetector);
        mMoreKeysKeyboardCache.clear();

        mSpaceKey = keyboard.getKey(Constants.CODE_SPACE);
        final int keyHeight = keyboard.mMostCommonKeyHeight - keyboard.mVerticalGap;
        mLanguageOnSpacebarTextSize = Math.min(keyHeight, keyboard.mMostCommonKeyWidth) * mLanguageOnSpacebarTextRatio;

        if (AccessibilityUtils.getInstance().isAccessibilityEnabled()) {
            if (mAccessibilityDelegate == null) {
                mAccessibilityDelegate = new MainKeyboardAccessibilityDelegate(this, mKeyDetector);
            }
            mAccessibilityDelegate.setKeyboard(keyboard);
        } else {
            mAccessibilityDelegate = null;
        }
    }

    /**
     * Enables or disables the key preview popup. This is a popup that shows a magnified
     * version of the depressed key. By default the preview is enabled.
     * @param previewEnabled whether or not to enable the key feedback preview
     * @param delay the delay after which the preview is dismissed
     */
    public void setKeyPreviewPopupEnabled(final boolean previewEnabled, final int delay) {
        mKeyPreviewDrawParams.setPopupEnabled(previewEnabled, delay);
    }

    /**
     * Enables or disables the key preview popup animations and set animations' parameters.
     *
     * @param hasCustomAnimationParams false to use the default key preview popup animations
     *   specified by keyPreviewShowUpAnimator and keyPreviewDismissAnimator attributes.
     *   true to override the default animations with the specified parameters.
     * @param showUpStartXScale from this x-scale the show up animation will start.
     * @param showUpStartYScale from this y-scale the show up animation will start.
     * @param showUpDuration the duration of the show up animation in milliseconds.
     * @param dismissEndXScale to this x-scale the dismiss animation will end.
     * @param dismissEndYScale to this y-scale the dismiss animation will end.
     * @param dismissDuration the duration of the dismiss animation in milliseconds.
     */
    public void setKeyPreviewAnimationParams(final boolean hasCustomAnimationParams,
            final float showUpStartXScale, final float showUpStartYScale, final int showUpDuration,
            final float dismissEndXScale, final float dismissEndYScale, final int dismissDuration) {
        mKeyPreviewDrawParams.setAnimationParams(hasCustomAnimationParams,
                showUpStartXScale, showUpStartYScale, showUpDuration,
                dismissEndXScale, dismissEndYScale, dismissDuration);
    }

    private void locatePreviewPlacerView() {
        getLocationInWindow(mOriginCoords);
        mDrawingPreviewPlacerView.setKeyboardViewGeometry(mOriginCoords, getWidth(), getHeight());
    }

    private void installPreviewPlacerView() {
        final View rootView = getRootView();
        if (rootView == null) {
            Log.w(TAG, "Cannot find root view");
            return;
        }
        final ViewGroup windowContentView = (ViewGroup)rootView.findViewById(android.R.id.content);
        // Note: It'd be very weird if we get null by android.R.id.content.
        if (windowContentView == null) {
            Log.w(TAG, "Cannot find android.R.id.content view to add DrawingPreviewPlacerView");
            return;
        }

        if(mDrawingPreviewPlacerView.getParent() != null) {
            ((ViewGroup)mDrawingPreviewPlacerView.getParent()).removeView(mDrawingPreviewPlacerView);
        }
        windowContentView.addView(mDrawingPreviewPlacerView);
    }

    // Implements {@link DrawingProxy#onKeyPressed(Key,boolean)}.
    @Override
    public void onKeyPressed(@Nonnull final Key key, final boolean withPreview) {
        key.onPressed();
        invalidateKey(key);
        if (withPreview && !key.getNoKeyPreview()) {
            showKeyPreview(key);
        }
    }

    private void showKeyPreview(@Nonnull final Key key) {
        final Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            return;
        }
        final KeyPreviewDrawParams previewParams = mKeyPreviewDrawParams;
        if (!previewParams.isPopupEnabled()) {
            previewParams.setVisibleOffset(-keyboard.mVerticalGap);
            return;
        }

        locatePreviewPlacerView();
        getLocationInWindow(mOriginCoords);
        mKeyPreviewChoreographer.placeAndShowKeyPreview(key, keyboard.mIconsSet, getKeyDrawParams(),
                getWidth(), mOriginCoords, mDrawingPreviewPlacerView, isHardwareAccelerated(),
                mDrawableProvider.getPreviewBackground(keyboard, key));
    }

    private void dismissKeyPreviewWithoutDelay(@Nonnull final Key key) {
        mKeyPreviewChoreographer.dismissKeyPreview(key, false /* withAnimation */);
        invalidateKey(key);
    }

    // Implements {@link DrawingProxy#onKeyReleased(Key,boolean)}.
    @Override
    public void onKeyReleased(@Nonnull final Key key, final boolean withAnimation) {
        key.onReleased();
        invalidateKey(key);
        if (!key.getNoKeyPreview()) {
            if (withAnimation) {
                dismissKeyPreview(key);
            } else {
                dismissKeyPreviewWithoutDelay(key);
            }
        }
    }

    private void dismissKeyPreview(@Nonnull final Key key) {
        if (isHardwareAccelerated()) {
            mKeyPreviewChoreographer.dismissKeyPreview(key, true /* withAnimation */);
            return;
        }
        // TODO: Implement preference option to control key preview method and duration.
        mTimerHandler.postDismissKeyPreview(key, mKeyPreviewDrawParams.getLingerTimeout());
    }

    public void setSlidingKeyInputPreviewEnabled(final boolean enabled) {
        mSlidingKeyInputDrawingPreview.setPreviewEnabled(enabled);
    }

    @Override
    public void showSlidingKeyInputPreview(@Nullable final PointerTracker tracker) {
        locatePreviewPlacerView();
        if (tracker != null) {
            mSlidingKeyInputDrawingPreview.setPreviewPosition(tracker);
        } else {
            mSlidingKeyInputDrawingPreview.dismissSlidingKeyInputPreview();
        }
    }

    private void setGesturePreviewMode(final boolean isGestureTrailEnabled,
            final boolean isGestureFloatingPreviewTextEnabled) {
        mGestureFloatingTextDrawingPreview.setPreviewEnabled(isGestureFloatingPreviewTextEnabled);
        mGestureTrailsDrawingPreview.setPreviewEnabled(isGestureTrailEnabled);
    }

    public void showGestureFloatingPreviewText(@Nonnull final SuggestedWords suggestedWords,
            final boolean dismissDelayed) {
        locatePreviewPlacerView();
        final GestureFloatingTextDrawingPreview gestureFloatingTextDrawingPreview =
                mGestureFloatingTextDrawingPreview;
        gestureFloatingTextDrawingPreview.setSuggetedWords(suggestedWords);
        if (dismissDelayed) {
            mTimerHandler.postDismissGestureFloatingPreviewText(
                    mGestureFloatingPreviewTextLingerTimeout);
        }
    }

    // Implements {@link DrawingProxy#dismissGestureFloatingPreviewTextWithoutDelay()}.
    @Override
    public void dismissGestureFloatingPreviewTextWithoutDelay() {
        mGestureFloatingTextDrawingPreview.dismissGestureFloatingPreviewText();
    }

    @Override
    public void showGestureTrail(@Nonnull final PointerTracker tracker,
            final boolean showsFloatingPreviewText) {
        locatePreviewPlacerView();
        if (showsFloatingPreviewText) {
            mGestureFloatingTextDrawingPreview.setPreviewPosition(tracker);
        }
        mGestureTrailsDrawingPreview.setPreviewPosition(tracker);
    }

    // Note that this method is called from a non-UI thread.
    @SuppressWarnings("static-method")
    public void setImeAllowsGestureInput(final boolean value) {
        PointerTracker.setImeAllowsGestureInput(value);
    }

    public void setGestureHandlingEnabledByUser(final boolean isGestureHandlingEnabledByUser,
            final boolean isGestureTrailEnabled,
            final boolean isGestureFloatingPreviewTextEnabled) {
        PointerTracker.setGestureHandlingEnabledByUser(isGestureHandlingEnabledByUser);
        setGesturePreviewMode(isGestureHandlingEnabledByUser && isGestureTrailEnabled,
                isGestureHandlingEnabledByUser && isGestureFloatingPreviewTextEnabled);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        installPreviewPlacerView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDrawingPreviewPlacerView.removeAllViews();
    }

    // Implements {@link DrawingProxy@showMoreKeysKeyboard(Key,PointerTracker)}.
    @Override
    @Nullable
    public MoreKeysPanel showMoreKeysKeyboard(@Nonnull final Key key,
            @Nonnull final PointerTracker tracker) {
        final List<MoreKeySpec> moreKeys = key.getMoreKeys();
        if (moreKeys.isEmpty()) {
            return null;
        }
        Keyboard moreKeysKeyboard = mMoreKeysKeyboardCache.get(key);
        if (moreKeysKeyboard == null) {
            // {@link KeyPreviewDrawParams#mPreviewVisibleWidth} should have been set at
            // {@link KeyPreviewChoreographer#placeKeyPreview(Key,TextView,KeyboardIconsSet,KeyDrawParams,int,int[]},
            // though there may be some chances that the value is zero. <code>width == 0</code>
            // will cause zero-division error at
            // {@link MoreKeysKeyboardParams#setParameters(int,int,int,int,int,int,boolean,int)}.
            final boolean isSingleMoreKeyWithPreview = false && mKeyPreviewDrawParams.isPopupEnabled()
                    && !key.getNoKeyPreview() && moreKeys.size() == 1
                    && mKeyPreviewDrawParams.getVisibleWidth() > 0;
            final MoreKeysKeyboard.Builder builder = new MoreKeysKeyboard.Builder(
                    getContext(), key, getKeyboard(), isSingleMoreKeyWithPreview,
                    mKeyPreviewDrawParams.getVisibleWidth(),
                    mKeyPreviewDrawParams.getVisibleHeight(), newLabelPaint(key));
            moreKeysKeyboard = builder.build();
            mMoreKeysKeyboardCache.put(key, moreKeysKeyboard);
        }

        final View container = mMoreKeysKeyboardContainer;
        final MoreKeysKeyboardView moreKeysKeyboardView =
                (MoreKeysKeyboardView)container.findViewById(R.id.more_keys_keyboard_view);
        moreKeysKeyboardView.setKeyboard(moreKeysKeyboard);
        container.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final int[] lastCoords = CoordinateUtils.newInstance();
        tracker.getLastCoordinates(lastCoords);

        final int bottomPadding = mKeyPreviewChoreographer.getBottomPaddingForKey(getContext(), key);

        final int pointX = key.getDrawX() + key.getDrawWidth() / 2;
        final int pointY = key.getY() + key.getHeight() - bottomPadding;

        moreKeysKeyboardView.showMoreKeysPanel(this, this, pointX, pointY, mKeyboardActionListener, lastCoords, key.isFastLongPress());
        return moreKeysKeyboardView;
    }

    public boolean isInDraggingFinger() {
        if (isShowingMoreKeysPanel()) {
            return true;
        }
        return PointerTracker.isAnyInDraggingFinger();
    }

    @Override
    public void onShowMoreKeysPanel(final MoreKeysPanel panel) {
        AudioAndHapticFeedbackManager.getInstance().performHapticFeedback(
                this, true);

        locatePreviewPlacerView();
        // Dismiss another {@link MoreKeysPanel} that may be being showed.
        onDismissMoreKeysPanel();
        // Dismiss all key previews that may be being showed.
        PointerTracker.setReleasedKeyGraphicsToAllKeys();
        // Dismiss sliding key input preview that may be being showed.
        mSlidingKeyInputDrawingPreview.dismissSlidingKeyInputPreview();
        panel.showInParent(mDrawingPreviewPlacerView);
        mMoreKeysPanel = panel;
    }

    public boolean isShowingMoreKeysPanel() {
        return mMoreKeysPanel != null && mMoreKeysPanel.isShowingInParent();
    }

    @Override
    public void onCancelMoreKeysPanel() {
        PointerTracker.dismissAllMoreKeysPanels();
    }

    @Override
    public void onDismissMoreKeysPanel() {
        if (isShowingMoreKeysPanel()) {
            mMoreKeysPanel.removeFromParent();
            mMoreKeysPanel = null;
        }
    }

    public void startDoubleTapShiftKeyTimer() {
        mTimerHandler.startDoubleTapShiftKeyTimer();
    }

    public void cancelDoubleTapShiftKeyTimer() {
        mTimerHandler.cancelDoubleTapShiftKeyTimer();
    }

    public boolean isInDoubleTapShiftKeyTimeout() {
        return mTimerHandler.isInDoubleTapShiftKeyTimeout();
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (getKeyboard() == null) {
            return false;
        }
        if (mNonDistinctMultitouchHelper != null) {
            if (event.getPointerCount() > 1 && mTimerHandler.isInKeyRepeat()) {
                // Key repeating timer will be canceled if 2 or more keys are in action.
                mTimerHandler.cancelKeyRepeatTimers();
            }
            // Non distinct multitouch screen support
            mNonDistinctMultitouchHelper.processMotionEvent(event, mKeyDetector);
            return true;
        }
        return processMotionEvent(event);
    }

    public boolean processMotionEvent(final MotionEvent event) {
        final int index = event.getActionIndex();
        final int id = event.getPointerId(index);
        final PointerTracker tracker = PointerTracker.getPointerTracker(id);
        // When a more keys panel is showing, we should ignore other fingers' single touch events
        // other than the finger that is showing the more keys panel.
        if (isShowingMoreKeysPanel() && !tracker.isShowingMoreKeysPanel()
                && PointerTracker.getActivePointerTrackerCount() == 1) {
            return true;
        }
        tracker.processMotionEvent(event, mKeyDetector);
        return true;
    }

    public void cancelAllOngoingEvents() {
        mTimerHandler.cancelAllMessages();
        PointerTracker.setReleasedKeyGraphicsToAllKeys();
        mGestureFloatingTextDrawingPreview.dismissGestureFloatingPreviewText();
        mSlidingKeyInputDrawingPreview.dismissSlidingKeyInputPreview();
        PointerTracker.dismissAllMoreKeysPanels();
        PointerTracker.cancelAllPointerTrackers();
    }

    public void closing() {
        cancelAllOngoingEvents();
        mMoreKeysKeyboardCache.clear();
    }

    public void onHideWindow() {
        onDismissMoreKeysPanel();
        final MainKeyboardAccessibilityDelegate accessibilityDelegate = mAccessibilityDelegate;
        if (accessibilityDelegate != null
                && AccessibilityUtils.getInstance().isAccessibilityEnabled()) {
            accessibilityDelegate.onHideWindow();
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return (mAccessibilityDelegate != null && AccessibilityUtils.getInstance().isTouchExplorationEnabled() && mAccessibilityDelegate.dispatchHoverEvent(event))
                || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return (mAccessibilityDelegate != null && AccessibilityUtils.getInstance().isTouchExplorationEnabled() && mAccessibilityDelegate.dispatchKeyEvent(event))
                || super.dispatchKeyEvent(event);
    }

    @Override
    public void onFocusChanged(boolean gainFocus, int direction,
                               Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if(mAccessibilityDelegate != null && AccessibilityUtils.getInstance().isTouchExplorationEnabled()) {
            mAccessibilityDelegate.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        }
    }

    public void updateShortcutKey(final boolean available) {
        // TODO: Remove
    }

    public void startDisplayLanguageOnSpacebar(final boolean subtypeChanged,
            final int languageOnSpacebarFormatType,
            final boolean hasMultipleEnabledIMEsOrSubtypes) {
        if (subtypeChanged) {
            KeyPreviewView.clearTextCache();
        }
        mLanguageOnSpacebarFormatType = languageOnSpacebarFormatType;
        mHasMultipleEnabledIMEsOrSubtypes = hasMultipleEnabledIMEsOrSubtypes;
        final ObjectAnimator animator = mLanguageOnSpacebarFadeoutAnimator;
        if (animator == null) {
            mLanguageOnSpacebarFormatType = LanguageOnSpacebarUtils.FORMAT_TYPE_NONE;
        } else {
            if (subtypeChanged
                    && languageOnSpacebarFormatType != LanguageOnSpacebarUtils.FORMAT_TYPE_NONE) {
                setLanguageOnSpacebarAnimAlpha(Constants.Color.ALPHA_OPAQUE);
                if (animator.isStarted()) {
                    animator.cancel();
                }
                animator.start();
            } else {
                if (!animator.isStarted()) {
                    mLanguageOnSpacebarAnimAlpha = mLanguageOnSpacebarFinalAlpha;
                }
            }
        }
        invalidateKey(mSpaceKey);
    }

    @Override
    protected void onDrawKeyTopVisuals(final Key key, final Canvas canvas, final Paint paint,
           final KeyDrawParams params, final KeyDrawingConfiguration kdc,
                                       final int keyWidth, final int keyHeight) {
        if (key.getAltCodeWhileTyping() && key.isEnabled()) {
            params.mAnimAlpha = mAltCodeKeyWhileTypingAnimAlpha;
        }
        super.onDrawKeyTopVisuals(key, canvas, paint, params, kdc, keyWidth, keyHeight);
        final int code = key.getCode();
        if (code == Constants.CODE_SPACE && (key.getIconId().equals("space_key") || mLanguageSwipeProgress != 0.0f)) {
            drawLanguageOnSpacebar(key, canvas, paint, kdc.getHintColor());
            drawSwipeIndicatorsOnSpacebar(key, canvas, paint, kdc.getHintColor());
            // Whether space key needs to show the "..." popup hint for special purposes
            if (key.isLongPressEnabled() && mHasMultipleEnabledIMEsOrSubtypes) {
                drawKeyPopupHint(key, canvas, paint, params);
            }
        } else if (code == Constants.CODE_LANGUAGE_SWITCH) {
            drawKeyPopupHint(key, canvas, paint, params);
        }
    }

    private boolean fitsTextIntoWidth(final int width, final String text, final Paint paint) {
        final int maxTextWidth = width - mLanguageOnSpacebarHorizontalMargin * 2;
        paint.setTextScaleX(1.0f);
        final float textWidth = TypefaceUtils.getStringWidth(text, paint);
        if (textWidth < width) {
            return true;
        }

        final float scaleX = maxTextWidth / textWidth;
        if (scaleX < MINIMUM_XSCALE_OF_LANGUAGE_NAME) {
            return false;
        }

        paint.setTextScaleX(scaleX);
        return TypefaceUtils.getStringWidth(text, paint) < maxTextWidth;
    }

    // Layout language name on spacebar.
    private String layoutLanguageOnSpacebar(final Paint paint,
                                            final Locale locale, final int width, final int horizontalWidth) {
        if (mLanguageOnSpacebarFormatType == LanguageOnSpacebarUtils.FORMAT_TYPE_NONE) {
            return "";
        }

        paint.setTextScaleX(1.0f);
        final String name = Subtypes.getLanguageOnSpaceBar(locale, horizontalWidth / paint.measureText("Q"));
        if (fitsTextIntoWidth(width, name, paint)) {
            return name;
        }

        return "";
    }

    private void drawLanguageOnSpacebar(final Key key, final Canvas canvas, final Paint paint, final int color) {
        final Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            return;
        }
        int width = key.getWidth();
        int height = key.getHeight();
        canvas.save();
        if(key.getUseVerticalSwipe()) {
            canvas.rotate(-90.0f);
            canvas.translate(-height, 0.0f);
            width = key.getHeight();
            height = key.getWidth();
        }
        paint.setTextAlign(Align.CENTER);
        paint.setTypeface(mDrawableProvider.selectKeyTypeface(Typeface.DEFAULT));
        paint.setTextSize(mLanguageOnSpacebarTextSize);
        final String language = layoutLanguageOnSpacebar(paint, keyboard.mId.mLocale, width, key.getWidth());
        // Draw language text with shadow
        final float descent = paint.descent();
        final float textHeight = -paint.ascent() + descent;
        final float baseline = height / 2 + textHeight / 2;
        if (mLanguageOnSpacebarTextShadowRadius > 0.0f) {
            paint.setShadowLayer(mLanguageOnSpacebarTextShadowRadius, 0, 0,
                    mLanguageOnSpacebarTextShadowColor);
        } else {
            paint.clearShadowLayer();
        }
        paint.setColor(color);
        paint.setAlpha(mLanguageOnSpacebarAnimAlpha);

        final float ratio = Math.min(1.0f, (width * 0.90f) /
                TypefaceUtils.getStringWidth(language, paint));

        paint.setTextScaleX(ratio);
        canvas.clipRect(0, 0, width, height);
        if(mLanguageSwipeProgress == 0.0f || mSurroundingLanguages == null) {
            canvas.drawText(language, width / 2, baseline - descent, paint);
        } else {
            float sign = Math.signum(mLanguageSwipeProgress);

            float offs = width * 0.75f;
            float x = width / 2.0f + Math.clamp((mLanguageSwipeProgress*0.75f + 0.2f * sign) * width / 2.0f, -offs, offs);

            float x2 = x - offs*sign;

            if(language.length() <= 2 && key.getUseVerticalSwipe()) {
                canvas.save();
                canvas.rotate(90.0f, x, height / 2);
                canvas.drawText(language, x, baseline - descent, paint);
                canvas.restore();
            } else {
                canvas.drawText(language, x, baseline - descent, paint);
            }

            float alpha = Math.clamp((Math.abs(mLanguageSwipeProgress) - 1.0f) / 0.15f, 0.0f, 1.0f) * 175 + 80;
            paint.setAlpha((int)alpha);
            if(mLanguageSwipeProgress < 0.0f) {
                canvas.drawText(mSurroundingLanguages.getFirst(), x2, baseline - descent, paint);
            } else {
                canvas.drawText(mSurroundingLanguages.getSecond(), x2, baseline - descent, paint);
            }
        }
        canvas.restore();
        paint.clearShadowLayer();
        paint.setTextScaleX(1.0f);
    }

    /**
     * Draws faint directional indicators on the spacebar advertising the swipe gestures
     * that are actually available.
     *
     * Hamp turns the spacebar into a cursor-control surface, but a gesture with no
     * affordance is a gesture nobody discovers, so the key hints at what it can do:
     *
     *   - Horizontal chevrons at the left and right edges: swiping sideways moves the
     *     cursor by a character. Drawn ONLY when the spacebar swipe mode is actually set
     *     to cursor movement - in LANGUAGE mode a sideways swipe switches subtype, and in
     *     OFF mode it does nothing, so drawing arrows in either case would be a lie.
     *   - A small stacked chevron pair at one edge: swiping up/down moves the cursor by a
     *     line. This gesture is unconditional in Hamp (see PointerTracker#onMovePointer-
     *     Vertical), so the hint is likewise unconditional.
     *
     * Everything is drawn at reduced alpha and inset at the edges: the language name owns
     * the centre of the key, and during a language swipe it animates across the full
     * width, so indicators are suppressed entirely while that animation is running.
     */
    private void drawSwipeIndicatorsOnSpacebar(final Key key, final Canvas canvas,
            final Paint paint, final int color) {
        // The language-swipe animation sweeps text across the whole key; stay out of its way.
        if (mLanguageSwipeProgress != 0.0f) {
            return;
        }

        // onDraw can run before settings are loaded, in which case there is nothing to
        // reflect yet. Skip this frame rather than risk an NPE on the draw path.
        final Settings settings = Settings.getInstance();
        if (settings == null) {
            return;
        }
        final SettingsValues settingsValues = settings.getCurrent();
        if (settingsValues == null) {
            return;
        }

        final boolean horizontalMovesCursor =
                settingsValues.mSpacebarSwipeMode == Settings.SPACEBAR_MODE_CURSOR;

        int width = key.getWidth();
        int height = key.getHeight();

        canvas.save();
        // Mirror drawLanguageOnSpacebar's handling of a vertically-oriented spacebar so
        // the indicators stay aligned with the key rather than the screen.
        if (key.getUseVerticalSwipe()) {
            canvas.rotate(-90.0f);
            canvas.translate(-height, 0.0f);
            width = key.getHeight();
            height = key.getWidth();
        }
        canvas.clipRect(0, 0, width, height);

        // Size the glyphs from the key so they scale with the keyboard rather than being
        // fixed pixels that look wrong on a tablet or in a resized layout.
        //
        // Careful with orientation: for a vertical spacebar the rotation above sets
        // width = key.getHeight() and height = key.getWidth(), so in drawing space
        // `width` is the NARROW axis (roughly 52) and `height` the long one (roughly 360)
        // - the opposite of the horizontal case. The gesture axes swap with it: what the
        // user perceives as "along the bar" is the y axis here.
        //
        // So the chevrons are laid out along whichever axis is long, and the pair that
        // indicates the along-the-bar gesture is placed at its two far ends, with the
        // cross-axis pair kept near one end. That keeps both sets clear of the centre,
        // where the language name is drawn.
        final boolean rotated = key.getUseVerticalSwipe();
        final float longAxis = Math.max(width, height);
        final float shortAxis = Math.min(width, height);
        final float armLength = shortAxis * INDICATOR_SIZE_RATIO;
        final float strokeWidth = Math.max(1.0f, shortAxis * INDICATOR_STROKE_RATIO);
        // Inset scales with the long axis so the glyphs sit near the ends of the bar in
        // both orientations instead of drifting toward the middle on a narrow key.
        final float inset = Math.max(armLength * 1.2f, longAxis * INDICATOR_END_INSET_RATIO);
        final float crossCentre = shortAxis / 2.0f;

        final Paint.Style oldStyle = paint.getStyle();
        final float oldStrokeWidth = paint.getStrokeWidth();
        final Paint.Cap oldCap = paint.getStrokeCap();
        final Paint.Join oldJoin = paint.getStrokeJoin();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.clearShadowLayer();
        paint.setColor(color);
        // Deliberately faint: an affordance, not a label.
        paint.setAlpha(INDICATOR_ALPHA);

        if (mSpacebarIndicatorPath == null) {
            mSpacebarIndicatorPath = new Path();
        }
        final Path path = mSpacebarIndicatorPath;

        // Chevron helper coordinates are expressed as (alongAxis, crossAxis) and mapped to
        // real canvas x/y at the end, so one set of maths serves both orientations.
        //
        //   horizontal bar : along = x, cross = y
        //   vertical bar   : along = y, cross = x
        final float alongEndA = inset;
        final float alongEndB = longAxis - inset;

        if (horizontalMovesCursor) {
            // Pair pointing outward at the two far ends of the bar: the along-the-bar
            // gesture (character-wise cursor movement).
            drawChevron(canvas, paint, path, rotated,
                    alongEndA + armLength, crossCentre - armLength,
                    alongEndA, crossCentre,
                    alongEndA + armLength, crossCentre + armLength);
            drawChevron(canvas, paint, path, rotated,
                    alongEndB - armLength, crossCentre - armLength,
                    alongEndB, crossCentre,
                    alongEndB - armLength, crossCentre + armLength);
        }

        // Cross-axis pair (line-wise cursor movement). Tucked in from one end so it never
        // collides with the along-axis chevrons above or the language name in the centre.
        final float crossPairAlong = horizontalMovesCursor
                ? alongEndA + armLength * 4.0f
                : alongEndA + armLength;
        final float gap = armLength * 0.9f;

        drawChevron(canvas, paint, path, rotated,
                crossPairAlong - armLength, crossCentre - gap,
                crossPairAlong, crossCentre - gap - armLength,
                crossPairAlong + armLength, crossCentre - gap);
        drawChevron(canvas, paint, path, rotated,
                crossPairAlong - armLength, crossCentre + gap,
                crossPairAlong, crossCentre + gap + armLength,
                crossPairAlong + armLength, crossCentre + gap);

        canvas.restore();

        // Restore paint state: this Paint instance is shared across the whole key-drawing
        // pass, so leaving it in STROKE mode would corrupt every subsequent key.
        paint.setStyle(oldStyle);
        paint.setStrokeWidth(oldStrokeWidth);
        paint.setStrokeCap(oldCap);
        paint.setStrokeJoin(oldJoin);
        paint.setAlpha(Constants.Color.ALPHA_OPAQUE);
    }

    /**
     * Strokes a three-point chevron given (along, cross) coordinates.
     *
     * When {@code rotated} is true the along/cross pair is emitted as (cross, along),
     * which maps the same layout onto a vertical spacebar without duplicating the maths.
     */
    private static void drawChevron(final Canvas canvas, final Paint paint, final Path path,
            final boolean rotated,
            final float a1, final float c1,
            final float a2, final float c2,
            final float a3, final float c3) {
        path.reset();
        if (rotated) {
            path.moveTo(c1, a1);
            path.lineTo(c2, a2);
            path.lineTo(c3, a3);
        } else {
            path.moveTo(a1, c1);
            path.lineTo(a2, c2);
            path.lineTo(a3, c3);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public void deallocateMemory() {
        super.deallocateMemory();
        mDrawingPreviewPlacerView.deallocateMemory();
    }

    public void updateStateHint(StateHint stateHint) {
        PointerTracker.setStateHint(stateHint);
    }

    public void updateSwipeLanguageProgress(float progress) {
        mLanguageSwipeProgress = progress;
        if (mLanguageSwipeProgress == 0.0f) {
            mSurroundingLanguages = null;
        } else if(mSurroundingLanguages == null) {
            mSurroundingLanguages = Subtypes.INSTANCE.getSurroundingLanguages(getContext());
        }

        invalidateKey(mSpaceKey);
    }
}
