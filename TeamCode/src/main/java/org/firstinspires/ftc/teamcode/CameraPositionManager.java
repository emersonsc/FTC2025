package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.hardware.limelightvision.Limelight3A;

/**
 * Manages the camera tilt servo to point the Limelight in different directions
 * for different tasks: AprilTag detection, obelisk reading, and artifact color detection
 */
public class CameraPositionManager {

    private Servo cameraTiltServo;
    private Limelight3A limelight;

    // Camera position states
    public enum CameraPosition {
        APRILTAG_VIEW,      // Looking up/forward for AprilTags and navigation
        OBELISK_VIEW,       // Looking at obelisk (motif detection)
        ARTIFACT_INTAKE     // Looking down at intake for color detection
    }

    // Servo positions - TUNE THESE to match your physical setup
    private static final double APRILTAG_POSITION = 0.2;   // Looking up/forward for AprilTags
    private static final double OBELISK_POSITION = 0.3;    // Angled to see obelisk
    private static final double INTAKE_POSITION = 0.9;     // Looking down at intake path

    // Pipeline indices for different detection tasks
    private static final int APRILTAG_PIPELINE = 0;        // AprilTag detection pipeline
    private static final int COLOR_GREEN_PIPELINE = 2;     // Green artifact detection
    private static final int COLOR_PURPLE_PIPELINE = 3;    // Purple artifact detection

    // Movement timing
    private static final long SERVO_SETTLE_TIME = 300;     // ms to wait after moving camera

    private CameraPosition currentPosition = CameraPosition.APRILTAG_VIEW;
    private long lastMoveTime = 0;
    private boolean isSettled = true;

    public CameraPositionManager(Servo cameraTiltServo, Limelight3A limelight) {
        this.cameraTiltServo = cameraTiltServo;
        this.limelight = limelight;

        // Initialize to AprilTag view
        moveTo(CameraPosition.APRILTAG_VIEW);
    }

    /**
     * Move camera to a specific position
     * @param position The target camera position
     */
    public void moveTo(CameraPosition position) {
        if (position == currentPosition && isSettled) {
            return; // Already there
        }

        currentPosition = position;
        isSettled = false;
        lastMoveTime = System.currentTimeMillis();

        switch (position) {
            case APRILTAG_VIEW:
                cameraTiltServo.setPosition(APRILTAG_POSITION);
                limelight.pipelineSwitch(APRILTAG_PIPELINE);
                break;

            case OBELISK_VIEW:
                cameraTiltServo.setPosition(OBELISK_POSITION);
                limelight.pipelineSwitch(APRILTAG_PIPELINE); // Obelisk uses AprilTag pipeline
                break;

            case ARTIFACT_INTAKE:
                cameraTiltServo.setPosition(INTAKE_POSITION);
                // Pipeline will be switched by color detector as needed
                break;
        }
    }

    /**
     * Update the settle state - call this in your loop
     */
    public void update() {
        if (!isSettled && (System.currentTimeMillis() - lastMoveTime) > SERVO_SETTLE_TIME) {
            isSettled = true;
        }
    }

    /**
     * Check if camera has settled into position
     * @return true if camera is ready for image processing
     */
    public boolean isSettled() {
        return isSettled;
    }

    /**
     * Get current camera position
     */
    public CameraPosition getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Check if camera is ready for AprilTag detection
     */
    public boolean isReadyForAprilTags() {
        return currentPosition == CameraPosition.APRILTAG_VIEW && isSettled;
    }

    /**
     * Check if camera is ready for obelisk/motif detection
     */
    public boolean isReadyForObelisk() {
        return currentPosition == CameraPosition.OBELISK_VIEW && isSettled;
    }

    /**
     * Check if camera is ready for artifact color detection
     */
    public boolean isReadyForColorDetection() {
        return currentPosition == CameraPosition.ARTIFACT_INTAKE && isSettled;
    }

    /**
     * Get status string for telemetry
     */
    public String getStatusString() {
        String settled = isSettled ? "Ready" : "Moving...";
        return String.format("Camera: %s (%s)", currentPosition, settled);
    }
}