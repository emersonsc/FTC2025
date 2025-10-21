package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import java.util.List;

/**
 * Helper class for detecting artifact colors using Limelight
 * This provides multiple detection methods you can choose from based on your setup
 */
public class LimelightColorDetector {

    private Limelight3A limelight;

    // Detection thresholds
    private static final double MIN_TARGET_AREA = 0.5;  // Minimum target area to consider valid
    private static final double MIN_CONFIDENCE = 0.7;    // Minimum confidence for classifier

    // Pipeline indices (configure these to match your Limelight setup)
    private static final int GREEN_PIPELINE = 2;   // Pipeline configured for green detection
    private static final int PURPLE_PIPELINE = 3;  // Pipeline configured for purple detection

    public LimelightColorDetector(Limelight3A limelight) {
        this.limelight = limelight;
    }

    /**
     * METHOD 1: Dual-Pipeline Approach
     * Switches between two pipelines and checks which one has better detection
     * Best for: Simple color blob detection with two separate HSV-tuned pipelines
     */
    public IndexerManager.ArtifactColor detectColorDualPipeline() {
        // Check green pipeline
        limelight.pipelineSwitch(GREEN_PIPELINE);
        try { Thread.sleep(50); } catch (InterruptedException e) {} // Give time to switch

        LLResult greenResult = limelight.getLatestResult();
        double greenArea = 0;
        if (greenResult != null && greenResult.isValid()) {
            List<LLResultTypes.ColorResult> colorResults = greenResult.getColorResults();
            if (!colorResults.isEmpty()) {
                greenArea = colorResults.get(0).getTargetArea();
            }
        }

        // Check purple pipeline
        limelight.pipelineSwitch(PURPLE_PIPELINE);
        try { Thread.sleep(50); } catch (InterruptedException e) {}

        LLResult purpleResult = limelight.getLatestResult();
        double purpleArea = 0;
        if (purpleResult != null && purpleResult.isValid()) {
            List<LLResultTypes.ColorResult> colorResults = purpleResult.getColorResults();
            if (!colorResults.isEmpty()) {
                purpleArea = colorResults.get(0).getTargetArea();
            }
        }

        // Return the color with larger detection area
        if (greenArea > MIN_TARGET_AREA || purpleArea > MIN_TARGET_AREA) {
            return (greenArea > purpleArea) ?
                    IndexerManager.ArtifactColor.GREEN :
                    IndexerManager.ArtifactColor.PURPLE;
        }

        return IndexerManager.ArtifactColor.NONE;
    }

    /**
     * METHOD 2: Single Pipeline with HSV Range Checking
     * Uses one pipeline and analyzes the color properties
     * Best for: When you want to check both colors in one pipeline
     */
    public IndexerManager.ArtifactColor detectColorSinglePipeline() {
        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            return IndexerManager.ArtifactColor.NONE;
        }

        List<LLResultTypes.ColorResult> colorResults = result.getColorResults();
        if (colorResults.isEmpty()) {
            return IndexerManager.ArtifactColor.NONE;
        }

        LLResultTypes.ColorResult target = colorResults.get(0);

        // Check if target is large enough
        if (target.getTargetArea() < MIN_TARGET_AREA) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Get target position - if you've set up regions in Limelight
        // or use target X/Y position to determine which artifact
        double tx = target.getTargetXDegrees();
        double ty = target.getTargetYDegrees();

        // Example: If artifacts pass through center of frame
        // and you can distinguish by position or other features
        // This is just an example - adjust based on your setup

