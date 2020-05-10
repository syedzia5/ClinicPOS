package com.zcs.clinicpos;

/**
 * Created by admin on 29/10/2017.
 */

public class CIMCAppGlobals {

    private static String serverIPAddress;

    public static String getServerIPAddress() {
        return serverIPAddress;
    }

    public static void setServerIPAddress(String serverIPAddress) {
        CIMCAppGlobals.serverIPAddress = serverIPAddress;
    }
}
