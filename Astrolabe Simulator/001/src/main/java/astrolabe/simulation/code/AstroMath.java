package astrolabe.simulation.code;

import java.time.LocalDateTime;

public class AstroMath {
    // Calculate Julian Date
    public static double toJulianDate(LocalDateTime dateTime) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        int day = dateTime.getDayOfMonth();
        double hour = dateTime.getHour() + dateTime.getMinute()/60.0 + dateTime.getSecond()/3600.0;

        if (month <= 2) {
            year -= 1;
            month += 12;
        }

        int a = year / 100;
        int b = 2 - a + a / 4;

        return (int)(365.25 * (year + 4716)) + (int)(30.6001 * (month + 1)) + day + hour/24.0 + b - 1524.5;
    }

    // Calculate local sidereal time (in degrees)
    public static double calculateLocalSiderealTime(double longitude, LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0;

        // Greenwich mean sidereal time
        double gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
                + 0.000387933 * t * t - t * t * t / 38710000.0;

        // Normalize to 0-360 degrees
        gmst = gmst % 360;
        if (gmst < 0) gmst += 360;

        // Convert to local sidereal time
        return (gmst + longitude) % 360;
    }

    // Convert equatorial to horizontal coordinates
    public static double[] equatorialToHorizontal(double ha, double dec, double lat) {
        double haRad = Math.toRadians(ha);
        double decRad = Math.toRadians(dec);
        double latRad = Math.toRadians(lat);

        double alt = Math.asin(Math.sin(decRad) * Math.sin(latRad) +
                Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad));
        double az = Math.atan2(Math.sin(haRad),
                Math.cos(haRad) * Math.sin(latRad) - Math.tan(decRad) * Math.cos(latRad));

        return new double[] {
                Math.toDegrees(alt),
                (Math.toDegrees(az) + 180) % 360  // Convert to 0-360 range
        };
    }

    // Calculate Sun position (simplified)
    public static double[] calculateSunPosition(LocalDateTime dateTime) {
        // Simplified calculation - for more accuracy use Meeus algorithm
        int dayOfYear = dateTime.getDayOfYear();
        double eclipticLong = (dayOfYear / 365.25) * 360;
        return new double[]{eclipticLong, 0}; // longitude, latitude
    }
}