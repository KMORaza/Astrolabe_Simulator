package astrolabe.simulation.code;

import java.time.LocalDateTime;

public class AstroMath {
    // Constants
    private static final double ARC_SEC_TO_DEG = 1.0 / 3600.0;
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double RAD_TO_DEG = 180.0 / Math.PI;
    private static final double PRECESSION_RA = 0.014;  // ~50.3 arcsec/year in seconds of time
    private static final double PRECESSION_DEC = 20.04; // arcseconds/year

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

    public static double calculateLocalSiderealTime(double longitude, LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0;

        double gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
                + 0.000387933 * t * t - t * t * t / 38710000.0;

        gmst = gmst % 360;
        if (gmst < 0) gmst += 360;

        return (gmst + longitude) % 360;
    }

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
                (Math.toDegrees(az) + 180) % 360
        };
    }

    public static double[] applyPrecessionAndNutation(double raHours, double decDeg, LocalDateTime dateTime) {
        double raDeg = raHours * 15;
        double dec = decDeg;

        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0;

        // Precession in right ascension (seconds of time per year)
        double deltaRA = PRECESSION_RA * t * 100;
        deltaRA = deltaRA * 15 / 3600; // Convert to degrees

        // Precession in declination (arcseconds per year)
        double deltaDec = PRECESSION_DEC * t * 100;
        deltaDec = deltaDec / 3600; // Convert to degrees

        // Apply precession
        double precessedRA = raDeg + deltaRA;
        double precessedDec = dec + deltaDec;

        // Get nutation
        double[] nutation = calculateNutation(t);
        double deltaPsi = nutation[0];
        double deltaEps = nutation[1];

        // Convert to ecliptic coordinates
        double eps = calculateObliquity(t) - deltaEps;
        double[] ecliptic = equatorialToEcliptic(precessedRA, precessedDec, eps);
        double lambda = ecliptic[0];
        double beta = ecliptic[1];

        // Apply nutation in longitude
        lambda += deltaPsi;

        // Convert back to equatorial
        eps = calculateObliquity(t);
        return eclipticToEquatorial(lambda, beta, eps);
    }

    public static double[] equatorialToEcliptic(double ra, double dec, double eps) {
        double raRad = Math.toRadians(ra);
        double decRad = Math.toRadians(dec);
        double epsRad = Math.toRadians(eps);

        double lambda = Math.atan2(
                Math.sin(raRad) * Math.cos(epsRad) + Math.tan(decRad) * Math.sin(epsRad),
                Math.cos(raRad)
        );

        double beta = Math.asin(
                Math.sin(decRad) * Math.cos(epsRad) -
                        Math.cos(decRad) * Math.sin(epsRad) * Math.sin(raRad)
        );

        return new double[]{
                Math.toDegrees(lambda) % 360,
                Math.toDegrees(beta)
        };
    }

    public static double[] calculateNutation(double t) {
        double D = Math.toRadians(297.85036 + 445267.111480*t);
        double M = Math.toRadians(357.52772 + 35999.050340*t);
        double M_prime = Math.toRadians(134.96298 + 477198.867398*t);
        double F = Math.toRadians(93.27191 + 483202.017538*t);
        double Omega = Math.toRadians(125.04452 - 1934.136261*t);

        // Long-period terms
        double deltaPsi = (-17.1996 * Math.sin(Omega)) * ARC_SEC_TO_DEG;
        double deltaEps = (9.2025 * Math.cos(Omega)) * ARC_SEC_TO_DEG;

        // Add principal short-period terms
        deltaPsi += (-1.3187 * Math.sin(2*F - 2*D + 2*Omega)) * ARC_SEC_TO_DEG;
        deltaEps += (0.5736 * Math.cos(2*F - 2*D + 2*Omega)) * ARC_SEC_TO_DEG;

        return new double[]{deltaPsi, deltaEps};
    }

    public static double calculateObliquity(double t) {
        double eps0 = 23.43929111 - 0.013004167 * t
                - 0.0000001639 * t * t + 0.0000005036 * t * t * t;
        double[] nutation = calculateNutation(t);
        return eps0 + nutation[1];
    }

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

    public static double[] calculateSunPosition(LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0;

        double L0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t;
        L0 = normalizeDegrees(L0);

        double M = 357.52911 + 35999.05029 * t - 0.0001537 * t * t;
        M = normalizeDegrees(M);
        double M_rad = Math.toRadians(M);

        double C = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(M_rad)
                + (0.019993 - 0.000101 * t) * Math.sin(2 * M_rad)
                + 0.000289 * Math.sin(3 * M_rad);

        double trueLong = L0 + C;
        double omega = 125.04 - 1934.136 * t;
        double lambda = trueLong - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));
        double beta = 0.0;
        double distance = 1.000001018 * (1 - 0.016708617 * Math.cos(M_rad)
                - 0.000139611 * Math.cos(2 * M_rad));
        double apparentDiameter = 0.5334 / distance;

        return new double[]{lambda, beta, apparentDiameter};
    }

    public static double[] calculateMoonPosition(LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0;

        double D = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868.0;
        D = normalizeDegrees(D);

        double M = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0;
        M = normalizeDegrees(M);

        double M_prime = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699.0;
        M_prime = normalizeDegrees(M_prime);

        double F = 93.2720950 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000.0;
        F = normalizeDegrees(F);

        double lambda = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t
                + t * t * t / 538841.0 - t * t * t * t / 65194000.0;

        double[] periodicTerms = calculateLunarPeriodicTerms(D, M, M_prime, F);
        lambda += periodicTerms[0];
        double beta = periodicTerms[1];
        double distance = 60.2685 - 3.1385 * Math.cos(Math.toRadians(M_prime));
        double apparentDiameter = 0.5181 * (60.2685 / distance);

        return new double[]{lambda, beta, apparentDiameter};
    }

    public static double calculateMoonPhase(LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0;

        double D = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868.0;
        double M = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0;
        double M_prime = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699.0;

        double phaseAngle = 180 - D - 6.289 * Math.sin(Math.toRadians(M_prime))
                + 2.100 * Math.sin(Math.toRadians(M))
                - 1.274 * Math.sin(Math.toRadians(2*D - M_prime))
                - 0.658 * Math.sin(Math.toRadians(2*D))
                - 0.214 * Math.sin(Math.toRadians(2*M_prime))
                - 0.110 * Math.sin(Math.toRadians(D));

        phaseAngle = (phaseAngle % 360 + 360) % 360;
        return phaseAngle / 360.0;
    }

    public static String getPhaseName(double phase) {
        if (phase < 0.03 || phase > 0.97) return "New Moon";
        if (phase < 0.22) return "Waxing Crescent";
        if (phase < 0.28) return "First Quarter";
        if (phase < 0.47) return "Waxing Gibbous";
        if (phase < 0.53) return "Full Moon";
        if (phase < 0.72) return "Waning Gibbous";
        if (phase < 0.78) return "Last Quarter";
        return "Waning Crescent";
    }

    public static double[] calculatePlanetPosition(String planet, LocalDateTime dateTime) {
        double jd = toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0;

        switch (planet) {
            case "Mercury":
                return calculateMercuryPosition(t);
            case "Venus":
                return calculateVenusPosition(t);
            case "Mars":
                return calculateMarsPosition(t);
            case "Jupiter":
                return calculateJupiterPosition(t);
            case "Saturn":
                return calculateSaturnPosition(t);
            default:
                return new double[]{Double.NaN, Double.NaN};
        }
    }

    private static double[] calculateMercuryPosition(double t) {
        double L = 252.250906 + 149474.0722491 * t;
        double a = 0.387098310;
        double e = 0.20563175;
        double i = 7.004986;
        double omega = 48.330893;
        double w = 77.456119;
        return calculatePlanetPosition(L, a, e, i, omega, w, t);
    }

    private static double[] calculateVenusPosition(double t) {
        double L = 181.979801 + 58519.2130302 * t;
        double a = 0.723329820;
        double e = 0.00677188;
        double i = 3.394662;
        double omega = 76.679920;
        double w = 131.563707;
        return calculatePlanetPosition(L, a, e, i, omega, w, t);
    }

    private static double[] calculateMarsPosition(double t) {
        double L = 355.433275 + 19141.6964746 * t;
        double a = 1.523679342;
        double e = 0.09340062;
        double i = 1.849726;
        double omega = 49.558093;
        double w = 336.060234;
        return calculatePlanetPosition(L, a, e, i, omega, w, t);
    }

    private static double[] calculateJupiterPosition(double t) {
        double L = 34.351519 + 3036.3027748 * t;
        double a = 5.202603191;
        double e = 0.04849485;
        double i = 1.303270;
        double omega = 100.464441;
        double w = 14.331309;
        return calculatePlanetPosition(L, a, e, i, omega, w, t);
    }

    private static double[] calculateSaturnPosition(double t) {
        double L = 50.077444 + 1223.5110686 * t;
        double a = 9.554909596;
        double e = 0.05550862;
        double i = 2.488878;
        double omega = 113.665524;
        double w = 93.056787;
        return calculatePlanetPosition(L, a, e, i, omega, w, t);
    }

    private static double[] calculatePlanetPosition(double L, double a, double e,
                                                    double i, double omega, double w, double t) {
        L = normalizeDegrees(L);
        double M = L - w;
        M = normalizeDegrees(M);

        double E = M + e * Math.sin(Math.toRadians(M)) * (1 + e * Math.cos(Math.toRadians(M)));

        double v = 2 * Math.atan2(
                Math.sqrt(1 + e) * Math.sin(Math.toRadians(E)/2),
                Math.sqrt(1 - e) * Math.cos(Math.toRadians(E)/2)
        );
        v = Math.toDegrees(v);

        double r = a * (1 - e * Math.cos(Math.toRadians(E)));
        double lon = v + w;
        lon = normalizeDegrees(lon);

        double lat = i * Math.sin(Math.toRadians(lon - omega));

        return new double[]{lon, lat};
    }

    private static double[] calculateLunarPeriodicTerms(double D, double M, double M_prime, double F) {
        double longitude = 6.2886 * Math.sin(Math.toRadians(M_prime))
                + 1.2740 * Math.sin(Math.toRadians(2 * D - M_prime))
                + 0.6583 * Math.sin(Math.toRadians(2 * D))
                + 0.2136 * Math.sin(Math.toRadians(2 * M_prime));

        double latitude = 5.1282 * Math.sin(Math.toRadians(F))
                + 0.2806 * Math.sin(Math.toRadians(M_prime + F))
                + 0.2777 * Math.sin(Math.toRadians(M_prime - F));

        return new double[]{longitude, latitude};
    }

    private static double normalizeDegrees(double degrees) {
        degrees = degrees % 360;
        return degrees < 0 ? degrees + 360 : degrees;
    }
}