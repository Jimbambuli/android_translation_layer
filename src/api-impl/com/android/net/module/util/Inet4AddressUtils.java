/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.net.module.util;

import java.net.Inet4Address;

/**
 * Collection of utilities to work with IPv4 addresses.
 * ATL: only the methods needed by WifiInfo are ported (AOSP bodies unchanged).
 * @hide
 */
public class Inet4AddressUtils {

    /**
     * Convert an IPv4 address from an InetAddress to an integer (1.2.3.4 -> 0x01020304)
     *
     * <p>This conversion can help order IP addresses: considering the ordering
     * 192.0.2.1 < 192.0.2.2 < ..., resulting ints will follow that ordering if read as unsigned
     * integers with {@link Integer#toUnsignedLong}.
     * @param inetAddr is an InetAddress corresponding to the IPv4 address
     * @return the IP address as integer
     */
    public static int inet4AddressToIntHTH(Inet4Address inetAddr)
            throws IllegalArgumentException {
        byte [] addr = inetAddr.getAddress();
        return ((addr[0] & 0xff) << 24) | ((addr[1] & 0xff) << 16)
                | ((addr[2] & 0xff) << 8) | (addr[3] & 0xff);
    }

    /**
     * Convert a IPv4 address from an InetAddress to an integer (1.2.3.4 -> 0x04030201)
     *
     * <p>This method stores the higher-order IPv4 address bytes in the lower-order int bytes,
     * which is an unusual convention. Consider {@link #inet4AddressToIntHTH(Inet4Address)} instead.
     * @param inetAddr is an InetAddress corresponding to the IPv4 address
     * @return the IP address as integer
     */
    public static int inet4AddressToIntHTL(Inet4Address inetAddr) {
        return Integer.reverseBytes(inet4AddressToIntHTH(inetAddr));
    }
}
