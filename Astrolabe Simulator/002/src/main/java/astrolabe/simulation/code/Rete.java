package astrolabe.simulation.code;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Rete extends Group {
    private double radius;
    private AstrolabeModel model;
    private Map<String, double[]> stars = new HashMap<>();
    private Circle moonDisk;
    private Arc moonPhaseArc;

    public Rete(double radius, AstrolabeModel model) {
        this.radius = radius;
        this.model = model;
        initializeStars();
        draw();
    }

    private void initializeStars() {
        stars.put("Polaris", new double[]{2.530193, 89.264109, 2.0});
        stars.put("Vega", new double[]{18.61565, 38.78369, 0.0});
        stars.put("Sirius", new double[]{6.752481, -16.716116, -1.46});
        stars.put("Betelgeuse", new double[]{5.91953, 7.407063, 0.42});
        stars.put("Rigel", new double[]{5.242297, -8.20164, 0.13});
        stars.put("Procyon", new double[]{7.655026, 5.224987, 0.34});
        stars.put("Capella", new double[]{5.27816, 45.99799, 0.08});
    }

    private void draw() {
        // Ecliptic circle
        Circle ecliptic = new Circle(radius * 0.8);
        ecliptic.setFill(Color.TRANSPARENT);
        ecliptic.setStroke(Color.rgb(200, 50, 50)); // Dark red
        ecliptic.setStrokeWidth(1);
        ecliptic.getStrokeDashArray().addAll(5d, 5d);
        this.getChildren().add(ecliptic);

        // Add stars
        LocalDateTime now = model.getDateTime();
        double lst = AstroMath.calculateLocalSiderealTime(model.getLongitude(), now);

        for (Map.Entry<String, double[]> entry : stars.entrySet()) {
            String name = entry.getKey();
            double[] starData = entry.getValue();
            double ra = starData[0];
            double dec = starData[1];
            double mag = starData[2];

            double ha = lst - ra * 15;
            double[] altAz = AstroMath.equatorialToHorizontal(ha, dec, model.getLatitude());
            double[] xy = Projection.stereographicProjection(altAz[0], altAz[1], radius);

            if (!Double.isNaN(xy[0])) {
                addStar(name, xy[0], xy[1], mag);
            }
        }

        // Add Moon phase visualization
        addMoonPhase(now);

        // Zenith point
        Circle zenith = new Circle(3, Color.rgb(100, 100, 255));
        this.getChildren().add(zenith);
    }

    private void addMoonPhase(LocalDateTime dateTime) {
        // Calculate Moon position and phase
        double[] moonPos = AstroMath.calculateMoonPosition(dateTime);
        double phase = calculateMoonPhase(dateTime);

        // Position Moon on the rete (simplified - could use actual coordinates)
        double moonX = radius * 0.6;
        double moonY = radius * 0.1;
        double moonSize = radius * 0.07;

        // Moon disk
        moonDisk = new Circle(moonSize);
        moonDisk.setCenterX(moonX);
        moonDisk.setCenterY(moonY);
        moonDisk.setFill(Color.rgb(200, 200, 200));
        moonDisk.setStroke(Color.rgb(150, 150, 150));
        this.getChildren().add(moonDisk);

        // Moon phase arc
        moonPhaseArc = new Arc();
        moonPhaseArc.setCenterX(moonX);
        moonPhaseArc.setCenterY(moonY);
        moonPhaseArc.setRadiusX(moonSize);
        moonPhaseArc.setRadiusY(moonSize);
        moonPhaseArc.setStartAngle(90);
        moonPhaseArc.setLength(180);
        moonPhaseArc.setType(ArcType.ROUND);
        moonPhaseArc.setFill(Color.rgb(40, 40, 40)); // Dark color for shadow

        // Position the arc based on phase
        updateMoonPhase(phase);
        this.getChildren().add(moonPhaseArc);

        // Moon phase label
        Text phaseLabel = new Text(moonX - moonSize, moonY + moonSize + 15, getPhaseName(phase));
        phaseLabel.getStyleClass().add("astrolabe-label");
        this.getChildren().add(phaseLabel);
    }

    private void updateMoonPhase(double phase) {
        // Phase ranges from 0 (New Moon) to 1 (Full Moon) and back to 0
        if (phase < 0.5) {
            // Waxing (shadow on the left)
            moonPhaseArc.setStartAngle(270);
            moonPhaseArc.setLength(180 * (0.5 - phase) * 2);
        } else {
            // Waning (shadow on the right)
            moonPhaseArc.setStartAngle(90);
            moonPhaseArc.setLength(180 * (phase - 0.5) * 2);
        }
    }

    private double calculateMoonPhase(LocalDateTime dateTime) {
        // Simplified Moon phase calculation (0 = New Moon, 0.5 = Full Moon, 1 = New Moon)
        double jd = AstroMath.toJulianDate(dateTime);
        double t = (jd - 2451545.0) / 36525.0; // Julian centuries since J2000.0

        // Mean elongation of the Moon
        double D = 297.8501921 + 445267.1114034 * t
                - 0.0018819 * t * t + t * t * t / 545868.0;

        // Sun's mean anomaly
        double M = 357.5291092 + 35999.0502909 * t
                - 0.0001536 * t * t + t * t * t / 24490000.0;

        // Moon's mean anomaly
        double M_prime = 134.9633964 + 477198.8675055 * t
                + 0.0087414 * t * t + t * t * t / 69699.0;

        // Phase angle (0-360 degrees)
        double phaseAngle = 180 - D
                - 6.289 * Math.sin(Math.toRadians(M_prime))
                + 2.100 * Math.sin(Math.toRadians(M))
                - 1.274 * Math.sin(Math.toRadians(2*D - M_prime))
                - 0.658 * Math.sin(Math.toRadians(2*D))
                - 0.214 * Math.sin(Math.toRadians(2*M_prime))
                - 0.110 * Math.sin(Math.toRadians(D));

        // Normalize and convert to phase fraction (0-1)
        phaseAngle = (phaseAngle % 360 + 360) % 360;
        return phaseAngle / 360.0;
    }

    private String getPhaseName(double phase) {
        if (phase < 0.03 || phase > 0.97) return "New Moon";
        if (phase < 0.22) return "Waxing Crescent";
        if (phase < 0.28) return "First Quarter";
        if (phase < 0.47) return "Waxing Gibbous";
        if (phase < 0.53) return "Full Moon";
        if (phase < 0.72) return "Waning Gibbous";
        if (phase < 0.78) return "Last Quarter";
        return "Waning Crescent";
    }

    private void addStar(String name, double x, double y, double magnitude) {
        double size = 8 - magnitude * 2;
        size = Math.max(2, Math.min(size, 10));

        Circle star = new Circle(size);
        star.setCenterX(x);
        star.setCenterY(y);
        star.setFill(Color.TRANSPARENT);
        star.setStroke(Color.rgb(220, 220, 220));
        star.setStrokeWidth(1);

        Text label = new Text(x + size + 2, y, name);
        label.getStyleClass().add("star-label");

        this.getChildren().addAll(star, label);
    }

    public void updateMoonPhase() {
        double phase = calculateMoonPhase(model.getDateTime());
        updateMoonPhase(phase);
    }
}