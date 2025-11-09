package org.firstinspires.ftc.teamcode;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

public class RobotData {
    public static boolean autoCompleted = false;
    public static Pose2D finalAutoPose = null;
    public static String alliance = "NotSet";
    public static String pattern = "NotSet";
    public static boolean teamIndicatorSeen = false;

    // Scoring poses - adjust these based on your field coordinate system and goal positions
    // Assuming origin at center, X positive to audience, Y positive to red alliance, units in inches
    // Calculated for 30 inches in front, facing the goal, based on tag positions from Chief Delphi post
    //public static final Pose2D BLUE_SCORING_POSE = new Pose2D(DistanceUnit.INCH, -40.71, -31.38, AngleUnit.DEGREES, 234);
     public static final Pose2D BLUE_SCORING_POSE = new Pose2D(DistanceUnit.INCH, -40.71, 0, AngleUnit.DEGREES, -90);

    public static final Pose2D RED_SCORING_POSE = new Pose2D(DistanceUnit.INCH, -40.71, 0, AngleUnit.DEGREES, 90);
}