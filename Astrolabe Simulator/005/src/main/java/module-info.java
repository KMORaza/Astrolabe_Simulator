module astrolabe.simulation.code.astrolabesimulator {
    requires javafx.controls;
    requires javafx.fxml;


    opens astrolabe.simulation.code to javafx.fxml;
    exports astrolabe.simulation.code;
}