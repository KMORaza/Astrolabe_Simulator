package astrolabe.simulation.code;

import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class Alidade extends Group {
    private double length;
    private double rotation = 0;
    private Text angleDisplay;

    public Alidade(double length) {
        this.length = length;
        draw();
        setupInteraction();
    }

    private void draw() {
        // Main rule
        Line rule = new Line(-length/2, 0, length/2, 0);
        rule.setStroke(Color.rgb(200, 200, 200));
        rule.setStrokeWidth(2);

        // Sighting vanes
        Rectangle leftVane = new Rectangle(5, 20, Color.rgb(80, 80, 80));
        leftVane.setX(-length/2 - 5);
        leftVane.setY(-10);

        Rectangle rightVane = new Rectangle(5, 20, Color.rgb(80, 80, 80));
        rightVane.setX(length/2);
        rightVane.setY(-10);

        // Angle display
        angleDisplay = new Text(20, 30, "0°");
        angleDisplay.getStyleClass().add("astrolabe-label");
        angleDisplay.setFill(Color.rgb(220, 220, 220));

        this.getChildren().addAll(rule, leftVane, rightVane, angleDisplay);
    }

    private void setupInteraction() {
        final double[] startAngle = new double[1];

        this.setOnMousePressed(event -> {
            startAngle[0] = Math.toDegrees(Math.atan2(event.getY(), event.getX())) - rotation;
            event.consume();
        });

        this.setOnMouseDragged(event -> {
            double currentAngle = Math.toDegrees(Math.atan2(event.getY(), event.getX()));
            rotation = currentAngle - startAngle[0];
            this.getTransforms().clear();
            this.getTransforms().add(new javafx.scene.transform.Rotate(rotation, 0, 0));
            updateAngleDisplay();
            event.consume();
        });
    }

    private void updateAngleDisplay() {
        double normalizedAngle = (360 - rotation) % 360;
        angleDisplay.setText(String.format("%.1f°", normalizedAngle));
        angleDisplay.setX(20 * Math.cos(Math.toRadians(rotation + 90)));
        angleDisplay.setY(20 * Math.sin(Math.toRadians(rotation + 90)));
    }

    public double getCurrentAngle() {
        return (360 - rotation) % 360;
    }
}
