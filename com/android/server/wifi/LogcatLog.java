package com.android.server.wifi;

import android.util.Log;
import com.android.internal.annotations.Immutable;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiLog;
import javax.annotation.concurrent.ThreadSafe;

@Immutable
@ThreadSafe
class LogcatLog implements WifiLog {
    private static final String[] TRACE_FRAMES_TO_IGNORE = {"getNameOfCallingMethod()", "trace()"};
    private static final DummyLogMessage sDummyLogMessage = new DummyLogMessage();
    private static volatile boolean sVerboseLogging = false;
    private final String mTag;

    LogcatLog(String tag) {
        this.mTag = tag;
    }

    public static void enableVerboseLogging(int verboseMode) {
        if (verboseMode > 0) {
            sVerboseLogging = true;
        } else {
            sVerboseLogging = false;
        }
    }

    public WifiLog.LogMessage err(String format) {
        return new RealLogMessage(6, this.mTag, format);
    }

    public WifiLog.LogMessage warn(String format) {
        return new RealLogMessage(5, this.mTag, format);
    }

    public WifiLog.LogMessage info(String format) {
        return new RealLogMessage(4, this.mTag, format);
    }

    public WifiLog.LogMessage trace(String format) {
        if (sVerboseLogging) {
            return new RealLogMessage(3, this.mTag, format, getNameOfCallingMethod(0));
        }
        return sDummyLogMessage;
    }

    public WifiLog.LogMessage trace(String format, int numFramesToIgnore) {
        if (sVerboseLogging) {
            return new RealLogMessage(3, this.mTag, format, getNameOfCallingMethod(numFramesToIgnore));
        }
        return sDummyLogMessage;
    }

    public WifiLog.LogMessage dump(String format) {
        if (sVerboseLogging) {
            return new RealLogMessage(2, this.mTag, format);
        }
        return sDummyLogMessage;
    }

    /* renamed from: eC */
    public void mo2087eC(String msg) {
        Log.e(this.mTag, msg);
    }

    /* renamed from: wC */
    public void mo2097wC(String msg) {
        Log.w(this.mTag, msg);
    }

    /* renamed from: iC */
    public void mo2090iC(String msg) {
        Log.i(this.mTag, msg);
    }

    /* renamed from: tC */
    public void mo2092tC(String msg) {
        Log.d(this.mTag, msg);
    }

    /* renamed from: e */
    public void mo2086e(String msg) {
        Log.e(this.mTag, msg);
    }

    /* renamed from: w */
    public void mo2096w(String msg) {
        Log.w(this.mTag, msg);
    }

    /* renamed from: i */
    public void mo2089i(String msg) {
        Log.i(this.mTag, msg);
    }

    /* renamed from: d */
    public void mo2084d(String msg) {
        Log.d(this.mTag, msg);
    }

    /* renamed from: v */
    public void mo2095v(String msg) {
        Log.v(this.mTag, msg);
    }

    private static class RealLogMessage implements WifiLog.LogMessage {
        private final String mFormat;
        private final int mLogLevel;
        private int mNextFormatCharPos;
        private final StringBuilder mStringBuilder;
        private final String mTag;

        RealLogMessage(int logLevel, String tag, String format) {
            this(logLevel, tag, format, (String) null);
        }

        RealLogMessage(int logLevel, String tag, String format, String prefix) {
            this.mLogLevel = logLevel;
            this.mTag = tag;
            this.mFormat = format;
            this.mStringBuilder = new StringBuilder();
            this.mNextFormatCharPos = 0;
            if (prefix != null) {
                StringBuilder sb = this.mStringBuilder;
                sb.append(prefix);
                sb.append(" ");
            }
        }

        /* renamed from: r */
        public WifiLog.LogMessage mo2073r(String value) {
            return mo2070c(value);
        }

        /* renamed from: c */
        public WifiLog.LogMessage mo2070c(String value) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(value);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        /* renamed from: c */
        public WifiLog.LogMessage mo2069c(long value) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(value);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        /* renamed from: c */
        public WifiLog.LogMessage mo2068c(char value) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(value);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        /* renamed from: c */
        public WifiLog.LogMessage mo2071c(boolean value) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(value);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        public void flush() {
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                StringBuilder sb = this.mStringBuilder;
                String str = this.mFormat;
                sb.append(str, this.mNextFormatCharPos, str.length());
            }
            Log.println(this.mLogLevel, this.mTag, this.mStringBuilder.toString());
        }

        @VisibleForTesting
        public String toString() {
            return this.mStringBuilder.toString();
        }

        private void copyUntilPlaceholder() {
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                int placeholderPos = this.mFormat.indexOf(37, this.mNextFormatCharPos);
                if (placeholderPos == -1) {
                    placeholderPos = this.mFormat.length();
                }
                this.mStringBuilder.append(this.mFormat, this.mNextFormatCharPos, placeholderPos);
                this.mNextFormatCharPos = placeholderPos;
            }
        }
    }

    private String getNameOfCallingMethod(int callerFramesToIgnore) {
        try {
            return new Throwable().getStackTrace()[TRACE_FRAMES_TO_IGNORE.length + callerFramesToIgnore].getMethodName();
        } catch (ArrayIndexOutOfBoundsException e) {
            return "<unknown>";
        }
    }
}
