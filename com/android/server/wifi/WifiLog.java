package com.android.server.wifi;

import com.google.errorprone.annotations.CompileTimeConstant;
import javax.annotation.CheckReturnValue;

public interface WifiLog {
    public static final char PLACEHOLDER = '%';

    public interface LogMessage {
        @CheckReturnValue
        /* renamed from: c */
        LogMessage mo2068c(char c);

        @CheckReturnValue
        /* renamed from: c */
        LogMessage mo2069c(long j);

        @CheckReturnValue
        /* renamed from: c */
        LogMessage mo2070c(String str);

        @CheckReturnValue
        /* renamed from: c */
        LogMessage mo2071c(boolean z);

        void flush();

        @CheckReturnValue
        /* renamed from: r */
        LogMessage mo2073r(String str);
    }

    /* renamed from: d */
    void mo2084d(String str);

    @CheckReturnValue
    LogMessage dump(@CompileTimeConstant String str);

    /* renamed from: e */
    void mo2086e(String str);

    /* renamed from: eC */
    void mo2087eC(@CompileTimeConstant String str);

    @CheckReturnValue
    LogMessage err(@CompileTimeConstant String str);

    /* renamed from: i */
    void mo2089i(String str);

    /* renamed from: iC */
    void mo2090iC(@CompileTimeConstant String str);

    @CheckReturnValue
    LogMessage info(@CompileTimeConstant String str);

    /* renamed from: tC */
    void mo2092tC(@CompileTimeConstant String str);

    @CheckReturnValue
    LogMessage trace(@CompileTimeConstant String str);

    @CheckReturnValue
    LogMessage trace(String str, int i);

    /* renamed from: v */
    void mo2095v(String str);

    /* renamed from: w */
    void mo2096w(String str);

    /* renamed from: wC */
    void mo2097wC(@CompileTimeConstant String str);

    @CheckReturnValue
    LogMessage warn(@CompileTimeConstant String str);
}
