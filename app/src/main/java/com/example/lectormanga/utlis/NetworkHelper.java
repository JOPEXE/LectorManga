package com.example.lectormanga.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

public class NetworkHelper {

    private static final String TAG = "NetworkHelper";

    /**
     * Verifica si hay conexión a internet
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    /**
     * Verifica si está conectado por WiFi
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null &&
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                    networkInfo.isConnected();
        }
    }
gir
    /**
     * Verifica si está conectado por datos móviles
     */
    public static boolean isMobileDataConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null &&
                    networkInfo.getType() == ConnectivityManager.TYPE_MOBILE &&
                    networkInfo.isConnected();
        }
    }

    /**
     * Obtiene el tipo de conexión como string
     */
    public static String getConnectionType(Context context) {
        if (isWifiConnected(context)) {
            return "WiFi";
        } else if (isMobileDataConnected(context)) {
            return "Datos Móviles";
        } else if (isNetworkAvailable(context)) {
            return "Otro tipo de red";
        } else {
            return "Sin conexión";
        }
    }

    /**
     * Verifica y muestra en log el estado de la red
     */
    public static void logNetworkStatus(Context context) {
        String connectionType = getConnectionType(context);
        boolean hasInternet = isNetworkAvailable(context);

        Log.d(TAG, "=== ESTADO DE RED ===");
        Log.d(TAG, "Tipo de conexión: " + connectionType);
        Log.d(TAG, "Internet disponible: " + hasInternet);
        Log.d(TAG, "WiFi: " + isWifiConnected(context));
        Log.d(TAG, "Datos móviles: " + isMobileDataConnected(context));
        Log.d(TAG, "====================");
    }

    /**
     * Muestra advertencia si está usando datos móviles
     */
    public static boolean shouldWarnAboutMobileData(Context context) {
        return isMobileDataConnected(context);
    }
}