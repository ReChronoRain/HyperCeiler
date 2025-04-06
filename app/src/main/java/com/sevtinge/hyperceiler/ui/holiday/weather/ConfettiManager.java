package com.sevtinge.hyperceiler.ui.holiday.weather;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;

import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.Confetto;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.ConfettoGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * A helper manager class for configuring a set of confetti and displaying them on the UI.
 */
public class ConfettiManager {
    public static final long INFINITE_DURATION = Long.MAX_VALUE;

    private final Random random = new Random();
    private final ConfettoGenerator confettoGenerator;
    private final ConfettiSource confettiSource;
    private final ViewGroup parentView;
    private final ConfettiView confettiView;

    private final Queue<Confetto> recycledConfetti = new LinkedList<>();
    private final List<Confetto> confetti = new ArrayList<>(300);
    private ValueAnimator animator;
    private long lastEmittedTimestamp;

    // All of the below configured values are in milliseconds despite the setter methods take them
    // in seconds as the parameters. The parameters for the setters are in seconds to allow for
    // users to better understand/visualize the dimensions.

    // Configured attributes for the entire confetti group
    private int numInitialCount;
    private long emissionDuration;
    private float emissionRate, emissionRateInverse;
    private Interpolator fadeOutInterpolator;
    private Rect bound;

    // Configured attributes for each confetto
    private float velocityX, velocityDeviationX;
    private float velocityY, velocityDeviationY;
    private float accelerationX, accelerationDeviationX;
    private float accelerationY, accelerationDeviationY;
    private Float targetVelocityX, targetVelocityXDeviation;
    private Float targetVelocityY, targetVelocityYDeviation;
    private int initialRotation, initialRotationDeviation;
    private float rotationalVelocity, rotationalVelocityDeviation;
    private float rotationalAcceleration, rotationalAccelerationDeviation;
    private Float targetRotationalVelocity, targetRotationalVelocityDeviation;
    private long ttl;

    private ConfettiAnimationListener animationListener;

    public ConfettiManager(Context context, ConfettoGenerator confettoGenerator,
                           ConfettiSource confettiSource, ViewGroup parentView) {
        this(confettoGenerator, confettiSource, parentView, ConfettiView.newInstance(context));
    }

