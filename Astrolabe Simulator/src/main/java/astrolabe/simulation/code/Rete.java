package astrolabe.simulation.code;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
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
    private Timeline animationTimeline;
    private int animationSpeed = 1;
    private boolean isAnimating = false;

    // Angular measurement components
    private Circle firstSelection;
    private Circle secondSelection;
    private Line measurementLine;
    private Text measurementText;
    private boolean isFirstSelection = true;

    public Rete(double radius, AstrolabeModel model) {
        this.radius = radius;
        this.model = model;
        initializeStars();
        setupMeasurementTools();
        draw();
        setupAnimation();
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

    private void setupMeasurementTools() {
        firstSelection = new Circle(5, Color.TRANSPARENT);
        firstSelection.setStroke(Color.YELLOW);
        firstSelection.setStrokeWidth(2);
        firstSelection.setVisible(false);

        secondSelection = new Circle(5, Color.TRANSPARENT);
        secondSelection.setStroke(Color.ORANGE);
        secondSelection.setStrokeWidth(2);
        secondSelection.setVisible(false);

        measurementLine = new Line();
        measurementLine.setStroke(Color.CYAN);
        measurementLine.setStrokeWidth(1);
        measurementLine.getStrokeDashArray().addAll(5d, 5d);
        measurementLine.setVisible(false);

        measurementText = new Text();
        measurementText.setFill(Color.WHITE);
        measurementText.setStroke(Color.BLACK);
        measurementText.setStrokeWidth(0.5);
        measurementText.getStyleClass().add("measurement-label");
        measurementText.setVisible(false);

        this.getChildren().addAll(firstSelection, secondSelection, measurementLine, measurementText);

        this.setOnMouseClicked(event -> handleMeasurementClick(event.getX(), event.getY()));
    }

    private void handleMeasurementClick(double x, double y) {
        if (isFirstSelection) {
            firstSelection.setCenterX(x);
            firstSelection.setCenterY(y);
            firstSelection.setVisible(true);
            secondSelection.setVisible(false);
            measurementLine.setVisible(false);
            measurementText.setVisible(false);
            isFirstSelection = false;
        } else {
            secondSelection.setCenterX(x);
            secondSelection.setCenterY(y);
            secondSelection.setVisible(true);

            measurementLine.setStartX(firstSelection.getCenterX());
            measurementLine.setStartY(firstSelection.getCenterY());
            measurementLine.setEndX(secondSelection.getCenterX());
            measurementLine.setEndY(secondSelection.getCenterY());
            measurementLine.setVisible(true);

            double angle = calculateAngle(
                    firstSelection.getCenterX(), firstSelection.getCenterY(),
                    secondSelection.getCenterX(), secondSelection.getCenterY()
            );

            measurementText.setText(String.format("%.1f°", angle));
            measurementText.setX((firstSelection.getCenterX() + secondSelection.getCenterX()) / 2);
            measurementText.setY((firstSelection.getCenterY() + secondSelection.getCenterY()) / 2);
            measurementText.setVisible(true);

            isFirstSelection = true;
        }
    }

    private double calculateAngle(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double distance = Math.sqrt(dx*dx + dy*dy);
        double angleRadians = 2 * Math.asin(distance / (2 * radius));
        return Math.toDegrees(angleRadians);
    }

    public void clearMeasurements() {
        firstSelection.setVisible(false);
        secondSelection.setVisible(false);
        measurementLine.setVisible(false);
        measurementText.setVisible(false);
        isFirstSelection = true;
    }

    public void draw() {
        getChildren().clear();
        planets.clear();

        // Draw celestial grid (latitude/longitude lines)
        drawCelestialGrid();

        // Re-add measurement tools so they stay on top
        getChildren().addAll(firstSelection, secondSelection, measurementLine, measurementText);

        // Ecliptic circle
        Circle ecliptic = new Circle(radius * 0.8);
        ecliptic.setFill(Color.TRANSPARENT);
        ecliptic.setStroke(Color.rgb(200, 50, 50));
        ecliptic.setStrokeWidth(1);
        ecliptic.getStrokeDashArray().addAll(5d, 5d);
        this.getChildren().add(ecliptic);

        // Add stars with precession and refraction
        LocalDateTime now = model.getDateTime();
        double lst = AstroMath.calculateLocalSiderealTime(model.getLongitude(), now);
        double t = (AstroMath.toJulianDate(now) - 2451545.0) / 36525.0;

        for (Map.Entry<String, double[]> entry : stars.entrySet()) {
            String name = entry.getKey();
            double[] starData = entry.getValue();
            double ra = starData[0];
            double dec = starData[1];
            double mag = starData[2];

            double[] precessed = precessCoordinates(ra, dec, t);
            ra = precessed[0];
            dec = precessed[1];

            double ha = lst - ra * 15;
            double[] altAz = AstroMath.equatorialToHorizontal(ha, dec, model.getLatitude());

            if (altAz[0] > -1) {
                altAz[0] = applyAtmosphericRefraction(altAz[0]);
            }

            double[] xy = Projection.stereographicProjection(altAz[0], altAz[1], radius);

            if (!Double.isNaN(xy[0])) {
                addStar(name, xy[0], xy[1], mag);
            }
        }

        // Add planets
        addPlanets(now);

        // Add Moon phase
        addMoonPhase(now);

        // Zenith point
        Circle zenith = new Circle(3, Color.rgb(100, 100, 255));
        this.getChildren().add(zenith);
    }

    private void drawCelestialGrid() {
        // Latitude lines (declination, parallels) - 5° increments
        for (int dec = -85; dec <= 85; dec += 5) {
            Circle latCircle = new Circle();
            double[] xy = Projection.stereographicProjection(0, dec, radius);
            double r = Math.abs(xy[1]); // Radius based on y-coordinate
            latCircle.setCenterX(0);
            latCircle.setCenterY(0);
            latCircle.setRadius(r);
            latCircle.setFill(Color.TRANSPARENT);
            latCircle.setStroke(Color.rgb(244, 164, 96, 0.9));
            latCircle.setStrokeWidth(0.7);
            latCircle.getStrokeDashArray().addAll(1d, 2d);
            latCircle.getStyleClass().add("celestial-grid");
            this.getChildren().add(latCircle);

            if (dec % 30 == 0) {
                double[] labelPos = Projection.stereographicProjection(0, dec, radius);
                Text label = new Text(labelPos[0] + 5, labelPos[1], String.format("%+d°", dec));
                label.getStyleClass().add("astrolabe-label");
                this.getChildren().add(label);
            }
        }

        // Longitude lines (right ascension, meridians) - 1-hour (15°) increments
        for (int ra = 0; ra < 360; ra += 15) {
            Polyline lonLine = new Polyline();
            for (int dec = -90; dec <= 90; dec += 5) {
                double ha = ra / 15.0; // Convert RA to hour angle
                double[] altAz = AstroMath.equatorialToHorizontal(ha, dec, model.getLatitude());
                double[] xy = Projection.stereographicProjection(altAz[0], altAz[1], radius);
                if (!Double.isNaN(xy[0])) {
                    lonLine.getPoints().addAll(xy[0], xy[1]);
                }
            }
            lonLine.setStroke(Color.rgb(244, 164, 96, 0.9));
            lonLine.setStrokeWidth(0.7);
            lonLine.getStrokeDashArray().addAll(1d, 2d);
            lonLine.getStyleClass().add("celestial-grid");
            this.getChildren().add(lonLine);
        }

        // Hour markings around the edge (I to XII)
        for (int hour = 0; hour < 12; hour++) {
            double angleRad = Math.toRadians(hour * 30); // 360° / 12 = 30° per hour
            double x = radius * Math.cos(angleRad);
            double y = radius * Math.sin(angleRad);
            Text hourLabel = new Text(x - 10, y - 10, romanNumeral(hour + 1));
            hourLabel.getStyleClass().add("astrolabe-label");
            this.getChildren().add(hourLabel);
        }
    }

    private String romanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            case 11: return "XI";
            case 12: return "XII";
            default: return "";
        }
    }

    private void setupAnimation() {
        animationTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.1), event -> {
                    if (isAnimating) {
                        LocalDateTime newTime = model.getDateTime().plusMinutes(15 * animationSpeed);
                        model.setDateTime(newTime);
                        draw();
                    }
                })
        );
        animationTimeline.setCycleCount(Animation.INDEFINITE);
    }

    public void playAnimation() { isAnimating = true; animationTimeline.play(); }
    public void pauseAnimation() { isAnimating = false; animationTimeline.pause(); }
    public void stopAnimation() { isAnimating = false; animationTimeline.stop(); }
    public void setAnimationSpeed(int speedMultiplier) { this.animationSpeed = speedMultiplier; }
    public void fastForward(int hours) { model.setDateTime(model.getDateTime().plusHours(hours)); draw(); }
    public void rewind(int hours) { model.setDateTime(model.getDateTime().minusHours(hours)); draw(); }
    public void stepForward(int minutes) { model.setDateTime(model.getDateTime().plusMinutes(minutes)); draw(); }
    public void stepBackward(int minutes) { model.setDateTime(model.getDateTime().minusMinutes(minutes)); draw(); }
    public boolean isAnimating() { return isAnimating; }

    private double applyAtmosphericRefraction(double apparentAltitudeDeg) {
        double alt = apparentAltitudeDeg * 60;
        double R = 1.02 / Math.tan(Math.toRadians((alt + 10.3 / (alt + 5.11)) / 60));
        return apparentAltitudeDeg - (R / 60);
    }

    private double[] precessCoordinates(double raHours, double decDeg, double t) {
        double ra = Math.toRadians(raHours * 15);
        double dec = Math.toRadians(decDeg);

        double zeta = Math.toRadians((2306.2181 + 1.39656 * t - 0.000139 * t * t) * t / 3600);
        double z = Math.toRadians((2306.2181 + 1.39656 * t - 0.000139 * t * t) * t / 3600);
        double theta = Math.toRadians((2004.3109 - 0.85330 * t - 0.000217 * t * t) * t / 3600);

        double A = Math.cos(dec) * Math.sin(ra + zeta);
        double B = Math.cos(theta) * Math.cos(dec) * Math.cos(ra + zeta) - Math.sin(theta) * Math.sin(dec);
        double C = Math.sin(theta) * Math.cos(dec) * Math.cos(ra + zeta) + Math.cos(theta) * Math.sin(dec);

        double newRa = Math.atan2(A, B) + z;
        double newDec = Math.asin(C);

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

        if (altAz[0] > -1) {
            altAz[0] = applyAtmosphericRefraction(altAz[0]);
        }

        double[] xy = Projection.stereographicProjection(altAz[0], altAz[1], radius);

        if (!Double.isNaN(xy[0])) {
            symbol.setLayoutX(xy[0]);
            symbol.setLayoutY(xy[1]);

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
        return new Group(circle, line, arc);
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

        Line pointer = new Line(0, 0, x, y); // Simplified star pointer
        pointer.setStroke(Color.rgb(200, 200, 200));
        pointer.setStrokeWidth(0.5);

        Text label = new Text(x + size + 2, y, name);
        label.getStyleClass().add("star-label");

        this.getChildren().addAll(star, pointer, label);
    }

    public void updateMoonPhase() {
        double phase = calculateMoonPhase(model.getDateTime());
        updateMoonPhase(phase);
    }
}
