package com.example.sp_v1.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    /**
     * Converts a long timestamp into a readable date string.
     * @param timestamp The timestamp in milliseconds.
     * @return A formatted date string like "Oct 13, 2025".
     */
    public static String formatTimestamp(long timestamp) {
        if (timestamp == 0) {
            return "N/A";
        }
        // Create a Date object from the timestamp
        Date date = new Date(timestamp);
        // Define the desired date format
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        // Format the date and return it as a string
        return sdf.format(date);
    }
}