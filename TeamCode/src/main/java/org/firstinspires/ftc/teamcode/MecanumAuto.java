package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

@Autonomous(name = "Robot: Autonomous Mecanum Drive", group = "Robot")
public class MecanumAuto extends LinearOpMode {
    private RobotHardware robot;

    @Override
    public void runOpMode() {
        robot = new RobotHardware(hardwareMap);
        robot.getPinpoint().setPosition(new Pose2D(DistanceUnit.INCH, 0, 0, AngleUnit.DEGREES, 45));

        telemetry.addData(">", "Robot Ready.  Press Play.");
        telemetry.update();

        waitForStart();

        // Step 1: Detect alliance
        robot.detectAlliance();
        telemetry.addData("Alliance Detected", RobotData.alliance);
        if (!RobotData.teamIndicatorSeen) {
            telemetry.addData("Alliance Detection", "Failed - Timeout");
        }
        telemetry.update();

        // Step 2: Turn to face obelisk based on alliance
        Pose2D currentPose = robot.updatePoseWithFusion();
        String alliance = RobotData.alliance;
        if (alliance.equals("Red")) {
            turnRelativeDegrees(45); // CCW for Red
        } else if (alliance.equals("Blue")) {
            turnRelativeDegrees(-45); // CW for Blue
        } else {
            telemetry.addData("Alliance Unknown", "No turn performed");
            telemetry.update();
        }

        // Step 3: Detect motif/pattern
        robot.detectPattern();
        telemetry.addData("Pattern Detected", RobotData.pattern);
        if (RobotData.pattern.equals("NotSet")) {
            telemetry.addData("Pattern Detection", "Failed - Timeout");
        }
        telemetry.update();

        // Step 4: Move to scoring location if alliance is known
        if (!RobotData.alliance.equals("NotSet")) {
            Pose2D target = RobotData.alliance.equals("Red") ? RobotData.RED_SCORING_POSE : RobotData.BLUE_SCORING_POSE;
            robot.driveToPose(target, this);
        }

        // Step 5: Save final pose for teleop
        RobotData.finalAutoPose = robot.updatePoseWithFusion();
        RobotData.autoCompleted = true;

        // Optional: Proceed with rest of auton
        while (opModeIsActive()) {
            idle();
        }
    }

    private void turnRelativeDegrees(double relativeAngleDeg) {
        double maxTurnPower = 0.3;
        double tolerance = 2.0;
        double k_h = 0.02; // Proportional gain - tune this value

        Pose2D currentPose = robot.updatePoseWithFusion();
        double initialHeading = currentPose.getHeading(AngleUnit.DEGREES);
        double targetHeading = AngleUnit.normalizeDegrees(initialHeading + relativeAngleDeg);

        while (opModeIsActive() && Math.abs(AngleUnit.normalizeDegrees(targetHeading - currentPose.getHeading(AngleUnit.DEGREES))) > tolerance) {
            currentPose = robot.updatePoseWithFusion();
            double headingError = AngleUnit.normalizeDegrees(targetHeading - currentPose.getHeading(AngleUnit.DEGREES));

            double rotate = k_h * headingError;

            // Cap the rotation power
            rotate = Math.max(-maxTurnPower, Math.min(maxTurnPower, rotate));

            robot.drive(0, 0, rotate);

            telemetry.addData("Turning Relative", relativeAngleDeg + " deg");
            telemetry.addData("Target Heading", targetHeading);
            telemetry.addData("Current Heading", currentPose.getHeading(AngleUnit.DEGREES));
            telemetry.addData("Heading Error", headingError);
            telemetry.update();
            sleep(10);
        }

        robot.drive(0, 0, 0);
        telemetry.addData("Turn Complete", "Reached target heading: " + targetHeading);
        telemetry.update();
    }
}