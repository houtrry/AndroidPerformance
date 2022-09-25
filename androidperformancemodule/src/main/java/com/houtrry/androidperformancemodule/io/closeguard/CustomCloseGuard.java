package com.houtrry.androidperformancemodule.io.closeguard;

import android.util.Log;

/**
 * @author: houtrry
 * @time: 2022/9/3
 * @desc: 代码源自系统源码
 */

class CustomCloseGuard {

    /**
     * Instance used when CloseGuard is disabled to avoid allocation.
     */
    private static final CustomCloseGuard NOOP = new CustomCloseGuard();

    /**
     * Enabled by default so we can catch issues early in VM startup.
     * Note, however, that Android disables this early in its startup,
     * but enables it with DropBoxing for system apps on debug builds.
     */
    private static volatile boolean ENABLED = true;

    /**
     * Hook for customizing how CloseGuard issues are reported.
     */
    private static volatile Reporter REPORTER = new DefaultReporter();

    /**
     * The default {@link Tracker}.
     */
    private static final DefaultTracker DEFAULT_TRACKER = new DefaultTracker();

    /**
     * Hook for customizing how CloseGuard issues are tracked.
     */
    private static volatile Tracker currentTracker = DEFAULT_TRACKER;

    /**
     * Returns a CloseGuard instance. If CloseGuard is enabled, {@code
     * #open(String)} can be used to set up the instance to warn on
     * failure to close. If CloseGuard is disabled, a non-null no-op
     * instance is returned.
     *
     * @return
     */
    public static CustomCloseGuard get() {
        if (!ENABLED) {
            return NOOP;
        }
        return new CustomCloseGuard();
    }

    /**
     * Used to enable or disable CloseGuard. Note that CloseGuard only
     * warns if it is enabled for both allocation and finalization.
     *
     * @param enabled
     */
    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    /**
     * True if CloseGuard mechanism is enabled.
     *
     * @return
     */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Used to replace default Reporter used to warn of CloseGuard
     * violations. Must be non-null.
     */
    public static void setReporter(Reporter reporter) {
        if (reporter == null) {
            throw new NullPointerException("reporter == null");
        }
        REPORTER = reporter;
    }

    /**
     * Returns non-null CloseGuard.Reporter.
     *
     * @return REPORTER
     */
    public static Reporter getReporter() {
        return REPORTER;
    }

    /**
     * Sets the {@link Tracker} that is notified when resources are allocated and released.
     * <p>This is only intended for use by {@code dalvik.system.CloseGuardSupport} class and so
     * MUST NOT be used for any other purposes.
     *
     * @param tracker
     * @throws NullPointerException if tracker is null
     */
    public static void setTracker(Tracker tracker) {
        if (tracker == null) {
            throw new NullPointerException("tracker == null");
        }
        currentTracker = tracker;
    }

    /**
     * Returns {@link #setTracker(Tracker) last Tracker that was set}, or otherwise a default
     * Tracker that does nothing.
     *
     * <p>This is only intended for use by {@code dalvik.system.CloseGuardSupport} class and so
     * MUST NOT be used for any other purposes.
     */
    public static Tracker getTracker() {
        return currentTracker;
    }

    private CustomCloseGuard() {
    }

    /**
     * If CloseGuard is enabled, {@code open} initializes the instance
     * with a warning that the caller should have explicitly called the
     * {@code closer} method instead of relying on finalization.
     *
     * @param closer non-null name of explicit termination method
     * @throws NullPointerException if closer is null, regardless of
     *                              whether or not CloseGuard is enabled
     */
    public void open(String closer) {
        // always perform the check for valid API usage...
        if (closer == null) {
            throw new NullPointerException("closer == null");
        }
        // ...but avoid allocating an allocationSite if disabled
        if (this == NOOP || !ENABLED) {
            return;
        }
        String message = "Explicit termination method '" + closer + "' not called";
        allocationSite = new Throwable(message);
        currentTracker.open(allocationSite);
    }

    private Throwable allocationSite;

    /**
     * Marks this CloseGuard instance as closed to avoid warnings on
     * finalization.
     */
    public void close() {
        currentTracker.close(allocationSite);
        allocationSite = null;
    }

    /**
     * If CloseGuard is enabled, logs a warning if the caller did not
     * properly cleanup by calling an explicit close method
     * before finalization. If CloseGuard is disabled, no action is
     * performed.
     */
    public void warnIfOpen() {
        if (allocationSite == null || !ENABLED) {
            return;
        }

        String message =
                ("A resource was acquired at attached stack trace but never released. "
                        + "See java.io.Closeable for information on avoiding resource leaks.");

        REPORTER.report(message, allocationSite);
    }

    /**
     * Interface to allow customization of tracking behaviour.
     *
     * <p>This is only intended for use by {@code dalvik.system.CloseGuardSupport} class and so
     * MUST NOT be used for any other purposes.
     */
    public interface Tracker {
        void open(Throwable allocationSite);

        void close(Throwable allocationSite);
    }

    /**
     * Default tracker which does nothing special and simply leaves it up to the GC to detect a leak.
     */
    private static final class DefaultTracker implements Tracker {
        @Override
        public void open(Throwable allocationSite) {
        }

        @Override
        public void close(Throwable allocationSite) {
        }
    }

    /**
     * Interface to allow customization of reporting behavior.
     */
    public interface Reporter {
        void report(String message, Throwable allocationSite);
    }

    /**
     * Default Reporter which reports CloseGuard violations to the log.
     */
    private static final class DefaultReporter implements Reporter {
        @Override
        public void report(String message, Throwable allocationSite) {
            Log.w(message, allocationSite);
        }
    }
}
