package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

import java.util.List;

/**
 *
 * GAMEPAD 1 (Driver):
 * - Left stick: Forward/Strafe
 * - Right stick: Rotate
 * - Left bumper: Switch to robot-relative drive
 * - A: Reset IMU heading
 * - X: Auto-drive to scoring position
 *
 * GAMEPAD 2 (Operator):
 * - Left bumper: INTAKE (hold to auto-load artifacts)
 * - Right bumper: SHOOT (hold to auto-shoot sequence)
 * - Y: Clear all indexer slots
 * - A: Reset indexer to home
 */
@TeleOp(name = "Robot: Complete Mecanum Drive", group = "Robot")
public class CompleteMecanumDrive extends OpMode {
    private RobotHardware robot;
    private IndexerManager indexer;
    private LimelightColorDetector colorDetector;
    private CameraPositionManager cameraManager;

    // Auto-drive to scoring position state
    private boolean targeting = false;
    private Pose2D targetScoringPose = null;

    // Flywheel control
    private boolean flywheelActive = false;
    private long flywheelStartTime = 0;
    private static final long FLYWHEEL_SPINUP_TIME = 1000; // ms

    @Override
    public void init() {
        robot = new RobotHardware(hardwareMap);

        // Initialize camera position manager
        cameraManager = new CameraPositionManager(
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo.class, "cameraTilt"),
                robot.getLimelight()
        );

