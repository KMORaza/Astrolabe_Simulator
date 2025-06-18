package astrolabe.simulation.code;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Rete extends Group {
    private double radius;
    private AstrolabeModel model;
    private Map<String, double[]> stars = new HashMap<>();

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

        // Zenith point
        Circle zenith = new Circle(3, Color.rgb(100, 100, 255));
        this.getChildren().add(zenith);
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
}