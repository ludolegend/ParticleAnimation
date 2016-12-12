package com.framgia.particleanimation.particleanimation;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.framgia.particleanimation.R;
import com.framgia.particleanimation.particleanimation.initializers.AccelerationInitializer;
import com.framgia.particleanimation.particleanimation.initializers.ParticleInitializer;
import com.framgia.particleanimation.particleanimation.initializers.RotationSpeedInitializer;
import com.framgia.particleanimation.particleanimation.initializers.SpeeddModuleAndRangeInitializer;
import com.framgia.particleanimation.particleanimation.modifiers.ParticleModifier;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by framgia on 12/12/2016.
 */

public class ParticleSystem {

    private Random mRandom;
    private int[] mParentLocation;
    private ViewGroup mParentView;
    private List<ParticleModifier> mModifiers;
    private List<ParticleInitializer> mInitializers;
    private int mMaxParticles;
    private ArrayList<Particle> mParticles;
    private long mTimeToLive;
    private float mDpToPxScale;
    private long mCurrentTime = 0;

    private int mEmiterXMin;
    private int mEmiterXMax;
    private int mEmiterYMin;
    private int mEmiterYMax;

    private float mParticlesPerMilisecond;
    private int mActivatedParticles;
    private ParticleField mDrawingView;
    private long mEmitingTime;
    private final ArrayList<Particle> mActiveParticles = new ArrayList<Particle>();
    private Timer mTimer;
    private final ParticleTimerTask mTimerTask = new ParticleTimerTask(this);
    private static final long TIMMERTASK_INTERVAL = 50;

