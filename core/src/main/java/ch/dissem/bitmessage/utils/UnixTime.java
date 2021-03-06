/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.utils;

/**
 * A simple utility class that simplifies using the second based time used in Bitmessage.
 */
public class UnixTime {
    /**
     * Length of a minute in seconds, intended for use with {@link #now(long)}.
     */
    public static final int MINUTE = 60;
    /**
     * Length of an hour in seconds, intended for use with {@link #now(long)}.
     */
    public static final long HOUR = 60 * MINUTE;
    /**
     * Length of a day in seconds, intended for use with {@link #now(long)}.
     */
    public static final long DAY = 24 * HOUR;

    /**
     * @return the time in second based Unix time ({@link System#currentTimeMillis()}/1000)
     */
    public static long now() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Same as {@link #now()} + shiftSeconds, but might be more readable.
     *
     * @param shiftSeconds number of seconds from now we're interested in
     * @return the Unix time in shiftSeconds seconds / shiftSeconds seconds ago
     */
    public static long now(long shiftSeconds) {
        return (System.currentTimeMillis() / 1000) + shiftSeconds;
    }
}
