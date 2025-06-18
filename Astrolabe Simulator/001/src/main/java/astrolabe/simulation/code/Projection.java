package astrolabe.simulation.code;

public class Projection {
    // Stereographic projection for astrolabe
    public static double[] stereographicProjection(double alt, double az, double radius) {
        // Convert altitude to zenith distance
        double z = 90 - alt;

        // Don't project stars below horizon
        if (z > 90) {
            return new double[]{Double.NaN, Double.NaN};
        }

        double zRad = Math.toRadians(z);
        double azRad = Math.toRadians(az);

        // Polar coordinates
        double r = radius * Math.tan(zRad / 2);
        double x = r * Math.sin(azRad);
        double y = -r * Math.cos(azRad);

        return new double[]{x, y};
    }

    // Inverse stereographic projection
    public static double[] inverseStereographicProjection(double x, double y, double radius) {
        double r = Math.sqrt(x*x + y*y);
        double z = 2 * Math.atan(r / radius);
        double az = Math.atan2(x, -y);

        return new double[]{
                90 - Math.toDegrees(z),  // altitude
                (Math.toDegrees(az) + 360) % 360  // azimuth
        };
    }
}