    public ConfettiManager(ConfettoGenerator confettoGenerator,
                           ConfettiSource confettiSource, ViewGroup parentView, ConfettiView confettiView) {
        this.confettoGenerator = confettoGenerator;
        this.confettiSource = confettiSource;
        this.parentView = parentView;
        this.confettiView = confettiView;
        this.confettiView.bind(confetti);

        this.confettiView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                terminate();
            }
        });

        // Set the defaults
        this.ttl = -1;
        this.bound = new Rect(0, 0, parentView.getWidth(), parentView.getHeight());
    }

    /**
     * The number of confetti initially emitted before any time has elapsed.
     *
     * @param numInitialCount the number of initial confetti.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setNumInitialCount(int numInitialCount) {
        this.numInitialCount = numInitialCount;
        return this;
    }

    /**
     * Configures how long this manager will emit new confetti after the animation starts.
     *
     * @param emissionDurationInMillis how long to emit new confetti in millis. This value can be
     *   {@link #INFINITE_DURATION} for a never-ending emission.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setEmissionDuration(long emissionDurationInMillis) {
        this.emissionDuration = emissionDurationInMillis;
        return this;
    }

    /**
     * Configures how frequently this manager will emit new confetti after the animation starts
     * if {@link #emissionDuration} is a positive value.
     *
     * @param emissionRate the rate of emission in # of confetti per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setEmissionRate(float emissionRate) {
        this.emissionRate = emissionRate / 1000f;
        this.emissionRateInverse = 1f / this.emissionRate;
        return this;
    }

    /**
     * @see #setVelocityX(float, float)
     *
     * @param velocityX the X velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setVelocityX(float velocityX) {
        return setVelocityX(velocityX, 0f);
    }

    /**
     * Set the velocityX used by this manager. This value defines the initial X velocity
     * for the generated confetti. The actual confetti's X velocity will be
     * (velocityX +- [0, velocityDeviationX]).
     *
     * @param velocityX the X velocity in pixels per second.
     * @param velocityDeviationX the deviation from X velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setVelocityX(float velocityX, float velocityDeviationX) {
        this.velocityX = velocityX / 1000f;
        this.velocityDeviationX = velocityDeviationX / 1000f;
        return this;
    }

    /**
     * @see #setVelocityY(float, float)
     *
     * @param velocityY the Y velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setVelocityY(float velocityY) {
        return setVelocityY(velocityY, 0f);
    }

    /**
     * Set the velocityY used by this manager. This value defines the initial Y velocity
     * for the generated confetti. The actual confetti's Y velocity will be
     * (velocityY +- [0, velocityDeviationY]). A positive Y velocity means that the velocity
     * is going down (because Y coordinate increases going down).
     *
     * @param velocityY the Y velocity in pixels per second.
     * @param velocityDeviationY the deviation from Y velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setVelocityY(float velocityY, float velocityDeviationY) {
        this.velocityY = velocityY / 1000f;
        this.velocityDeviationY = velocityDeviationY / 1000f;
        return this;
    }

    /**
     * @see #setAccelerationX(float, float)
     *
     * @param accelerationX the X acceleration in pixels per second^2.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setAccelerationX(float accelerationX) {
        return setAccelerationX(accelerationX, 0f);
    }

    /**
     * Set the accelerationX used by this manager. This value defines the X acceleration
     * for the generated confetti. The actual confetti's X acceleration will be
     * (accelerationX +- [0, accelerationDeviationX]).
     *
     * @param accelerationX the X acceleration in pixels per second^2.
     * @param accelerationDeviationX the deviation from X acceleration in pixels per second^2.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setAccelerationX(float accelerationX, float accelerationDeviationX) {
        this.accelerationX = accelerationX / 1000000f;
        this.accelerationDeviationX = accelerationDeviationX / 1000000f;
        return this;
    }

    /**
     * @see #setAccelerationY(float, float)
     *
     * @param accelerationY the Y acceleration in pixels per second^2.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setAccelerationY(float accelerationY) {
        return setAccelerationY(accelerationY, 0f);
    }

    /**
     * Set the accelerationY used by this manager. This value defines the Y acceleration
     * for the generated confetti. The actual confetti's Y acceleration will be
     * (accelerationY +- [0, accelerationDeviationY]). A positive Y acceleration means that the
     * confetto will be accelerating downwards.
     *
     * @param accelerationY the Y acceleration in pixels per second^2.
     * @param accelerationDeviationY the deviation from Y acceleration in pixels per second^2.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setAccelerationY(float accelerationY, float accelerationDeviationY) {
        this.accelerationY = accelerationY / 1000000f;
        this.accelerationDeviationY = accelerationDeviationY / 1000000f;
        return this;
    }

    /**
     * @see #setTargetVelocityX(float, float)
     *
     * @param targetVelocityX the target X velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTargetVelocityX(float targetVelocityX) {
        return setTargetVelocityX(targetVelocityX, 0f);
    }

    /**
     * Set the target X velocity that confetti can reach during the animation. The actual confetti's
     * target X velocity will be (targetVelocityX +- [0, targetVelocityXDeviation]).
     *
     * @param targetVelocityX the target X velocity in pixels per second.
     * @param targetVelocityXDeviation  the deviation from target X velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTargetVelocityX(float targetVelocityX,
                                              float targetVelocityXDeviation) {
        this.targetVelocityX = targetVelocityX / 1000f;
        this.targetVelocityXDeviation = targetVelocityXDeviation / 1000f;
        return this;
    }

    /**
     * @see #setTargetVelocityY(float, float)
     *
     * @param targetVelocityY the target Y velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTargetVelocityY(float targetVelocityY) {
        return setTargetVelocityY(targetVelocityY, 0f);
    }

    /**
     * Set the target Y velocity that confetti can reach during the animation. The actual confetti's
     * target Y velocity will be (targetVelocityY +- [0, targetVelocityYDeviation]).
     *
     * @param targetVelocityY the target Y velocity in pixels per second.
     * @param targetVelocityYDeviation  the deviation from target Y velocity in pixels per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTargetVelocityY(float targetVelocityY,
                                              float targetVelocityYDeviation) {
        this.targetVelocityY = targetVelocityY / 1000f;
        this.targetVelocityYDeviation = targetVelocityYDeviation / 1000f;
        return this;
    }

    /**
     * @see #setInitialRotation(int, int)
     *
     * @param initialRotation the initial rotation in degrees.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setInitialRotation(int initialRotation) {
        return setInitialRotation(initialRotation, 0);
    }

    /**
     * Set the initialRotation used by this manager. This value defines the initial rotation in
     * degrees for the generated confetti. The actual confetti's initial rotation will be
     * (initialRotation +- [0, initialRotationDeviation]).
     *
     * @param initialRotation the initial rotation in degrees.
     * @param initialRotationDeviation the deviation from initial rotation in degrees.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setInitialRotation(int initialRotation, int initialRotationDeviation) {
        this.initialRotation = initialRotation;
        this.initialRotationDeviation = initialRotationDeviation;
        return this;
    }

    /**
     * @see #setRotationalVelocity(float, float)
     *
     * @param rotationalVelocity the initial rotational velocity in degrees per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setRotationalVelocity(float rotationalVelocity) {
        return setRotationalVelocity(rotationalVelocity, 0f);
    }

    /**
     * Set the rotationalVelocity used by this manager. This value defines the the initial
     * rotational velocity for the generated confetti. The actual confetti's initial
     * rotational velocity will be (rotationalVelocity +- [0, rotationalVelocityDeviation]).
     *
     * @param rotationalVelocity the initial rotational velocity in degrees per second.
     * @param rotationalVelocityDeviation the deviation from initial rotational velocity in
     *   degrees per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setRotationalVelocity(float rotationalVelocity,
                                                 float rotationalVelocityDeviation) {
        this.rotationalVelocity = rotationalVelocity / 1000f;
        this.rotationalVelocityDeviation = rotationalVelocityDeviation / 1000f;
        return this;
    }

    /**
     * @see #setRotationalAcceleration(float, float)
     *
     * @param rotationalAcceleration the rotational acceleration in degrees per second^2.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setRotationalAcceleration(float rotationalAcceleration) {
        return setRotationalAcceleration(rotationalAcceleration, 0f);
    }

    /**
     * Set the rotationalAcceleration used by this manager. This value defines the the
     * acceleration of the rotation for the generated confetti. The actual confetti's rotational
     * acceleration will be (rotationalAcceleration +- [0, rotationalAccelerationDeviation]).
     *
     * @param rotationalAcceleration the rotational acceleration in degrees per second^2.
     * @param rotationalAccelerationDeviation the deviation from rotational acceleration in degrees
     *   per second^2.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setRotationalAcceleration(float rotationalAcceleration,
                                                     float rotationalAccelerationDeviation) {
        this.rotationalAcceleration = rotationalAcceleration / 1000000f;
        this.rotationalAccelerationDeviation = rotationalAccelerationDeviation / 1000000f;
        return this;
    }

    /**
     * @see #setTargetRotationalVelocity(float, float)
     *
     * @param targetRotationalVelocity the target rotational velocity in degrees per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTargetRotationalVelocity(float targetRotationalVelocity) {
        return setTargetRotationalVelocity(targetRotationalVelocity, 0f);
    }

    /**
     * Set the target rotational velocity that confetti can reach during the animation. The actual
     * confetti's target rotational velocity will be
     * (targetRotationalVelocity +- [0, targetRotationalVelocityDeviation]).
     *
     * @param targetRotationalVelocity the target rotational velocity in degrees per second.
     * @param targetRotationalVelocityDeviation the deviation from target rotational velocity
     *   in degrees per second.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTargetRotationalVelocity(float targetRotationalVelocity,
                                                       float targetRotationalVelocityDeviation) {
        this.targetRotationalVelocity = targetRotationalVelocity / 1000f;
        this.targetRotationalVelocityDeviation = targetRotationalVelocityDeviation / 1000f;
        return this;
    }

    /**
     * Specifies a custom bound that the confetti will clip to. By default, the confetti will be
     * able to animate throughout the entire screen. The dimensions specified in bound is
     * global dimensions, e.g. x=0 is the top of the screen, rather than relative dimensions.
     *
     * @param bound the bound that clips the confetti as they animate.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setBound(Rect bound) {
        this.bound = bound;
        return this;
    }

    /**
     * Specifies a custom time to live for the confetti generated by this manager. When a confetti
     * reaches its time to live timer, it will disappear and terminate its animation.
     *
     * <p>The time to live value does not include the initial delay of the confetti.
     *
     * @param ttlInMillis the custom time to live in milliseconds.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTTL(long ttlInMillis) {
        this.ttl = ttlInMillis;
        return this;
    }

    /**
     * Enables fade out for all of the confetti generated by this manager. Fade out means that
     * the confetti will animate alpha according to the fadeOutInterpolator according
     * to its TTL or, if TTL is not set, its bounds.
     *
     * @param fadeOutInterpolator an interpolator that interpolates animation progress [0, 1] into
     *   an alpha value [0, 1], 0 being transparent and 1 being opaque.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager enableFadeOut(Interpolator fadeOutInterpolator) {
        this.fadeOutInterpolator = fadeOutInterpolator;
        return this;
    }

    /**
     * Disables fade out for all of the confetti generated by this manager.
     *
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager disableFadeOut() {
        this.fadeOutInterpolator = null;
        return this;
    }

    /**
     * Enables or disables touch events for the confetti generated by this manager. By enabling
     * touch, the user can touch individual confetto and drag/fling them on the screen independent
     * of their original animation state.
     *
     * @param touchEnabled whether or not to enable touch.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setTouchEnabled(boolean touchEnabled) {
        this.confettiView.setTouchEnabled(touchEnabled);
        return this;
    }

    /**
     * Sets a {@link ConfettiAnimationListener} for this confetti manager.
     *
     * @param listener the animation listener, or null to clear out the existing listener.
     * @return the confetti manager so that the set calls can be chained.
     */
    public ConfettiManager setConfettiAnimationListener(ConfettiAnimationListener listener) {
        this.animationListener = listener;
        return this;
    }

    /**
     * Start the confetti animation configured by this manager.
     *
     * @return the confetti manager itself that just started animating.
     */
    public ConfettiManager animate() {
        if (animationListener != null) {
            animationListener.onAnimationStart(this);
        }

        cleanupExistingAnimation();
        attachConfettiViewToParent();
        addNewConfetti(numInitialCount, 0);
        startNewAnimation();
        return this;
    }

    /**
     * Terminate the currently running animation if there is any.
     */
    public void terminate() {
        if (animator != null) {
            animator.cancel();
        }
        confettiView.terminate();

        if (animationListener != null) {
            animationListener.onAnimationEnd(this);
        }
    }

    private void cleanupExistingAnimation() {
        if (animator != null) {
            animator.cancel();
        }

        lastEmittedTimestamp = 0;
        final Iterator<Confetto> iterator = confetti.iterator();
        while (iterator.hasNext()) {
            removeConfetto(iterator.next());
            iterator.remove();
        }
    }

    private void attachConfettiViewToParent() {
        final ViewParent currentParent = confettiView.getParent();
        if (currentParent != null) {
            if (currentParent != parentView) {
                ((ViewGroup) currentParent).removeView(confettiView);
                parentView.addView(confettiView);
            }
        } else {
            parentView.addView(confettiView);
        }

        confettiView.reset();
    }

    private void addNewConfetti(int numConfetti, long initialDelay) {
        for (int i = 0; i < numConfetti; i++) {
            Confetto confetto = recycledConfetti.poll();
            if (confetto == null) {
                confetto = confettoGenerator.generateConfetto(random);
            }

            confetto.reset();
            configureConfetto(confetto, confettiSource, random, initialDelay);
            confetto.prepare(bound);

            addConfetto(confetto);
        }
    }

    private void startNewAnimation() {
        // Never-ending animator, we will cancel once the termination condition is reached.
        animator = ValueAnimator.ofInt(0)
                .setDuration(Long.MAX_VALUE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final long elapsedTime = valueAnimator.getCurrentPlayTime();
                processNewEmission(elapsedTime);
                updateConfetti(elapsedTime);

                if (confetti.size() == 0 && elapsedTime >= emissionDuration) {
                    terminate();
                } else {
                    confettiView.invalidate();
                }
            }
        });

        animator.start();
    }

    private void processNewEmission(long elapsedTime) {
        if (elapsedTime < emissionDuration) {
            if (lastEmittedTimestamp == 0) {
                lastEmittedTimestamp = elapsedTime;
            } else {
                final long timeSinceLastEmission = elapsedTime - lastEmittedTimestamp;

                // Randomly determine how many confetti to emit
                final int numNewConfetti = (int)
                        (random.nextFloat() * emissionRate * timeSinceLastEmission);
                if (numNewConfetti > 0) {
                    lastEmittedTimestamp += Math.round(emissionRateInverse * numNewConfetti);
                    addNewConfetti(numNewConfetti, elapsedTime);
                }
            }
        }
    }

    private void updateConfetti(long elapsedTime) {
        final Iterator<Confetto> iterator = confetti.iterator();
        while (iterator.hasNext()) {
            final Confetto confetto = iterator.next();
            if (!confetto.applyUpdate(elapsedTime)) {
                iterator.remove();
                removeConfetto(confetto);
            }
        }
    }

    private void addConfetto(Confetto confetto) {
        this.confetti.add(confetto);
        if (animationListener != null) {
            animationListener.onConfettoEnter(confetto);
        }
    }

    private void removeConfetto(Confetto confetto) {
        if (this.animationListener != null) {
            this.animationListener.onConfettoExit(confetto);
        }
        recycledConfetti.add(confetto);
    }

    protected void configureConfetto(Confetto confetto, ConfettiSource confettiSource,
                                     Random random, long initialDelay) {
        confetto.setInitialDelay(initialDelay);
        confetto.setInitialX(confettiSource.getInitialX(random.nextFloat()));
        confetto.setInitialY(confettiSource.getInitialY(random.nextFloat()));
        confetto.setInitialVelocityX(getVarianceAmount(velocityX, velocityDeviationX, random));
        confetto.setInitialVelocityY(getVarianceAmount(velocityY, velocityDeviationY, random));
        confetto.setAccelerationX(getVarianceAmount(accelerationX, accelerationDeviationX, random));
        confetto.setAccelerationY(getVarianceAmount(accelerationY, accelerationDeviationY, random));
        confetto.setTargetVelocityX(targetVelocityX == null ? null
                : getVarianceAmount(targetVelocityX, targetVelocityXDeviation, random));
        confetto.setTargetVelocityY(targetVelocityY == null ? null
                : getVarianceAmount(targetVelocityY, targetVelocityYDeviation, random));
        confetto.setInitialRotation(
                getVarianceAmount(initialRotation, initialRotationDeviation, random));
        confetto.setInitialRotationalVelocity(
                getVarianceAmount(rotationalVelocity, rotationalVelocityDeviation, random));
        confetto.setRotationalAcceleration(
                getVarianceAmount(rotationalAcceleration, rotationalAccelerationDeviation, random));
        confetto.setTargetRotationalVelocity(targetRotationalVelocity == null ? null
                : getVarianceAmount(targetRotationalVelocity, targetRotationalVelocityDeviation,
                random));
        confetto.setTTL(ttl);
        confetto.setFadeOut(fadeOutInterpolator);
    }

    private float getVarianceAmount(float base, float deviation, Random random) {
        // Normalize random to be [-1, 1] rather than [0, 1]
        return base + (deviation * (random.nextFloat() * 2 - 1));
    }

    public interface ConfettiAnimationListener {
        void onAnimationStart(ConfettiManager confettiManager);
        void onAnimationEnd(ConfettiManager confettiManager);
        void onConfettoEnter(Confetto confetto);
        void onConfettoExit(Confetto confetto);
    }

    public static class ConfettiAnimationListenerAdapter implements ConfettiAnimationListener {
        @Override public void onAnimationStart(ConfettiManager confettiManager) {}
        @Override public void onAnimationEnd(ConfettiManager confettiManager) {}
        @Override public void onConfettoEnter(Confetto confetto) {}
        @Override public void onConfettoExit(Confetto confetto) {}
    }
}