        return IndexerManager.ArtifactColor.NONE; // Replace with your logic
    }

    /**
     * METHOD 3: Classifier-Based Detection
     * Uses a trained neural network classifier
     * Best for: Most accurate detection when you have a trained model
     */
    public IndexerManager.ArtifactColor detectColorClassifier() {
        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Check classifier results
        List<LLResultTypes.ClassifierResult> classifierResults = result.getClassifierResults();

        if (classifierResults.isEmpty()) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Get the highest confidence result
        LLResultTypes.ClassifierResult bestResult = classifierResults.get(0);

        // Check confidence threshold
        if (bestResult.getConfidence() < MIN_CONFIDENCE) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Parse class name
        String className = bestResult.getClassName().toLowerCase();

        if (className.contains("green")) {
            return IndexerManager.ArtifactColor.GREEN;
        } else if (className.contains("purple")) {
            return IndexerManager.ArtifactColor.PURPLE;
        }

        return IndexerManager.ArtifactColor.NONE;
    }

    /**
     * METHOD 4: Detector-Based Detection
     * Uses object detection (like YOLO or similar)
     * Best for: When you have a trained object detector
     */
    public IndexerManager.ArtifactColor detectColorDetector() {
        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Check detector results
        List<LLResultTypes.DetectorResult> detectorResults = result.getDetectorResults();

        if (detectorResults.isEmpty()) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Get the first detected object (closest/largest)
        LLResultTypes.DetectorResult detection = detectorResults.get(0);

        // Check confidence
        if (detection.getConfidence() < MIN_CONFIDENCE) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Parse class name
        String className = detection.getClassName().toLowerCase();

        if (className.contains("green")) {
            return IndexerManager.ArtifactColor.GREEN;
        } else if (className.contains("purple")) {
            return IndexerManager.ArtifactColor.PURPLE;
        }

        return IndexerManager.ArtifactColor.NONE;
    }

    /**
     * METHOD 5: Python Script Results
     * Uses custom Python script running on Limelight
     * Best for: Custom logic that's easier to implement in Python
     */
    public IndexerManager.ArtifactColor detectColorPythonScript() {
        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            return IndexerManager.ArtifactColor.NONE;
        }

        // Python scripts can output custom data through various means
        // Check the Limelight API for how your Python script sends data

        // Example: If Python script outputs via custom corners or other method
        List<LLResultTypes.ColorResult> colorResults = result.getColorResults();

        if (!colorResults.isEmpty()) {
            LLResultTypes.ColorResult pythonOutput = colorResults.get(0);

            // Your Python script might encode the color in a specific way
            // For example, using target X position as a signal:
            // tx < 0 = GREEN, tx > 0 = PURPLE

            double tx = pythonOutput.getTargetXDegrees();

            if (Math.abs(tx) > 1.0) { // Threshold to avoid noise
                return (tx < 0) ?
                        IndexerManager.ArtifactColor.GREEN :
                        IndexerManager.ArtifactColor.PURPLE;
            }
        }

        return IndexerManager.ArtifactColor.NONE;
    }

    /**
     * RECOMMENDED: Robust detection with fallbacks
     * Tries multiple methods and uses the most confident result
     */
    public IndexerManager.ArtifactColor detectColorRobust() {
        // Try classifier first (most accurate if you have one)
        IndexerManager.ArtifactColor classifierResult = detectColorClassifier();
        if (classifierResult != IndexerManager.ArtifactColor.NONE) {
            return classifierResult;
        }

        // Fall back to detector
        IndexerManager.ArtifactColor detectorResult = detectColorDetector();
        if (detectorResult != IndexerManager.ArtifactColor.NONE) {
            return detectorResult;
        }

        // Fall back to color pipeline
        return detectColorDualPipeline();
    }
}

/**
 * ========== INTEGRATION INTO SimplifiedIndexerTeleOp ==========
 *
 * 1. Add field to your TeleOp class:
 *    private LimelightColorDetector colorDetector;
 *
 * 2. In init() method:
 *    colorDetector = new LimelightColorDetector(robot.getLimelight());
 *
 * 3. Replace the detectArtifactColor() method:
 *    private IndexerManager.ArtifactColor detectArtifactColor() {
 *        // Choose the method that matches your Limelight setup:
 *        return colorDetector.detectColorRobust();
 *        // OR
 *        // return colorDetector.detectColorClassifier();
 *        // OR
 *        // return colorDetector.detectColorDualPipeline();
 *    }
 */

/**
 * ========== LIMELIGHT SETUP RECOMMENDATIONS ==========
 *
 * OPTION A: Dual Color Pipeline (Easiest to set up)
 * -------------------------------------------------
 * 1. Create Pipeline 2 - "Green Detection"
 *    - Use Color pipeline type
 *    - Tune HSV ranges for green artifacts
 *    - Set appropriate erosion/dilation
 *
 * 2. Create Pipeline 3 - "Purple Detection"
 *    - Use Color pipeline type
 *    - Tune HSV ranges for purple artifacts
 *    - Set appropriate erosion/dilation
 *
 * 3. Use detectColorDualPipeline() method
 *
 * OPTION B: Neural Network Classifier (Most Accurate)
 * ---------------------------------------------------
 * 1. Collect training images of green and purple artifacts
 * 2. Train a classifier using Limelight's tools
 * 3. Upload classifier to Limelight
 * 4. Use detectColorClassifier() method
 *
 * OPTION C: Object Detector (Good for complex scenes)
 * ---------------------------------------------------
 * 1. Collect and label training images
 * 2. Train object detector (YOLO, etc.)
 * 3. Upload to Limelight
 * 4. Use detectColorDetector() method
 *
 * TIPS:
 * - Test under various lighting conditions
 * - Use good lighting near intake area
 * - Consider adding a small LED ring light near camera
 * - Tune detection thresholds (MIN_TARGET_AREA, MIN_CONFIDENCE)
 */