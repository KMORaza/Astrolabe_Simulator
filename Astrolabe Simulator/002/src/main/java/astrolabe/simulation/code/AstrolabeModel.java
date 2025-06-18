package astrolabe.simulation.code;

import java.time.LocalDateTime;
import javafx.animation.AnimationTimer;

public class AstrolabeModel {
    private double latitude = 51.5074; // Default: London
    private double longitude = -0.1278;
    private LocalDateTime dateTime = LocalDateTime.now();
    private boolean realTime = false;
    private AnimationTimer timer;

    public AstrolabeModel() {
        // Real-time update timer
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (realTime) {
                    dateTime = LocalDateTime.now();
                }
            }
        };
        timer.start();
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isRealTime() {
        return realTime;
    }

    public void setRealTime(boolean realTime) {
        this.realTime = realTime;
    }
}