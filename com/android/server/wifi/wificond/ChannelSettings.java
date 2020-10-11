package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.Objects;

public class ChannelSettings implements Parcelable {
    public static final Parcelable.Creator<ChannelSettings> CREATOR = new Parcelable.Creator<ChannelSettings>() {
        public ChannelSettings createFromParcel(Parcel in) {
            ChannelSettings result = new ChannelSettings();
            result.frequency = in.readInt();
            if (in.dataAvail() != 0) {
                Log.e(ChannelSettings.TAG, "Found trailing data after parcel parsing.");
            }
            return result;
        }

        public ChannelSettings[] newArray(int size) {
            return new ChannelSettings[size];
        }
    };
    private static final String TAG = "ChannelSettings";
    public int frequency;

    public boolean equals(Object rhs) {
        ChannelSettings channel;
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof ChannelSettings) || (channel = (ChannelSettings) rhs) == null) {
            return false;
        }
        if (this.frequency == channel.frequency) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.frequency)});
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.frequency);
    }
}
