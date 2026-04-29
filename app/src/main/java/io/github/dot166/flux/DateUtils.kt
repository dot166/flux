package io.github.dot166.flux

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.text.format.DateFormat
import androidx.core.os.ConfigurationCompat
import java.text.ParseException
import java.util.Locale

object DateUtils {

    /**
     * Converts a date string from a specified input format to the target format:
     * 'EEE, dd MMM yyyy HH:mm:ss Z'.
     * 
     * @param dateString The date string to convert.
     * @param inputFormatPattern The pattern of the input date string (e.g., "yyyy-MM-dd'T'HH:mm:ssZ").
     * @return The converted date string in 'EEE, dd MMM yyyy HH:mm:ss Z' format, or null if conversion fails.
     */
    fun convertToTargetFormat(dateString: String?, inputFormatPattern: String?, ctx: Context): String? {
        if (dateString == null || dateString.trim { it <= ' ' }
                .isEmpty() || inputFormatPattern == null || inputFormatPattern.trim { it <= ' ' }
                .isEmpty()) {
            return null
        }

        val inputFormat = SimpleDateFormat(inputFormatPattern, Locale.ENGLISH)
        inputFormat.setLenient(false)
        try {
            val date = inputFormat.parse(dateString)
            val currentLocale: Locale = ConfigurationCompat.getLocales(ctx.resources.configuration)[0]!!
            val sdf = SimpleDateFormat(DateFormat.getBestDateTimePattern(currentLocale, "EEE, dd MMM yyyy HH:mm:ss Z"), currentLocale)
            sdf.timeZone = TimeZone.getTimeZone("UTC") // this could break formatting, disable it, always show users localtime
            return sdf.format(date)
        } catch (e: ParseException) {
            System.err.println("Error parsing date string '" + dateString + "' with pattern '" + inputFormatPattern + "': " + e.message)
            return null
        } catch (e: Exception) {
            System.err.println("An unexpected error occurred during conversion: " + e.message)
            return null
        }
    }

    /**
     * Attempts to convert a date string from a predefined set of common formats
     * to the target format: 'EEE, dd MMM yyyy HH:mm:ss Z'.
     * It tries each format in order until one succeeds.
     * 
     * @param dateString The date string to convert.
     * @return The converted date string in 'EEE, dd MMM yyyy HH:mm:ss Z' format, or null if no format matches.
     */
    fun convertFromCommonFormats(dateString: String?, ctx: Context): String? {
        val commonInputFormats = arrayOf<String?>(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss Z",
            "yyyy-MM-dd HH:mm:ss",
            "MM/dd/yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
        )

        for (pattern in commonInputFormats) {
            val converted = convertToTargetFormat(dateString, pattern, ctx)
            if (converted != null) {
                return converted
            }
        }
        System.err.println("No matching format found for date string: '$dateString'")
        return null
    }

    fun convertDateToEpochSeconds(dateString: String?): Long {
        val commonInputFormats = arrayOf<String?>(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss Z",
            "yyyy-MM-dd HH:mm:ss",
            "MM/dd/yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
        )

        for (pattern in commonInputFormats) {
            if (dateString == null || dateString.trim { it <= ' ' }
                    .isEmpty() || pattern == null || pattern.trim { it <= ' ' }
                    .isEmpty()) {
                break
            }

            val inputFormat = SimpleDateFormat(pattern, Locale.ENGLISH)
            inputFormat.setLenient(false)
            try {
                val date = inputFormat.parse(dateString)
                val milliseconds = date.time
                val seconds = milliseconds / 1000
                return seconds
            } catch (e: ParseException) {
                System.err.println("Error parsing date string '" + dateString + "' with pattern '" + pattern + "': " + e.message)
                continue
            } catch (e: Exception) {
                System.err.println("An unexpected error occurred during conversion: " + e.message)
                continue
            }
        }
        System.err.println("No matching format found for date string: '$dateString'")
        return -1
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}


