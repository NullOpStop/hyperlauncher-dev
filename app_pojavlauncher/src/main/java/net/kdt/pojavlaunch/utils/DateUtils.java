package net.kdt.pojavlaunch.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateUtils {

    /**
     * Parses the date string from the Minecraft version list.
     * @param releaseTime the date string in ISO 8601 format or yyyy-MM-dd
     * @return a Date object representing the release time
     * @throws ParseException if the date string is not in a recognized format
     */
    public static Date parseReleaseDate(String releaseTime) throws ParseException {
        if(releaseTime == null) return null;
        int tIndexOf = releaseTime.indexOf('T');
        if(tIndexOf != -1) releaseTime = releaseTime.substring(0, tIndexOf);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(releaseTime);
    }

    /**
     * Checks if the Date object is before the date denoted by
     * year, month, dayOfMonth parameters
     * @param date the Date object that we compare against
     * @param year the year
     * @param month the month (zero-based)
     * @param dayOfMonth the day of the month
     * @return true if the Date is before year, month, dayOfMonth, false otherwise
     */
    public static boolean dateBefore(@Nullable Date date, int year, int month, int dayOfMonth) {
        if (date == null) return false;
        return date.before(new Date(new GregorianCalendar(year, month, dayOfMonth).getTimeInMillis()));
    }

    /**
     * Extracts the original release date of a game version, ignoring any mods (if present)
     * @param gameVersion the JMinecraftVersionList.Version object
     * @return the game's original release date
     */
    public static Date getOriginalReleaseDate(JMinecraftVersionList.Version gameVersion) throws ParseException {
        if(Tools.isValidString(gameVersion.inheritsFrom)) {
            gameVersion = Tools.getVersionInfo(gameVersion.inheritsFrom, true);
        }else {
            gameVersion = Tools.getVersionInfo(gameVersion.id, true);
        }
        return parseReleaseDate(gameVersion.releaseTime);
    }
}