    public ParticleSystem(ViewGroup parentView, int maxParticles, long timeToLive) {
        mRandom = new Random();
        mParentLocation = new int[2];

        setParentViewGroup(parentView);

        mModifiers = new ArrayList<ParticleModifier>();
        mInitializers = new ArrayList<ParticleInitializer>();

        mMaxParticles = maxParticles;
        // Create the particles

        mParticles = new ArrayList<Particle>();
        mTimeToLive = timeToLive;

        DisplayMetrics displayMetrics = parentView.getContext().getResources().getDisplayMetrics();
        mDpToPxScale = (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);

        // draw particle
        int width = parentView.getContext().getResources().getDimensionPixelSize(R.dimen.dp_8);
        int height = parentView.getContext().getResources().getDimensionPixelSize(R.dimen.dp_20);
        int[] colorList = parentView.getContext().getResources().getIntArray(R.array.color_list);
        int colorListLength = colorList.length;
        Bitmap bitmap;
        Canvas canvas;
        for (int i=0; i<mMaxParticles; i++) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            canvas.drawColor(colorList[mRandom.nextInt(colorListLength)]);
            mParticles.add(new Particle(bitmap));
        }
    }

    public ParticleSystem(Activity a, int maxParticles, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, timeToLive);
    }

    public ParticleSystem(Activity a, int maxParticles, long timeToLive) {
        this(a, maxParticles, timeToLive, android.R.id.content);
    }

    public ParticleSystem setParentViewGroup(ViewGroup viewGroup) {
        mParentView = viewGroup;
        if (mParentView != null) {
            mParentView.getLocationInWindow(mParentLocation);
        }
        return this;
    }

    public ParticleSystem setStartTime(long time) {
        mCurrentTime = time;
        return this;
    }

    public ParticleSystem setSpeedModuleAndAngleRange(float speedMin, float speedMax, int minAngle, int maxAngle) {
        // else emitting from top (270°) to bottom (90°) range would not be possible if someone
        // entered minAngle = 270 and maxAngle=90 since the module would swap the values
        while (maxAngle < minAngle) {
            maxAngle += 360;
        }
        mInitializers.add(new SpeeddModuleAndRangeInitializer(dpToPx(speedMin), dpToPx(speedMax), minAngle, maxAngle));
        return this;
    }

    public float dpToPx(float dp) {
        return dp * mDpToPxScale;
    }

    public ParticleSystem setRotationSpeed(float rotationSpeed) {
        mInitializers.add(new RotationSpeedInitializer(rotationSpeed, rotationSpeed));
        return this;
    }

    public ParticleSystem setAcceleration(float acceleration, int angle) {
        mInitializers.add(new AccelerationInitializer(acceleration, acceleration, angle, angle));
        return this;
    }

    public void emit(View emiter, int particlesPerSecond) {
        // Setup emiter
        emitWithGravity(emiter, Gravity.CENTER, particlesPerSecond);
    }

    public void emitWithGravity (View emiter, int gravity, int particlesPerSecond) {
        // Setup emiter
        configureEmiter(emiter, gravity);
        startEmiting(particlesPerSecond);
    }

    private void configureEmiter(View emiter, int gravity) {
        // It works with an emision range
        int[] location = new int[2];
        emiter.getLocationInWindow(location);

        // Check horizontal gravity and set range
        if (hasGravity(gravity, Gravity.LEFT)) {
            mEmiterXMin = location[0] - mParentLocation[0];
            mEmiterXMax = mEmiterXMin;
        }
        else if (hasGravity(gravity, Gravity.RIGHT)) {
            mEmiterXMin = location[0] + emiter.getWidth() - mParentLocation[0];
            mEmiterXMax = mEmiterXMin;
        }
        else if (hasGravity(gravity, Gravity.CENTER_HORIZONTAL)){
            mEmiterXMin = location[0] + emiter.getWidth()/2 - mParentLocation[0];
            mEmiterXMax = mEmiterXMin;
        }
        else {
            // All the range
            mEmiterXMin = location[0] - mParentLocation[0];
            mEmiterXMax = location[0] + emiter.getWidth() - mParentLocation[0];
        }

        // Now, vertical gravity and range
        if (hasGravity(gravity, Gravity.TOP)) {
            mEmiterYMin = location[1] - mParentLocation[1];
            mEmiterYMax = mEmiterYMin;
        }
        else if (hasGravity(gravity, Gravity.BOTTOM)) {
            mEmiterYMin = location[1] + emiter.getHeight() - mParentLocation[1];
            mEmiterYMax = mEmiterYMin;
        }
        else if (hasGravity(gravity, Gravity.CENTER_VERTICAL)){
            mEmiterYMin = location[1] + emiter.getHeight()/2 - mParentLocation[1];
            mEmiterYMax = mEmiterYMin;
        }
        else {
            // All the range
            mEmiterYMin = location[1] - mParentLocation[1];
            mEmiterYMax = location[1] + emiter.getHeight() - mParentLocation[1];
        }
    }

    private boolean hasGravity(int gravity, int gravityToCheck) {
        return (gravity & gravityToCheck) == gravityToCheck;
    }

    private void startEmiting(int particlesPerSecond) {
        mActivatedParticles = 0;
        mParticlesPerMilisecond = particlesPerSecond/1000f;
        // Add a full size view to the parent view
        mDrawingView = new ParticleField(mParentView.getContext());
        mParentView.addView(mDrawingView);
        mEmitingTime = -1; // Meaning infinite
        mDrawingView.setParticles (mActiveParticles);
        updateParticlesBeforeStartTime(particlesPerSecond);
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, 0, TIMMERTASK_INTERVAL);
    }

    private void updateParticlesBeforeStartTime(int particlesPerSecond) {
        if (particlesPerSecond == 0) {
            return;
        }
        long currentTimeInMs = mCurrentTime / 1000;
        long framesCount = currentTimeInMs / particlesPerSecond;
        if (framesCount == 0) {
            return;
        }
        long frameTimeInMs = mCurrentTime / framesCount;
        for (int i = 1; i <= framesCount; i++) {
            onUpdate(frameTimeInMs * i + 1);
        }
    }

    private static class ParticleTimerTask extends TimerTask {

        private final WeakReference<ParticleSystem> mPs;

        public ParticleTimerTask(ParticleSystem ps) {
            mPs = new WeakReference<ParticleSystem>(ps);
        }

        @Override
        public void run() {
            if(mPs.get() != null) {
                ParticleSystem ps = mPs.get();
                ps.onUpdate(ps.mCurrentTime);
                ps.mCurrentTime += TIMMERTASK_INTERVAL;
            }
        }
    }

    private void onUpdate(long miliseconds) {
        while (((mEmitingTime > 0 && miliseconds < mEmitingTime)|| mEmitingTime == -1) && // This point should emit
                !mParticles.isEmpty() && // We have particles in the pool
                mActivatedParticles < mParticlesPerMilisecond*miliseconds) { // and we are under the number of particles that should be launched
            // Activate a new particle
            activateParticle(miliseconds);
        }
        synchronized(mActiveParticles) {
            for (int i = 0; i < mActiveParticles.size(); i++) {
                boolean active = mActiveParticles.get(i).update(miliseconds);
                if (!active) {
                    Particle p = mActiveParticles.remove(i);
                    i--; // Needed to keep the index at the right position
                    mParticles.add(p);
                }
            }
        }
        mDrawingView.postInvalidate();
    }

    private void activateParticle(long delay) {
        Particle p = mParticles.remove(0);
        p.init();
        // Initialization goes before configuration, scale is required before can be configured properly
        for (int i=0; i<mInitializers.size(); i++) {
            mInitializers.get(i).initParticle(p, mRandom);
        }
        int particleX = getFromRange(mEmiterXMin, mEmiterXMax);
        int particleY = getFromRange(mEmiterYMin, mEmiterYMax);
        p.configure(mTimeToLive, particleX, particleY);
        p.activate(delay, mModifiers);
        mActiveParticles.add(p);
        mActivatedParticles++;
    }

    private int getFromRange(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        if (minValue < maxValue) {
            return mRandom.nextInt(maxValue - minValue) + minValue;
        }
        else {
            return mRandom.nextInt(minValue - maxValue) + maxValue;
        }
    }

    public void stopEmitting () {
        mEmitingTime = mCurrentTime;
    }

}


