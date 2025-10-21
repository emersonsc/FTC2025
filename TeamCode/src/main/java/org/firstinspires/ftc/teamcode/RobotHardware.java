package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;

import java.util.List;

public class RobotHardware {
    private GoBildaPinpointDriver pinpoint;
    private Limelight3A limelight;
    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private IMU imu;
    private Servo Team_Indicator, pattern1, pattern2, pattern3;

    private static final double RED_INDICATOR = 0.27;
    private static final double BLUE_INDICATOR = 0.61;
    private static final double GREEN_INDICATOR = 0.48;
    private static final double PURPLE_INDICATOR = 0.69;
    private static final double OFF_INDICATOR = 0.0;

    public static final int RED_APRILTAG_ID = 24;
    public static final int BLUE_APRILTAG_ID = 20;
    public static final int GPP_APRILTAG_ID = 21;
    public static final int PGP_APRILTAG_ID = 22;
    public static final int PPG_APRILTAG_ID = 23;

    private double trustVision = 0.8;
    private double maxPoseError = 10.0;
    private double maxHeadingError = 15.0;

    public RobotHardware(HardwareMap hardwareMap) {
        pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, "pinpoint");
        configurePinpoint();

        frontLeft = hardwareMap.get(DcMotor.class, "front_left_drive");
        frontRight = hardwareMap.get(DcMotor.class, "front_right_drive");
        backLeft = hardwareMap.get(DcMotor.class, "back_left_drive");
        backRight = hardwareMap.get(DcMotor.class, "back_right_drive");

        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontLeft.setDirection(DcMotor.Direction.REVERSE);

        frontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        imu = hardwareMap.get(IMU.class, "imu");
        RevHubOrientationOnRobot orientationOnRobot = new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.RIGHT);
        imu.initialize(new IMU.Parameters(orientationOnRobot));

        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        limelight.pipelineSwitch(0);
        limelight.start();

        Team_Indicator = hardwareMap.get(Servo.class, "TeamIndicator");
        Team_Indicator.setPosition(OFF_INDICATOR);
        pattern1 = hardwareMap.get(Servo.class, "pattern1");
        pattern1.setPosition(OFF_INDICATOR);
        pattern2 = hardwareMap.get(Servo.class, "pattern2");
        pattern2.setPosition(OFF_INDICATOR);
        pattern3 = hardwareMap.get(Servo.class, "pattern3");
        pattern3.setPosition(OFF_INDICATOR);
    }

    public void configurePinpoint() {
        pinpoint.setOffsets(-84.0, -168.0, DistanceUnit.MM);
        pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD);
        pinpoint.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD,
                GoBildaPinpointDriver.EncoderDirection.FORWARD);
        pinpoint.resetPosAndIMU();
    }

    public void drive(double forward, double right, double rotate) {
        double frontLeftPower = forward + right + rotate;
        double frontRightPower = forward - right - rotate;
        double backRightPower = forward + right - rotate;
        double backLeftPower = forward - right + rotate;

        double maxPower = 1.0;
        double maxSpeed = 1.0;

        maxPower = Math.max(maxPower, Math.abs(frontLeftPower));
        maxPower = Math.max(maxPower, Math.abs(frontRightPower));
        maxPower = Math.max(maxPower, Math.abs(backRightPower));
        maxPower = Math.max(maxPower, Math.abs(backLeftPower));

        frontLeft.setPower(maxSpeed * (frontLeftPower / maxPower));
        frontRight.setPower(maxSpeed * (frontRightPower / maxPower));
        backLeft.setPower(maxSpeed * (backLeftPower / maxPower));
        backRight.setPower(maxSpeed * (backRightPower / maxPower));
    }

    public Pose2D updatePoseWithFusion() {
        pinpoint.update();
        Pose2D odoPose = pinpoint.getPosition();
        double odoX = odoPose.getX(DistanceUnit.INCH);
        double odoY = odoPose.getY(DistanceUnit.INCH);
        double odoHeading = odoPose.getHeading(AngleUnit.DEGREES);

        LLResult result = limelight.getLatestResult();
        if (result != null && result.isValid()) {
            Pose3D botpose = result.getBotpose();
            Position limePos = botpose.getPosition();
            double visionX = limePos.x * 39.3701;
            double visionY = limePos.y * 39.3701;
            double visionHeading = botpose.getOrientation().getYaw(AngleUnit.DEGREES);

            boolean allianceTagDetected = false;
            for (LLResultTypes.FiducialResult fr : result.getFiducialResults()) {
                int id = fr.getFiducialId();
                if (id == RED_APRILTAG_ID || id == BLUE_APRILTAG_ID) {
                    allianceTagDetected = true;
                    break;
                }
            }

            double posError = Math.hypot(visionX - odoX, visionY - odoY);
            double headingError = Math.abs(AngleUnit.normalizeDegrees(visionHeading - odoHeading));
            boolean visionTrusted = allianceTagDetected && posError < maxPoseError && headingError < maxHeadingError && result.getFiducialResults().size() >= 1;

            if (visionTrusted) {
                double fusedX = trustVision * visionX + (1 - trustVision) * odoX;
                double fusedY = trustVision * visionY + (1 - trustVision) * odoY;
                double fusedHeading = AngleUnit.normalizeDegrees(odoHeading + trustVision * AngleUnit.normalizeDegrees(visionHeading - odoHeading));
                odoPose = new Pose2D(DistanceUnit.INCH, fusedX, fusedY, AngleUnit.DEGREES, fusedHeading);
                pinpoint.setPosition(odoPose);
            }
        }
        return odoPose;
    }

    public void detectAlliance() {
        RobotData.teamIndicatorSeen = false;
        RobotData.alliance = "NotSet";
        long startTime = System.currentTimeMillis();
        long timeout = 5000;

        while (!RobotData.teamIndicatorSeen && (System.currentTimeMillis() - startTime < timeout)) {
            updatePoseWithFusion();
            LLResult result = limelight.getLatestResult();
            if (result != null && result.isValid()) {
                List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();
                for (LLResultTypes.FiducialResult fr : fiducialResults) {
                    int id = fr.getFiducialId();
                    if (id == RED_APRILTAG_ID) {
                        setTeamIndicator("Red");
                        RobotData.teamIndicatorSeen = true;
                        RobotData.alliance = "Red";
                    } else if (id == BLUE_APRILTAG_ID) {
                        setTeamIndicator("Blue");
                        RobotData.teamIndicatorSeen = true;
                        RobotData.alliance = "Blue";
                    }
                }
            }
        }
    }

    public void detectPattern() {
        RobotData.pattern = "NotSet";
        long startTime = System.currentTimeMillis();
        long timeout = 5000;

        while (RobotData.pattern.equals("NotSet") && (System.currentTimeMillis() - startTime < timeout)) {
            updatePoseWithFusion();
            LLResult result = limelight.getLatestResult();
            if (result != null && result.isValid()) {
                List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();
                for (LLResultTypes.FiducialResult fr : fiducialResults) {
                    int id = fr.getFiducialId();
                    if (id == GPP_APRILTAG_ID) {
                        setPattern("GPP");
                        RobotData.pattern = "GPP";
                    } else if (id == PGP_APRILTAG_ID) {
                        setPattern("PGP");
                        RobotData.pattern = "PGP";
                    } else if (id == PPG_APRILTAG_ID) {
                        setPattern("PPG");
                        RobotData.pattern = "PPG";
                    }
                }
            }
        }
    }

    public void setTeamIndicator(String alliance) {
        if (alliance.equals("Red")) {
            Team_Indicator.setPosition(RED_INDICATOR);
        } else if (alliance.equals("Blue")) {
            Team_Indicator.setPosition(BLUE_INDICATOR);
        }
    }

    public void setPattern(String pattern) {
        if (pattern.equals("GPP")) {
            pattern1.setPosition(GREEN_INDICATOR);
            pattern2.setPosition(PURPLE_INDICATOR);
            pattern3.setPosition(PURPLE_INDICATOR);
        } else if (pattern.equals("PGP")) {
            pattern1.setPosition(PURPLE_INDICATOR);
            pattern2.setPosition(GREEN_INDICATOR);
            pattern3.setPosition(PURPLE_INDICATOR);
        } else if (pattern.equals("PPG")) {
            pattern1.setPosition(PURPLE_INDICATOR);
            pattern2.setPosition(PURPLE_INDICATOR);
            pattern3.setPosition(GREEN_INDICATOR);
        }
    }

    public void driveToPose(Pose2D target, LinearOpMode opMode) {
        double positionTolerance = 2.0; // inches
        double headingTolerance = 5.0; // degrees
        double k_p = 0.01; // tune position gain
        double k_h = 0.01; // tune heading gain

        while (opMode.opModeIsActive()) {
            Pose2D current = updatePoseWithFusion();
            double dx = target.getX(DistanceUnit.INCH) - current.getX(DistanceUnit.INCH);
            double dy = target.getY(DistanceUnit.INCH) - current.getY(DistanceUnit.INCH);
            double dheading = AngleUnit.normalizeDegrees(target.getHeading(AngleUnit.DEGREES) - current.getHeading(AngleUnit.DEGREES));

            double positionError = Math.hypot(dx, dy);
            if (positionError < positionTolerance && Math.abs(dheading) < headingTolerance) {
                break;
            }

            double headingRad = current.getHeading(AngleUnit.RADIANS);
            double localForward = -dx * Math.sin(headingRad) + dy * Math.cos(headingRad);
            double localRight = dx * Math.cos(headingRad) + dy * Math.sin(headingRad);
            double forward = k_p * localForward;
            double right = k_p * localRight;
            double rotate = k_h * dheading;

            forward = Math.max(-1, Math.min(1, forward));
            right = Math.max(-1, Math.min(1, right));
            rotate = Math.max(-1, Math.min(1, rotate));

            drive(forward, right, rotate);
            opMode.sleep(10);
        }
        drive(0, 0, 0);
    }

    public GoBildaPinpointDriver getPinpoint() {
        return pinpoint;
    }

    public IMU getImu() {
        return imu;
    }

    public Limelight3A getLimelight() {
        return limelight;
    }
}