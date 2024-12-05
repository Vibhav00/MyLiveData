package com.vk.mylivedata.ld;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import java.util.Iterator;
import java.util.Map;


public abstract class MyCustomLd<T> {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mDataLock = new Object();
    static final int START_VERSION = -1;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final Object NOT_SET = new Object();

    @SuppressLint("RestrictedApi")
    private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers =
            new SafeIterableMap<>();

    // how many observers are in active state
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            int mActiveCount = 0;
    // to handle active/inactive reentry, we guard with this boolean
    private boolean mChangingActiveState;
    private volatile Object mData;
    // when setData is called, we set the pending data and actual data swap happens on the main
    // thread
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    volatile Object mPendingData = NOT_SET;
    private int mVersion;

    private boolean mDispatchingValue;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean mDispatchInvalidated;
    private final Runnable mPostValueRunnable = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Object newValue;
            synchronized (mDataLock) {
                newValue = mPendingData;
                mPendingData = NOT_SET;
            }
            setValue((T) newValue);
        }
    };

    /**
     * Creates a LiveData initialized with the given {@code value}.
     *
     * @param value initial value
     */
    public MyCustomLd(T value) {
        mData = value;
        mVersion = START_VERSION + 1;
    }

    /**
     * Creates a MyCustomLd with no value assigned to it.
     */
    public MyCustomLd() {
        mData = NOT_SET;
        mVersion = START_VERSION;
    }

    @SuppressWarnings("unchecked")
    private void considerNotify(MyCustomLd.ObserverWrapper observer) {
        if (!observer.mActive) {
            return;
        }
        // Check latest state b4 dispatch. Maybe it changed state but we didn't get the event yet.
        //
        // we still first check observer.active to keep it as the entrance for events. So even if
        // the observer moved to an active state, if we've not received that event, we better not
        // notify for a more predictable notification order.
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false);
            return;
        }
        if (observer.mLastVersion >= mVersion) {
            return;
        }
        observer.mLastVersion = mVersion;
        observer.mObserver.onChanged((T) mData);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dispatchingValue(@Nullable MyCustomLd.ObserverWrapper initiator) {
        if (mDispatchingValue) {
            mDispatchInvalidated = true;
            return;
        }
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {
                for (@SuppressLint("RestrictedApi") Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
                     mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                    considerNotify(iterator.next().getValue());
                    if (mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }


    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        assertMainThread("observe");
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        MyCustomLd.LifecycleBoundObserver wrapper = new MyCustomLd.LifecycleBoundObserver(owner, observer);
        @SuppressLint("RestrictedApi") MyCustomLd.ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }


    @MainThread
    public void observeForever(@NonNull Observer<? super T> observer) {
        assertMainThread("observeForever");
        MyCustomLd.AlwaysActiveObserver wrapper = new MyCustomLd.AlwaysActiveObserver(observer);
        @SuppressLint("RestrictedApi") MyCustomLd.ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing instanceof MyCustomLd.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    @MainThread
    public void removeObserver(@NonNull final Observer<? super T> observer) {
        assertMainThread("removeObserver");
        @SuppressLint("RestrictedApi") MyCustomLd.ObserverWrapper removed = mObservers.remove(observer);
        if (removed == null) {
            return;
        }
        removed.detachObserver();
        removed.activeStateChanged(false);
    }


    @SuppressWarnings("WeakerAccess")
    @MainThread
    public void removeObservers(@NonNull final LifecycleOwner owner) {
        assertMainThread("removeObservers");
        for (Map.Entry<Observer<? super T>, ObserverWrapper> entry : mObservers) {
            if (entry.getValue().isAttachedTo(owner)) {
                removeObserver(entry.getKey());
            }
        }
    }


    @SuppressLint("RestrictedApi")
    protected void postValue(T value) {
        boolean postTask;
        synchronized (mDataLock) {
            postTask = mPendingData == NOT_SET;
            mPendingData = value;
        }
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }


    @MainThread
    protected void setValue(T value) {
        assertMainThread("setValue");
        mVersion++;
        mData = value;
        dispatchingValue(null);
    }


    @SuppressWarnings("unchecked")
    @Nullable
    public T getValue() {
        Object data = mData;
        if (data != NOT_SET) {
            return (T) data;
        }
        return null;
    }


    public boolean isInitialized() {
        return mData != NOT_SET;
    }

    int getVersion() {
        return mVersion;
    }


    protected void onActive() {

    }


    protected void onInactive() {

    }


    @SuppressLint("RestrictedApi")
    @SuppressWarnings("WeakerAccess")
    public boolean hasObservers() {
        return mObservers.size() > 0;
    }


    @SuppressLint("RestrictedApi")
    public int observerCount() {
        return mObservers.size();
    }


    @SuppressWarnings("WeakerAccess")
    public boolean hasActiveObservers() {
        return mActiveCount > 0;
    }

    @MainThread
    void changeActiveCounter(int change) {
        int previousActiveCount = mActiveCount;
        mActiveCount += change;
        if (mChangingActiveState) {
            return;
        }
        mChangingActiveState = true;
        try {
            while (previousActiveCount != mActiveCount) {
                boolean needToCallActive = previousActiveCount == 0 && mActiveCount > 0;
                boolean needToCallInactive = previousActiveCount > 0 && mActiveCount == 0;
                previousActiveCount = mActiveCount;
                if (needToCallActive) {
                    onActive();
                } else if (needToCallInactive) {
                    onInactive();
                }
            }
        } finally {
            mChangingActiveState = false;
        }
    }

    class LifecycleBoundObserver extends MyCustomLd.ObserverWrapper implements LifecycleEventObserver {
        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            mOwner = owner;
        }

         /**
          *  if previous state is Resumed and current state is Started that means the
          *  observer is paused .
          * **/
        @Override
        boolean shouldBeActive() {
            // check if observer is paused
            if (prevState1 != null) {
                if (prevState1 == Lifecycle.State.RESUMED && currentState1 == STARTED) {
                    prevState1 = currentState1;
                    return false;
                }
            }
            prevState1 = currentState1;
            return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
        }

        Lifecycle.State prevState = null;
        Lifecycle.State currentState = null;

        Lifecycle.State prevState1 = null;
        Lifecycle.State currentState1 = null;

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source,
                                   @NonNull Lifecycle.Event event) {

            currentState = mOwner.getLifecycle().getCurrentState();
            if (currentState == DESTROYED) {
                removeObserver(mObserver);
                return;
            }
            prevState = null;
            currentState1 = currentState;
            while (prevState != currentState) {
                activeStateChanged(shouldBeActive());
                prevState = currentState;
                currentState = mOwner.getLifecycle().getCurrentState();
            }
        }

        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }

        @Override
        void detachObserver() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }

    private abstract class ObserverWrapper {
        final Observer<? super T> mObserver;
        boolean mActive;
        int mLastVersion = START_VERSION;

        ObserverWrapper(Observer<? super T> observer) {
            mObserver = observer;
        }

        abstract boolean shouldBeActive();

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachObserver() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            // immediately set active state, so we'd never dispatch anything to inactive
            // owner
            mActive = newActive;
            changeActiveCounter(mActive ? 1 : -1);
            if (mActive) {
                dispatchingValue(this);
            }
        }
    }

    private class AlwaysActiveObserver extends MyCustomLd.ObserverWrapper {

        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }

    @SuppressLint("RestrictedApi")
    static void assertMainThread(String methodName) {
        if (!ArchTaskExecutor.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background"
                    + " thread");
        }
    }
}
