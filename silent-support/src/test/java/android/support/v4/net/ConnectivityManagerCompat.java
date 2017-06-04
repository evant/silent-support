package android.support.v4.net;

import android.net.ConnectivityManager;

public class ConnectivityManagerCompat {
    public static boolean record_isActiveNetworkMetered;

    public static void reset() {
        record_isActiveNetworkMetered = false;
    }

    public static boolean isActiveNetworkMetered(ConnectivityManager con) {
        record_isActiveNetworkMetered = true;
        return true;
    }
}
