package astrolabe.simulation.code;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Rete extends Group {
    private double radius;
    private AstrolabeModel model;
    private Map<String, double[]> stars = new HashMap<>();
    private Map<String, Node> planets = new HashMap<>();
    private Circle moonDisk;
    private Arc moonPhaseArc;

    public Rete(double radius, AstrolabeModel model) {
        this.radius = radius;
        this.model = model;
        initializeStars();
        draw();
    }

    private void initializeStars() {
        // J2000 coordinates
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
        ecliptic.setStroke(Color.rgb(200, 50, 50));
        ecliptic.setStrokeWidth(1);
        ecliptic.getStrokeDashArray().addAll(5d, 5d);
        this.getChildren().add(ecliptic);

        // Add stars
        LocalDateTime now = model.getDateTime();
        double lst = AstroMath.calculateLocalSiderealTime(model.getLongitude(), now);
        double t = (AstroMath.toJulianDate(now) - 2451545.0) / 36525.0; // Centuries since J2000

        for (Map.Entry<String, double[]> entry : stars.entrySet()) {
            String name = entry.getKey();
            double[] starData = entry.getValue();
            double ra = starData[0];
            double dec = starData[1];
            double mag = starData[2];

            // Apply precession correction
            double[] precessed = precessCoordinates(ra, dec, t);
            ra = precessed[0];
            dec = precessed[1];

            double ha = lst - ra * 15;
            double[] altAz = AstroMath.equatorialToHorizontal(ha, dec, model.getLatitude());
            double[] xy = Projection.stereographicProjection(altAz[0], altAz[1], radius);

            if (!Double.isNaN(xy[0])) {
                addStar(name, xy[0], xy[1], mag);
            }
        }

        // Add planets
        addPlanets(now);

        // Add Moon phase visualization
        addMoonPhase(now);

        // Zenith point
        Circle zenith = new Circle(3, Color.rgb(100, 100, 255));
        this.getChildren().add(zenith);
    }

    private double[] precessCoordinates(double raHours, double decDeg, double t) {
        // Convert to radians
        double ra = Math.toRadians(raHours * 15);
        double dec = Math.toRadians(decDeg);

        // Precession parameters (zeta, z, theta in radians)
        double zeta = Math.toRadians((2306.2181 + 1.39656 * t - 0.000139 * t * t) * t / 3600);
        double z = Math.toRadians((2306.2181 + 1.39656 * t - 0.000139 * t * t) * t / 3600);
        double theta = Math.toRadians((2004.3109 - 0.85330 * t - 0.000217 * t * t) * t / 3600);

        // Apply precession transformation
        double A = Math.cos(dec) * Math.sin(ra + zeta);
        double B = Math.cos(theta) * Math.cos(dec) * Math.cos(ra + zeta) - Math.sin(theta) * Math.sin(dec);
        double C = Math.sin(theta) * Math.cos(dec) * Math.cos(ra + zeta) + Math.cos(theta) * Math.sin(dec);

        double newRa = Math.atan2(A, B) + z;
        double newDec = Math.asin(C);

        // Convert back to degrees
        newRa = (Math.toDegrees(newRa) / 15) % 24;
        if (newRa < 0) newRa += 24;
        newDec = Math.toDegrees(newDec);

        return new double[]{newRa, newDec};
    }

    private void addPlanets(LocalDateTime dateTime) {
        double[] mercury = AstroMath.calculatePlanetPosition("Mercury", dateTime);
        double[] venus = AstroMath.calculatePlanetPosition("Venus", dateTime);
        double[] mars = AstroMath.calculatePlanetPosition("Mars", dateTime);
        double[] jupiter = AstroMath.calculatePlanetPosition("Jupiter", dateTime);
        double[] saturn = AstroMath.calculatePlanetPosition("Saturn", dateTime);

        double lst = AstroMath.calculateLocalSiderealTime(model.getLongitude(), dateTime);
        double t = (AstroMath.toJulianDate(dateTime) - 2451545.0) / 36525.0;
        double eps = AstroMath.calculateObliquity(t);

        addPlanet("Mercury", mercury, lst, eps, radius * 0.06, Color.rgb(150, 150, 150), createMercurySymbol());
        addPlanet("Venus", venus, lst, eps, radius * 0.08, Color.rgb(255, 215, 0), createVenusSymbol());
        addPlanet("Mars", mars, lst, eps, radius * 0.07, Color.rgb(200, 50, 50), createMarsSymbol());
        addPlanet("Jupiter", jupiter, lst, eps, radius * 0.10, Color.rgb(200, 150, 100), createJupiterSymbol());
        addPlanet("Saturn", saturn, lst, eps, radius * 0.09, Color.rgb(200, 200, 100), createSaturnSymbol());
    }

    private void addPlanet(String name, double[] planetPos, double lst, double eps, double size, Color color, Node symbol) {
        if (planetPos == null || Double.isNaN(planetPos[0])) return;

        double[] equatorial = AstroMath.eclipticToEquatorial(planetPos[0], planetPos[1], eps);
        double ha = lst - equatorial[0];
        double[] altAz = AstroMath.equatorialToHorizontal(ha, equatorial[1], model.getLatitude());
        double[] xy = Projection.stereographicProjection(altAz[0], altAz[1], radius);

        if (!Double.isNaN(xy[0])) {
            symbol.setLayoutX(xy[0]);
            symbol.setLayoutY(xy[1]);

            // Apply color to all shapes in the group
            if (symbol instanceof Group) {
                for (Node node : ((Group)symbol).getChildren()) {
                    if (node instanceof Shape) {
                        ((Shape)node).setFill(color);
                        ((Shape)node).setStroke(Color.BLACK);
                        ((Shape)node).setStrokeWidth(0.5);
                    }
                }
            }

            Text label = new Text(xy[0] + size + 2, xy[1], name);
            label.getStyleClass().add("planet-label");

            this.getChildren().addAll(symbol, label);
            planets.put(name, symbol);
        }
    }

    private Node createMercurySymbol() {
        Circle circle = new Circle(0, 0, 5);
        Line line = new Line(0, -8, 0, 8);
        Arc arc = new Arc(0, 0, 5, 5, 0, 180);
        arc.setType(ArcType.OPEN);
        arc.setStrokeWidth(1);

        Group symbol = new Group(circle, line, arc);
        symbol.setScaleX(0.7);
        symbol.setScaleY(0.7);
        return symbol;
    }

    private Node createVenusSymbol() {
        Circle circle = new Circle(0, 0, 6);
        Line cross = new Line(-6, 0, 6, 0);
        return new Group(circle, cross);
    }

    private Node createMarsSymbol() {
        Circle circle = new Circle(0, 0, 6);
        Line arrow = new Line(0, -8, 0, 8);
        arrow.getStrokeDashArray().addAll(2d, 2d);
        return new Group(circle, arrow);
    }

    private Node createJupiterSymbol() {
        Ellipse ellipse = new Ellipse(0, 0, 8, 5);
        Line line = new Line(0, -8, 0, 8);
        return new Group(ellipse, line);
    }

    private Node createSaturnSymbol() {
        Ellipse ellipse = new Ellipse(0, 0, 8, 5);
        Line line = new Line(0, -8, 0, 8);
        Arc ring = new Arc(0, 0, 10, 3, 0, 180);
        ring.setType(ArcType.OPEN);
        ring.setStrokeWidth(1.5);
        return new Group(ellipse, line, ring);
    }

    private void addMoonPhase(LocalDateTime dateTime) {
        double[] moonPos = AstroMath.calculateMoonPosition(dateTime);
        double phase = calculateMoonPhase(dateTime);

        double moonX = radius * 0.6;
        double moonY = radius * 0.1;
        double moonSize = radius * 0.07;

        moonDisk = new Circle(moonSize);
        moonDisk.setCenterX(moonX);
        moonDisk.setCenterY(moonY);
        moonDisk.setFill(Color.rgb(200, 200, 200));
        moonDisk.setStroke(Color.rgb(150, 150, 150));
        this.getChildren().add(moonDisk);

        moonPhaseArc = new Arc();
        moonPhaseArc.setCenterX(moonX);
        moonPhaseArc.setCenterY(moonY);
        moonPhaseArc.setRadiusX(moonSize);
        moonPhaseArc.setRadiusY(moonSize);
        moonPhaseArc.setStartAngle(90);
        moonPhaseArc.setLength(180);
        moonPhaseArc.setType(ArcType.ROUND);
        moonPhaseArc.setFill(Color.rgb(40, 40, 40));

        updateMoonPhase(phase);
        this.getChildren().add(moonPhaseArc);

        Text phaseLabel = new Text(moonX - moonSize, moonY + moonSize + 15, getPhaseName(phase));
        phaseLabel.getStyleClass().add("astrolabe-label");
        this.getChildren().add(phaseLabel);
    }

    private void updateMoonPhase(double phase) {
        if (phase < 0.5) {
            moonPhaseArc.setStartAngle(270);
            moonPhaseArc.setLength(180 * (0.5 - phase) * 2);
        } else {
            moonPhaseArc.setStartAngle(90);
            moonPhaseArc.setLength(180 * (phase - 0.5) * 2);
        }
    }

    private double calculateMoonPhase(LocalDateTime dateTime) {
        double jd = AstroMath.toJulianDate(dateTime);
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