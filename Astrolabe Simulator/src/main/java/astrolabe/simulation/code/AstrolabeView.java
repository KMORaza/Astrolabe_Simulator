package astrolabe.simulation.code;

import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AstrolabeView {
    private final AstrolabeModel model;
    private final BorderPane view;
    private Rete rete;
    private Alidade frontAlidade;
    private Alidade backAlidade;
    private Group frontView;
    private Group backView;
    private double startAngle;

    public AstrolabeView(AstrolabeModel model) {
        this.model = model;
        this.view = new BorderPane();
        view.setStyle("-fx-background-color: #2b2b2b;");
        initializeUI();
    }

    private void initializeUI() {
        // Create front and back views
        frontView = createFrontView();
        backView = createBackView();

        // Default to front view
        view.setCenter(frontView);

        // Add control panel with view toggle, lat/lon sliders, and date selector
        VBox controls = new VBox(10);
        controls.getStyleClass().add("control-panel");

        Label title = new Label("Astrolabe Simulator");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ToggleButton viewToggle = new ToggleButton("Show Back");
        viewToggle.setOnAction(e -> {
            if (viewToggle.isSelected()) {
                view.setCenter(backView);
                viewToggle.setText("Show Front");
            } else {
                view.setCenter(frontView);
                viewToggle.setText("Show Back");
            }
        });

        // Latitude slider
        Label latLabel = new Label(String.format("Latitude: %.2f°", model.getLatitude()));
        Slider latSlider = new Slider(-90, 90, model.getLatitude());
        latSlider.setShowTickMarks(true);
        latSlider.setShowTickLabels(true);
        latSlider.setMajorTickUnit(30);
        latSlider.setMinorTickCount(5);
        latSlider.setSnapToTicks(true);
        latSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setLatitude(newVal.doubleValue());
            latLabel.setText(String.format("Latitude: %.2f°", model.getLatitude()));
            rete.draw(); // Update rete to reflect new latitude
        });

        // Longitude slider
        Label lonLabel = new Label(String.format("Longitude: %.2f°", model.getLongitude()));
        Slider lonSlider = new Slider(-180, 180, model.getLongitude());
        lonSlider.setShowTickMarks(true);
        lonSlider.setShowTickLabels(true);
        lonSlider.setMajorTickUnit(60);
        lonSlider.setMinorTickCount(5);
        lonSlider.setSnapToTicks(true);
        lonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setLongitude(newVal.doubleValue());
            lonLabel.setText(String.format("Longitude: %.2f°", model.getLongitude()));
            rete.draw(); // Update rete to reflect new longitude
        });

        // Date selector
        Label dateLabel = new Label("Date: " + model.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
        DatePicker datePicker = new DatePicker(model.getDateTime().toLocalDate());
        ToggleGroup eraToggle = new ToggleGroup();
        RadioButton ceRadio = new RadioButton("CE");
        RadioButton bcRadio = new RadioButton("BC");
        ceRadio.setToggleGroup(eraToggle);
        bcRadio.setToggleGroup(eraToggle);
        ceRadio.setSelected(true);
        ceRadio.setTextFill(Color.WHITE);
        bcRadio.setTextFill(Color.WHITE);

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            int year = newVal.getYear();
            if (bcRadio.isSelected()) {
                year = -year + 1; // Convert to negative year (e.g., 500 BC = -499)
            }
            LocalDateTime newDateTime = newVal.withYear(year).atStartOfDay();
            model.setDateTime(newDateTime);
            dateLabel.setText("Date: " + model.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
            rete.draw(); // Update rete to reflect new date
        });

        eraToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            LocalDate selectedDate = datePicker.getValue();
            int year = selectedDate.getYear();
            if (newVal == bcRadio) {
                year = -year + 1; // Convert to negative year
            } else {
                year = Math.abs(year); // Ensure positive for CE
            }
            LocalDateTime newDateTime = selectedDate.withYear(year).atStartOfDay();
            model.setDateTime(newDateTime);
            dateLabel.setText("Date: " + model.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
            rete.draw(); // Update rete to reflect new era
        });

        // Time label (static for now, can be enhanced later)
        Label timeLabel = new Label("Time: " + model.getDateTime().toLocalTime());

        controls.getChildren().addAll(
                title,
                latLabel,
                latSlider,
                lonLabel,
                lonSlider,
                dateLabel,
                datePicker,
                ceRadio,
                bcRadio,
                timeLabel,
                viewToggle
        );

        view.setRight(controls);
    }

    private Group createFrontView() {
        Group frontGroup = new Group();
        frontGroup.getStyleClass().add("astrolabe");

        Mater mater = new Mater(300);
        frontGroup.getChildren().add(mater);

        rete = new Rete(280, model);
        frontGroup.getChildren().add(rete);

        frontAlidade = new Alidade(290);
        frontGroup.getChildren().add(frontAlidade);

        // Make rete rotatable
        rete.setOnMousePressed(this::handleReteMousePressed);
        rete.setOnMouseDragged(this::handleReteMouseDragged);

        return frontGroup;
    }

    private Group createBackView() {
        Group backGroup = new Group();
        backGroup.getStyleClass().add("astrolabe");

        // Create back plate (simplified)
        Circle backPlate = new Circle(300);
        backPlate.setFill(Color.rgb(50, 50, 50));
        backPlate.setStroke(Color.rgb(120, 120, 120));
        backGroup.getChildren().add(backPlate);

        // Add altitude scale
        for (int deg = 0; deg <= 90; deg += 5) {
            double radius = 280 - deg * 2.5;
            Circle circle = new Circle(radius);
            circle.setFill(Color.TRANSPARENT);
            circle.setStroke(Color.rgb(100, 100, 100));
            circle.getStrokeDashArray().addAll(1d, 3d);
            backGroup.getChildren().add(circle);

            if (deg % 10 == 0) {
                Text label = new Text(radius + 5, 5, deg + "°");
                label.getStyleClass().add("astrolabe-label");
                backGroup.getChildren().add(label);
            }
        }

        // Add back alidade (for altitude measurement)
        backAlidade = new Alidade(290);
        backGroup.getChildren().add(backAlidade);

        // Add zenith point
        Circle zenith = new Circle(3, Color.rgb(100, 100, 255));
        backGroup.getChildren().add(zenith);

        return backGroup;
    }

    private void handleReteMousePressed(MouseEvent event) {
        startAngle = Math.toDegrees(Math.atan2(event.getY(), event.getX()));
    }

    private void handleReteMouseDragged(MouseEvent event) {
        double currentAngle = Math.toDegrees(Math.atan2(event.getY(), event.getX()));
        double rotationAngle = currentAngle - startAngle;
        rete.getTransforms().add(new Rotate(rotationAngle, 0, 0));
        startAngle = currentAngle;
    }

    public BorderPane getView() {
        return view;
    }
}
