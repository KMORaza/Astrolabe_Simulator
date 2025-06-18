package astrolabe.simulation.code;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class Mater extends Group {
    private double radius;

    public Mater(double radius) {
        this.radius = radius;
        this.getStyleClass().add("astrolabe");
        drawFront();
        drawBack();
    }

    private void drawFront() {
        // Outer circle
        Circle outerCircle = new Circle(radius);
        outerCircle.setFill(Color.rgb(40, 40, 40));
        outerCircle.setStroke(Color.rgb(100, 100, 100));
        outerCircle.setStrokeWidth(2);
        this.getChildren().add(outerCircle);

        // Inner circle
        Circle innerCircle = new Circle(radius * 0.95);
        innerCircle.setFill(Color.TRANSPARENT);
        innerCircle.setStroke(Color.rgb(120, 120, 120));
        innerCircle.setStrokeWidth(1);
        this.getChildren().add(innerCircle);

        // Degree markings
        for (int deg = 0; deg < 360; deg += 5) {
            double angleRad = Math.toRadians(deg);
            double x1 = radius * Math.cos(angleRad);
            double y1 = radius * Math.sin(angleRad);

            double tickLength = (deg % 90 == 0) ? 15 : (deg % 30 == 0) ? 10 : 5;
            double x2 = (radius - tickLength) * Math.cos(angleRad);
            double y2 = (radius - tickLength) * Math.sin(angleRad);

            Line tick = new Line(x1, y1, x2, y2);
            tick.setStroke(Color.rgb(150, 150, 150));
            this.getChildren().add(tick);

            if (deg % 30 == 0) {
                Text label = new Text(x2 - 10, y2 - 10, Integer.toString(deg));
                label.getStyleClass().add("astrolabe-label");
                this.getChildren().add(label);
            }
        }
    }

    private void drawBack() {
        Group backGroup = new Group();
        backGroup.getStyleClass().add("astrolabe-back");

        // Base circle
        Circle backCircle = new Circle(radius);
        backCircle.setFill(Color.rgb(30, 30, 30));
        backCircle.setStroke(Color.rgb(80, 80, 80));
        backCircle.setStrokeWidth(2);
        backGroup.getChildren().add(backCircle);

        // Altitude scale
        for (int deg = 0; deg <= 90; deg += 5) {
            double r = radius * 0.9 * (1 - deg/90.0);
            Circle circle = new Circle(r);
            circle.setFill(Color.TRANSPARENT);
            circle.setStroke(deg % 10 == 0 ? Color.rgb(150, 150, 150) : Color.rgb(100, 100, 100));
            circle.getStrokeDashArray().addAll(deg % 10 == 0 ? 2d : 1d, 3d);
            backGroup.getChildren().add(circle);

            if (deg % 10 == 0 && deg > 0) {
                Text label = new Text(r + 5, 5, deg + "°");
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);
            }
        }

        // Latitude scale
        double latRadiusInner = radius * 0.9;
        double latRadiusOuter = radius * 1.0;
        for (int deg = -90; deg <= 90; deg += 10) {
            double angleRad = Math.toRadians(deg);
            double x1 = latRadiusInner * Math.cos(angleRad);
            double y1 = latRadiusInner * Math.sin(angleRad);
            double x2 = latRadiusOuter * Math.cos(angleRad);
            double y2 = latRadiusOuter * Math.sin(angleRad);

            Line tick = new Line(x1, y1, x2, y2);
            tick.setStroke(deg % 30 == 0 ? Color.rgb(180, 180, 180) : Color.rgb(120, 120, 120));
            backGroup.getChildren().add(tick);

            if (deg % 30 == 0) {
                Text label = new Text(x2 + 5, y2 + 5, String.format("%+d°", deg));
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);
            }
        }

        // Shadow square
        double squareSize = radius * 0.4;
        double centerX = radius * 0.3;
        double centerY = radius * 0.3;

        Line top = new Line(centerX, centerY, centerX + squareSize, centerY);
        Line right = new Line(centerX + squareSize, centerY, centerX + squareSize, centerY + squareSize);
        Line bottom = new Line(centerX + squareSize, centerY + squareSize, centerX, centerY + squareSize);
        Line left = new Line(centerX, centerY + squareSize, centerX, centerY);

        Color squareColor = Color.rgb(180, 180, 180);
        top.setStroke(squareColor);
        right.setStroke(squareColor);
        bottom.setStroke(squareColor);
        left.setStroke(squareColor);

        backGroup.getChildren().addAll(top, right, bottom, left);

        // Shadow square graduations
        for (int i = 1; i <= 12; i++) {
            double pos = centerX + (squareSize * i / 12);
            Line tick = new Line(pos, centerY, pos, centerY - 5);
            tick.setStroke(squareColor);
            backGroup.getChildren().add(tick);

            Line tick2 = new Line(centerX - 5, centerY + (squareSize * i / 12),
                    centerX, centerY + (squareSize * i / 12));
            tick2.setStroke(squareColor);
            backGroup.getChildren().add(tick2);

            if (i % 3 == 0) {
                Text label = new Text(pos - 5, centerY - 10, Integer.toString(i));
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);

                Text label2 = new Text(centerX - 20, centerY + (squareSize * i / 12) + 5, Integer.toString(i));
                label2.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label2);
            }
        }

        // Shadow square labels
        Text versaLabel = new Text(centerX + squareSize/2 - 20, centerY - 20, "Umbra Versa");
        versaLabel.getStyleClass().add("astrolabe-label");

        Text rectaLabel = new Text(centerX - 50, centerY + squareSize/2 - 5, "Umbra Recta");
        rectaLabel.getStyleClass().add("astrolabe-label");
        rectaLabel.setRotate(-90);

        backGroup.getChildren().addAll(versaLabel, rectaLabel);

        // Declination scale
        double innerRadius = radius * 0.7;
        double outerRadius = radius * 0.85;

        for (int deg = -23; deg <= 23; deg++) {
            double angleRad = Math.toRadians(deg * 7.5);

            double x1 = innerRadius * Math.cos(angleRad);
            double y1 = innerRadius * Math.sin(angleRad);
            double x2 = outerRadius * Math.cos(angleRad);
            double y2 = outerRadius * Math.sin(angleRad);

            Line tick = new Line(x1, y1, x2, y2);
            tick.setStroke(deg % 5 == 0 ? Color.rgb(180, 180, 180) : Color.rgb(120, 120, 120));
            backGroup.getChildren().add(tick);

            if (deg % 5 == 0) {
                Text label = new Text(x2 + 5 * Math.cos(angleRad), y2 + 5 * Math.sin(angleRad),
                        String.format("%+d°", deg));
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);
            }
        }

        Text declLabel = new Text(0, outerRadius + 20, "Declination Scale");
        declLabel.getStyleClass().add("astrolabe-label");
        backGroup.getChildren().add(declLabel);

        // Hour markings
        double hourRadius = radius * 0.6;

        for (int hour = 0; hour < 24; hour++) {
            double angleRad = Math.toRadians(hour * 15);
            double x = hourRadius * Math.cos(angleRad);
            double y = hourRadius * Math.sin(angleRad);

            Line tick = new Line(x * 0.9, y * 0.9, x, y);
            tick.setStroke(hour % 3 == 0 ? Color.rgb(200, 150, 100) : Color.rgb(150, 100, 50));

            if (hour % 3 == 0) {
                Text label = new Text(x * 1.05, y * 1.05, Integer.toString(hour));
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);
            }

            backGroup.getChildren().add(tick);
        }

        Text hourLabel = new Text(0, hourRadius + 20, "Hour Scale");
        hourLabel.getStyleClass().add("astrolabe-label");
        backGroup.getChildren().add(hourLabel);

        // Trigonometric scales
        double scaleRadius = radius * 0.45;
        double scaleWidth = radius * 0.15;

        // Sine scale
        for (int deg = 0; deg <= 90; deg += 5) {
            double angleRad = Math.toRadians(deg);
            double value = Math.sin(angleRad);
            double x = scaleRadius + scaleWidth * value;
            double y = scaleRadius - deg * (scaleRadius * 0.02);

            Line tick = new Line(scaleRadius, y, x, y);
            tick.setStroke(deg % 10 == 0 ? Color.rgb(150, 200, 150) : Color.rgb(100, 150, 100));

            if (deg % 15 == 0) {
                Text label = new Text(x + 5, y, String.format("sin %d°", deg));
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);
            }

            backGroup.getChildren().add(tick);
        }

        // Cosine scale
        for (int deg = 0; deg <= 90; deg += 5) {
            double angleRad = Math.toRadians(deg);
            double value = Math.cos(angleRad);
            double x = scaleRadius - scaleWidth * value;
            double y = scaleRadius - deg * (scaleRadius * 0.02);

            Line tick = new Line(scaleRadius, y, x, y);
            tick.setStroke(deg % 10 == 0 ? Color.rgb(150, 150, 200) : Color.rgb(100, 100, 150));

            if (deg % 15 == 0) {
                Text label = new Text(x - 30, y, String.format("cos %d°", deg));
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);
            }

            backGroup.getChildren().add(tick);
        }

        Text trigLabel = new Text(scaleRadius, scaleRadius * 1.8, "Trigonometric Scales");
        trigLabel.getStyleClass().add("astrolabe-label");
        backGroup.getChildren().add(trigLabel);

        this.getChildren().add(backGroup);
        backGroup.setVisible(false);
    }

    public void showFront() {
        this.getChildren().get(0).setVisible(true);
        this.getChildren().get(1).setVisible(false);
    }

    public void showBack() {
        this.getChildren().get(0).setVisible(false);
        this.getChildren().get(1).setVisible(true);
    }
}