        // Initialize indexer
        indexer = new IndexerManager(
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo.class, "indexer"),
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo.class, "lifter")
        );

        // Initialize color detector WITH camera manager
        colorDetector = new LimelightColorDetector(robot.getLimelight(), cameraManager);

        // Load pose from auto if available
        if (RobotData.autoCompleted && RobotData.finalAutoPose != null) {
            robot.getPinpoint().setPosition(RobotData.finalAutoPose);
            telemetry.addData("Pose Loaded from Auto", "X: %.2f, Y: %.2f, Heading: %.2f",
                    RobotData.finalAutoPose.getX(DistanceUnit.INCH),
                    RobotData.finalAutoPose.getY(DistanceUnit.INCH),
                    RobotData.finalAutoPose.getHeading(AngleUnit.DEGREES));
        } else {
            robot.getPinpoint().setPosition(new Pose2D(DistanceUnit.INCH, 0, 0, AngleUnit.DEGREES, 0));
            telemetry.addData("Pose", "Initialized to (0, 0, 0)");
        }

        telemetry.addData(">", "Robot Ready. Press Play.");
        telemetry.update();
    }

    @Override
    public void loop() {
        Pose2D odoPose = robot.updatePoseWithFusion();

        // Update camera position manager
        cameraManager.update();

        // ========== AUTOMATIC CAMERA MANAGEMENT ==========
        // Point camera based on what we're doing
        if (gamepad2.left_bumper) {
            // Intaking - point camera down at intake
            cameraManager.moveTo(CameraPositionManager.CameraPosition.ARTIFACT_INTAKE);
        } else if (targeting || gamepad2.right_bumper) {
            // Shooting or auto-driving - point camera up for AprilTags
            cameraManager.moveTo(CameraPositionManager.CameraPosition.APRILTAG_VIEW);
        } else {
            // Default - keep camera up for AprilTag detection and navigation
            if (cameraManager.getCurrentPosition() != CameraPositionManager.CameraPosition.APRILTAG_VIEW) {
                cameraManager.moveTo(CameraPositionManager.CameraPosition.APRILTAG_VIEW);
            }
        }

        // ========== ALLIANCE AND PATTERN DETECTION ==========
        // Only detect when camera is pointing at AprilTags
        if (cameraManager.isReadyForAprilTags()) {
            LLResult result = robot.getLimelight().getLatestResult();
            if (result != null && result.isValid()) {
                List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();
                for (LLResultTypes.FiducialResult fr : fiducialResults) {
                    int id = fr.getFiducialId();

                    // Alliance detection
                    if (RobotData.alliance.equals("NotSet")) {
                        if (id == RobotHardware.RED_APRILTAG_ID) {
                            RobotData.alliance = "Red";
                            robot.setTeamIndicator("Red");
                        } else if (id == RobotHardware.BLUE_APRILTAG_ID) {
                            RobotData.alliance = "Blue";
                            robot.setTeamIndicator("Blue");
                        }
                    }

                    // Pattern detection
                    if (RobotData.pattern.equals("NotSet")) {
                        if (id == RobotHardware.GPP_APRILTAG_ID) {
                            RobotData.pattern = "GPP";
                            robot.setPattern("GPP");
                        } else if (id == RobotHardware.PGP_APRILTAG_ID) {
                            RobotData.pattern = "PGP";
                            robot.setPattern("PGP");
                        } else if (id == RobotHardware.PPG_APRILTAG_ID) {
                            RobotData.pattern = "PPG";
                            robot.setPattern("PPG");
                        }
                    }
                }
            }
        }

        // ========== GAMEPAD 1: DRIVING CONTROLS ==========
        double forward = -gamepad1.left_stick_y;
        double right = gamepad1.left_stick_x;
        double rotate = gamepad1.right_stick_x;

        // A button: Reset IMU heading
        if (gamepad1.a) {
            robot.getImu().resetYaw();
            double odoX = odoPose.getX(DistanceUnit.INCH);
            double odoY = odoPose.getY(DistanceUnit.INCH);
            robot.getPinpoint().setPosition(new Pose2D(DistanceUnit.INCH, odoX, odoY, AngleUnit.DEGREES, 0));
        }

        // X button: Auto-drive to scoring location
        if (gamepad1.x) {
            if (RobotData.alliance.equals("NotSet")) {
                telemetry.addData("Error", "Alliance not set - cannot move to scoring location");
            } else {
                targetScoringPose = RobotData.alliance.equals("Red") ?
                        RobotData.RED_SCORING_POSE :
                        RobotData.BLUE_SCORING_POSE;
                targeting = true;
            }
        }

        // Handle auto-targeting or manual drive
        if (targeting) {
            double dx = targetScoringPose.getX(DistanceUnit.INCH) - odoPose.getX(DistanceUnit.INCH);
            double dy = targetScoringPose.getY(DistanceUnit.INCH) - odoPose.getY(DistanceUnit.INCH);
            double dheading = AngleUnit.normalizeDegrees(targetScoringPose.getHeading(AngleUnit.DEGREES) - odoPose.getHeading(AngleUnit.DEGREES));

            double positionError = Math.hypot(dx, dy);
            if (positionError < 2.0 && Math.abs(dheading) < 5.0) {
                targeting = false;
                robot.drive(0, 0, 0);
                telemetry.addData("Scoring Location", "Reached");
            } else {
                double headingRad = odoPose.getHeading(AngleUnit.RADIANS);
                double localForward = -dx * Math.sin(headingRad) + dy * Math.cos(headingRad);
                double localRight = dx * Math.cos(headingRad) + dy * Math.sin(headingRad);

                double k_p = 0.01; // tune position gain
                double k_h = 0.01; // tune heading gain

                forward = k_p * localForward;
                right = k_p * localRight;
                rotate = k_h * dheading;

                forward = Math.max(-1, Math.min(1, forward));
                right = Math.max(-1, Math.min(1, right));
                rotate = Math.max(-1, Math.min(1, rotate));

                robot.drive(forward, right, rotate);
            }
        } else {
            // Manual driving
            if (gamepad1.left_bumper) {
                // Robot-relative drive
                robot.drive(forward, right, rotate);
            } else {
                // Field-relative drive
                driveFieldRelative(forward, right, rotate, odoPose);
            }
        }

        // ========== GAMEPAD 2: INDEXER CONTROLS ==========

        // LEFT BUMPER: INTAKE MODE
        if (gamepad2.left_bumper) {
            // Start intake if not already running
            if (indexer.getLoadingState() == IndexerManager.LoadingState.IDLE) {
                indexer.startLoading();
            }

            // Detect artifact color from Limelight (only when camera ready)
            IndexerManager.ArtifactColor detectedColor = colorDetector.detectColorRobust();

            // Update the loading state machine
            indexer.updateLoading(detectedColor);

        } else {
            // Stop intake when button released
            if (indexer.getLoadingState() != IndexerManager.LoadingState.IDLE &&
                    indexer.getLoadingState() != IndexerManager.LoadingState.COMPLETE) {
                indexer.stopLoading();
            }
        }

        // RIGHT BUMPER: SHOOTING MODE
        if (gamepad2.right_bumper) {
            // Start flywheel if not already active
            if (!flywheelActive) {
                flywheelActive = true;
                flywheelStartTime = System.currentTimeMillis();
                // TODO: Activate your flywheel motor here
                // robot.startFlywheel();
            }

            // Start shooting sequence if not already shooting
            if (indexer.getShootingState() == IndexerManager.ShootingState.IDLE ||
                    indexer.getShootingState() == IndexerManager.ShootingState.COMPLETE) {

                indexer.startShooting(RobotData.pattern);
            }

            // Check if flywheel is ready (up to speed)
            boolean flywheelReady = (System.currentTimeMillis() - flywheelStartTime) > FLYWHEEL_SPINUP_TIME;

            // Update the shooting state machine
            indexer.updateShooting(flywheelReady);

        } else {
            // Stop flywheel when button released
            if (flywheelActive) {
                flywheelActive = false;
                // TODO: Stop your flywheel motor here
                // robot.stopFlywheel();
            }

            // Reset shooting state if button released mid-sequence
            if (indexer.getShootingState() != IndexerManager.ShootingState.IDLE &&
                    indexer.getShootingState() != IndexerManager.ShootingState.COMPLETE) {
                indexer.stopShooting();
            }
        }

        // Y button: Clear all indexer slots (emergency reset)
        if (gamepad2.y) {
            indexer.clearAllSlots();
        }

        // A button: Reset indexer to home position
        if (gamepad2.a) {
            indexer.resetToHome();
        }

        // ========== TELEMETRY ==========
        telemetry.addData("Alliance", RobotData.alliance);
        telemetry.addData("Pattern", RobotData.pattern);
        telemetry.addData("Camera", cameraManager.getStatusString());
        telemetry.addLine();

        telemetry.addData("Position", "X: %.1f, Y: %.1f",
                odoPose.getX(DistanceUnit.INCH),
                odoPose.getY(DistanceUnit.INCH));
        telemetry.addData("Heading", "%.1f°", odoPose.getHeading(AngleUnit.DEGREES));
        telemetry.addLine();

        telemetry.addData("Indexer", indexer.getStatusString());

        // Loading status
        if (indexer.getLoadingState() != IndexerManager.LoadingState.IDLE) {
            telemetry.addData("Loading", indexer.getLoadingState());
            if (indexer.isLoadingComplete()) {
                telemetry.addData("", "ALL SLOTS FULL!");
            }
        }

        // Shooting status
        if (indexer.getShootingState() != IndexerManager.ShootingState.IDLE) {
            telemetry.addData("Shooting", indexer.getShootingState());
            telemetry.addData("Shot", indexer.getCurrentShotNumber() + " of 3");
        }

        if (flywheelActive) {
            telemetry.addData("Flywheel", "ACTIVE");
        }

        if (targeting) {
            telemetry.addData("Auto-Drive", "Navigating to scoring position");
        }

        telemetry.addLine();
        telemetry.addData("GP1", "Sticks: Drive | A: Reset Yaw | X: Auto-Score");
        telemetry.addData("GP2", "L-Bump: Intake | R-Bump: Shoot | Y: Clear | A: Reset");

        telemetry.update();
    }

    private void driveFieldRelative(double forward, double right, double rotate, Pose2D odoPose) {
        double theta = Math.atan2(forward, right);
        double r = Math.hypot(right, forward);
        theta = AngleUnit.normalizeRadians(theta - odoPose.getHeading(AngleUnit.RADIANS));
        double newForward = r * Math.sin(theta);
        double newRight = r * Math.cos(theta);
        robot.drive(newForward, newRight, rotate);
    }
}