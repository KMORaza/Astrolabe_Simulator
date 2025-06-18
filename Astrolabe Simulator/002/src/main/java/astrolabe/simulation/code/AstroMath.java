package astrolabe.simulation.code;

import java.time.LocalDateTime;

public class AstroMath {
    // Constants for Meeus algorithms
    private static final double ARC_SEC_TO_DEG = 1.0 / 3600.0;
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double RAD_TO_DEG = 180.0 / Math.PI;

    // Calculate Julian Date (more precise version)
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

    // Calculate Sun position using Meeus algorithm (high precision)
    public static double[] calculateSunPosition(LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0; // Julian centuries since J2000.0

        // Geometric mean longitude (degrees)
        double L0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t;
        L0 = normalizeDegrees(L0);

        // Mean anomaly (degrees)
        double M = 357.52911 + 35999.05029 * t - 0.0001537 * t * t;
        M = normalizeDegrees(M);
        double M_rad = Math.toRadians(M);

        // Equation of center
        double C = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(M_rad)
                + (0.019993 - 0.000101 * t) * Math.sin(2 * M_rad)
                + 0.000289 * Math.sin(3 * M_rad);

        // True longitude (degrees)
        double trueLong = L0 + C;

        // Apparent longitude (corrected for nutation and aberration)
        double omega = 125.04 - 1934.136 * t;
        double lambda = trueLong - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        // Ecliptic latitude is negligible for Sun (always < 1 arc second)
        double beta = 0.0;

        // Distance in AU (for apparent diameter calculation)
        double distance = 1.000001018 * (1 - 0.016708617 * Math.cos(M_rad)
                - 0.000139611 * Math.cos(2 * M_rad));

        // Apparent diameter in degrees (average ~0.5334°)
        double apparentDiameter = 0.5334 / distance;

        return new double[]{lambda, beta, apparentDiameter};
    }

    // Calculate Moon position using Meeus algorithm (simplified)
    public static double[] calculateMoonPosition(LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0; // Julian centuries since J2000.0

        // Mean elongation (degrees)
        double D = 297.8501921 + 445267.1114034 * t
                - 0.0018819 * t * t + t * t * t / 545868.0;
        D = normalizeDegrees(D);

        // Mean anomaly (Sun)
        double M = 357.5291092 + 35999.0502909 * t
                - 0.0001536 * t * t + t * t * t / 24490000.0;
        M = normalizeDegrees(M);

        // Mean anomaly (Moon)
        double M_prime = 134.9633964 + 477198.8675055 * t
                + 0.0087414 * t * t + t * t * t / 69699.0;
        M_prime = normalizeDegrees(M_prime);

        // Moon's argument of latitude
        double F = 93.2720950 + 483202.0175233 * t
                - 0.0036539 * t * t - t * t * t / 3526000.0;
        F = normalizeDegrees(F);

        // Ecliptic longitude (degrees)
        double lambda = 218.3164477 + 481267.88123421 * t
                - 0.0015786 * t * t + t * t * t / 538841.0
                - t * t * t * t / 65194000.0;

        // Periodic terms for longitude and distance
        double[] periodicTerms = calculateLunarPeriodicTerms(D, M, M_prime, F);
        lambda += periodicTerms[0];

        // Ecliptic latitude (degrees)
        double beta = periodicTerms[1];

        // Distance in Earth radii (for apparent diameter calculation)
        double distance = 60.2685 - 3.1385 * Math.cos(Math.toRadians(M_prime));

        // Apparent diameter in degrees (average ~0.5181°)
        double apparentDiameter = 0.5181 * (60.2685 / distance);

        return new double[]{lambda, beta, apparentDiameter};
    }

    // Helper method for lunar periodic terms
    private static double[] calculateLunarPeriodicTerms(double D, double M, double M_prime, double F) {
        // This is a simplified version with only the largest terms
        double longitude = 6.2886 * Math.sin(Math.toRadians(M_prime))
                + 1.2740 * Math.sin(Math.toRadians(2 * D - M_prime))
                + 0.6583 * Math.sin(Math.toRadians(2 * D))
                + 0.2136 * Math.sin(Math.toRadians(2 * M_prime));

        double latitude = 5.1282 * Math.sin(Math.toRadians(F))
                + 0.2806 * Math.sin(Math.toRadians(M_prime + F))
                + 0.2777 * Math.sin(Math.toRadians(M_prime - F));

        return new double[]{longitude, latitude};
    }

    // Normalize angles to 0-360 degrees
    private static double normalizeDegrees(double degrees) {
        degrees = degrees % 360;
        return degrees < 0 ? degrees + 360 : degrees;
    }

    // Calculate nutation in longitude and obliquity (simplified)
    public static double[] calculateNutation(double t) {
        // Mean elongation of the Moon
        double D = 297.85036 + 445267.111480 * t;

        // Sun's mean anomaly
        double M = 357.52772 + 35999.050340 * t;

        // Moon's mean anomaly
        double M_prime = 134.96298 + 477198.867398 * t;

        // Moon's argument of latitude
        double F = 93.27191 + 483202.017538 * t;

        // Longitude of ascending node
        double omega = 125.04452 - 1934.136261 * t;

        // Nutation in longitude (degrees)
        double deltaPsi = (-17.1996 * Math.sin(Math.toRadians(omega))) * ARC_SEC_TO_DEG;

        // Nutation in obliquity (degrees)
        double deltaEps = (9.2025 * Math.cos(Math.toRadians(omega))) * ARC_SEC_TO_DEG;

        return new double[]{deltaPsi, deltaEps};
    }

    // Calculate obliquity of the ecliptic (degrees)
    public static double calculateObliquity(double t) {
        double eps0 = 23.43929111 - 0.013004167 * t
                - 0.0000001639 * t * t + 0.0000005036 * t * t * t;

        // Add nutation
        double[] nutation = calculateNutation(t);
        return eps0 + nutation[1];
    }

    // Convert ecliptic to equatorial coordinates
    public static double[] eclipticToEquatorial(double lambda, double beta, double eps) {
        double lambdaRad = Math.toRadians(lambda);
        double betaRad = Math.toRadians(beta);
        double epsRad = Math.toRadians(eps);

        double ra = Math.atan2(
                Math.sin(lambdaRad) * Math.cos(epsRad) - Math.tan(betaRad) * Math.sin(epsRad),
                Math.cos(lambdaRad)
        );

        double dec = Math.asin(
                Math.sin(betaRad) * Math.cos(epsRad) +
                        Math.cos(betaRad) * Math.sin(epsRad) * Math.sin(lambdaRad)
        );

        return new double[]{
                Math.toDegrees(ra) % 360,
                Math.toDegrees(dec)
        };
    }
}