package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.Servo;

public class IndexerManager {
    private Servo indexerServo;
    private Servo lifterServo;

    // Artifact colors
    public enum ArtifactColor {
        GREEN,
        PURPLE,
        NONE  // Empty slot
    }

    // State machine for loading
    public enum LoadingState {
        IDLE,
        WAITING_FOR_ARTIFACT,
        ARTIFACT_DETECTED,
        ADVANCING_TO_NEXT_SLOT,
        COMPLETE
    }

    // State machine for shooting
    public enum ShootingState {
        IDLE,
        ROTATING_TO_SLOT,
        LIFTING_ARTIFACT,
        SHOOTING,
        LOWERING_LIFTER,
        COMPLETE
    }

    // Physical slot contents
    private ArtifactColor slotOne = ArtifactColor.NONE;
    private ArtifactColor slotTwo = ArtifactColor.NONE;
    private ArtifactColor slotThree = ArtifactColor.NONE;

    // INTAKE positions (front of robot) - where artifacts are loaded
    private static final double INTAKE_SLOT_ONE = 0.0;      // 0°
    private static final double INTAKE_SLOT_TWO = 0.0667;   // 120°
    private static final double INTAKE_SLOT_THREE = 0.1333; // 240°

    // SHOOTER positions (180° opposite of intake) - where artifacts are shot
    private static final double SHOOTER_SLOT_ONE = 0.1;      // 180°
    private static final double SHOOTER_SLOT_TWO = 0.1667;   // 300°
    private static final double SHOOTER_SLOT_THREE = 0.2333; // 420°

    // Lifter servo positions
    private static final double LIFTER_HOME = 0.0;    // Down position
    private static final double LIFTER_LIFT = 0.5;    // Up position (adjust as needed)

    // Current position tracking
    private double currentPosition = INTAKE_SLOT_ONE;
    private int currentSlotAtIntake = 1;
    private double totalRotation = 0.0;

    // Loading state
    private LoadingState loadingState = LoadingState.IDLE;
    private ArtifactColor lastDetectedColor = ArtifactColor.NONE;
    private long lastColorDetectionTime = 0;
    private static final long COLOR_DETECTION_DEBOUNCE = 500; // ms

    // Shooting state
    private ShootingState shootingState = ShootingState.IDLE;
    private int[] shootingSequence = null;
    private int currentShotIndex = 0;
    private long stateStartTime = 0;
    private static final long ROTATION_SETTLE_TIME = 500;    // ms to wait after rotation
    private static final long LIFT_TIME = 300;               // ms to lift artifact
    private static final long SHOOT_TIME = 500;              // ms to shoot
    private static final long LOWER_TIME = 300;              // ms to lower lifter

    public IndexerManager(Servo indexerServo, Servo lifterServo) {
        this.indexerServo = indexerServo;
        this.lifterServo = lifterServo;
        this.indexerServo.setPosition(INTAKE_SLOT_ONE);
        this.lifterServo.setPosition(LIFTER_HOME);
        this.currentPosition = INTAKE_SLOT_ONE;
        this.currentSlotAtIntake = 1;
    }

    // ========== LOADING METHODS ==========

    /**
     * Start the loading process - call this when intake button is pressed
     */
    public void startLoading() {
        if (loadingState == LoadingState.IDLE || loadingState == LoadingState.COMPLETE) {
            // Reset to first empty slot
            if (slotOne == ArtifactColor.NONE) {
                rotateSlotToIntake(1);
            } else if (slotTwo == ArtifactColor.NONE) {
                rotateSlotToIntake(2);
            } else if (slotThree == ArtifactColor.NONE) {
                rotateSlotToIntake(3);
            }
            loadingState = LoadingState.WAITING_FOR_ARTIFACT;
            lastDetectedColor = ArtifactColor.NONE;
        }
    }

    /**
     * Stop the loading process - call this when intake button is released or all slots full
     */
    public void stopLoading() {
        loadingState = LoadingState.IDLE;
    }

    /**
     * Update loading state - call this in your loop() with detected color from Limelight
     * @param detectedColor The color currently seen by Limelight (NONE if nothing detected)
     */
    public void updateLoading(ArtifactColor detectedColor) {
        if (loadingState == LoadingState.IDLE || loadingState == LoadingState.COMPLETE) {
            return;
        }

        switch (loadingState) {
            case WAITING_FOR_ARTIFACT:
                // Check if we detect a new artifact (with debouncing)
                if (detectedColor != ArtifactColor.NONE && detectedColor != lastDetectedColor) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastColorDetectionTime > COLOR_DETECTION_DEBOUNCE) {
                        // Load the artifact into current slot
                        loadArtifactAtCurrentSlot(detectedColor);
                        lastDetectedColor = detectedColor;
                        lastColorDetectionTime = currentTime;
                        loadingState = LoadingState.ARTIFACT_DETECTED;
                    }
                }
                break;

            case ARTIFACT_DETECTED:
                // Wait a moment, then advance to next slot
                if (System.currentTimeMillis() - lastColorDetectionTime > 200) {
                    // Check if all slots are full
                    if (slotOne != ArtifactColor.NONE &&
                            slotTwo != ArtifactColor.NONE &&
                            slotThree != ArtifactColor.NONE) {
                        loadingState = LoadingState.COMPLETE;
                    } else {
                        // Find next empty slot
                        advanceToNextEmptySlot();
                        loadingState = LoadingState.WAITING_FOR_ARTIFACT;
                        lastDetectedColor = ArtifactColor.NONE;
                    }
                }
                break;
        }
    }

    /**
     * Check if loading is complete (all 3 slots full)
     */
    public boolean isLoadingComplete() {
        return slotOne != ArtifactColor.NONE &&
                slotTwo != ArtifactColor.NONE &&
                slotThree != ArtifactColor.NONE;
    }

    /**
     * Get current loading state
     */
    public LoadingState getLoadingState() {
        return loadingState;
    }

    // ========== SHOOTING METHODS ==========

    /**
     * Start the shooting sequence - call this when shoot button is pressed
     * @param motif The detected motif (GPP, PGP, or PPG)
     * @return true if sequence started, false if no valid sequence possible
     */
    public boolean startShooting(String motif) {
        if (shootingState != ShootingState.IDLE && shootingState != ShootingState.COMPLETE) {
            return false; // Already shooting
        }

        // Calculate shooting sequence
        shootingSequence = calculateShootingSequence(motif);

        if (shootingSequence.length == 0) {
            return false; // No valid sequence
        }

        currentShotIndex = 0;
        shootingState = ShootingState.ROTATING_TO_SLOT;
        stateStartTime = System.currentTimeMillis();

        // Start by rotating first slot to shooter
        rotateSlotToShooter(shootingSequence[currentShotIndex]);

        return true;
    }

    /**
     * Update shooting state machine - call this in your loop()
     * @param flywheelReady Set to true when your flywheel is up to speed
     */
    public void updateShooting(boolean flywheelReady) {
        if (shootingState == ShootingState.IDLE || shootingState == ShootingState.COMPLETE) {
            return;
        }

        long elapsed = System.currentTimeMillis() - stateStartTime;

        switch (shootingState) {
            case ROTATING_TO_SLOT:
                // Wait for rotation to settle
                if (elapsed > ROTATION_SETTLE_TIME) {
                    shootingState = ShootingState.LIFTING_ARTIFACT;
                    stateStartTime = System.currentTimeMillis();
                    lifterServo.setPosition(LIFTER_LIFT);
                }
                break;

            case LIFTING_ARTIFACT:
                // Wait for lifter to reach top position
                if (elapsed > LIFT_TIME) {
                    shootingState = ShootingState.SHOOTING;
                    stateStartTime = System.currentTimeMillis();
                }
                break;

            case SHOOTING:
                // Wait for flywheel to shoot the artifact
                if (elapsed > SHOOT_TIME && flywheelReady) {
                    // Clear the slot we just shot
                    clearSlot(shootingSequence[currentShotIndex]);

                    shootingState = ShootingState.LOWERING_LIFTER;
                    stateStartTime = System.currentTimeMillis();
                    lifterServo.setPosition(LIFTER_HOME);
                }
                break;

            case LOWERING_LIFTER:
                // Wait for lifter to return home
                if (elapsed > LOWER_TIME) {
                    // Move to next shot
                    currentShotIndex++;

                    if (currentShotIndex < shootingSequence.length) {
                        // More shots to go
                        shootingState = ShootingState.ROTATING_TO_SLOT;
                        stateStartTime = System.currentTimeMillis();
                        rotateSlotToShooter(shootingSequence[currentShotIndex]);
                    } else {
                        // All done!
                        shootingState = ShootingState.COMPLETE;
                    }
                }
                break;
        }
    }

    /**
     * Check if shooting sequence is complete
     */
    public boolean isShootingComplete() {
        return shootingState == ShootingState.COMPLETE;
    }

    /**
     * Get current shooting state
     */
    public ShootingState getShootingState() {
        return shootingState;
    }

    /**
     * Get the current shot number (1-3) or 0 if not shooting
     */
    public int getCurrentShotNumber() {
        if (shootingState == ShootingState.IDLE || shootingState == ShootingState.COMPLETE) {
            return 0;
        }
        return currentShotIndex + 1;
    }

    /**
     * Reset shooting state (in case of abort)
     */
    public void stopShooting() {
        shootingState = ShootingState.IDLE;
        lifterServo.setPosition(LIFTER_HOME);
    }

    // ========== HELPER METHODS ==========

    /**
     * Calculate shooting sequence based on motif
     * Shoots best match first, then remaining artifacts even if wrong colors
     */
    private int[] calculateShootingSequence(String motif) {
        if (motif == null || motif.equals("NotSet")) {
            // No motif - just shoot whatever we have in order
            return getAnyAvailableSequence();
        }

        ArtifactColor[] requiredOrder = getMotifOrder(motif);
        int[] sequence = new int[3];
        boolean[] slotUsed = new boolean[4]; // Index 0 unused, 1-3 for slots
        int sequenceIndex = 0;

        // First pass: Find slots that match the motif in order
        for (int i = 0; i < 3; i++) {
            ArtifactColor needed = requiredOrder[i];

            // Find an unused slot with this color
            for (int slot = 1; slot <= 3; slot++) {
                if (!slotUsed[slot] && getSlotColor(slot) == needed) {
                    sequence[sequenceIndex++] = slot;
                    slotUsed[slot] = true;
                    break;
                }
            }
        }

        // Second pass: Add any remaining artifacts (even if wrong color)
        for (int slot = 1; slot <= 3; slot++) {
            if (!slotUsed[slot] && getSlotColor(slot) != ArtifactColor.NONE) {
                sequence[sequenceIndex++] = slot;
                slotUsed[slot] = true;
            }
        }

        // Return only the filled portion of the array
        int[] result = new int[sequenceIndex];
        System.arraycopy(sequence, 0, result, 0, sequenceIndex);
        return result;
    }

    /**
     * Get shooting sequence for any available artifacts (no motif)
     */
    private int[] getAnyAvailableSequence() {
        int[] temp = new int[3];
        int count = 0;

        if (slotOne != ArtifactColor.NONE) temp[count++] = 1;
        if (slotTwo != ArtifactColor.NONE) temp[count++] = 2;
        if (slotThree != ArtifactColor.NONE) temp[count++] = 3;

        int[] result = new int[count];
        System.arraycopy(temp, 0, result, 0, count);
        return result;
    }

    /**
     * Get the motif order as colors
     */
    private ArtifactColor[] getMotifOrder(String motif) {
        ArtifactColor[] order = new ArtifactColor[3];
        for (int i = 0; i < 3 && i < motif.length(); i++) {
            char c = motif.charAt(i);
            order[i] = (c == 'G') ? ArtifactColor.GREEN : ArtifactColor.PURPLE;
        }
        return order;
    }

    /**
     * Load artifact into the current slot at intake
     */
    private void loadArtifactAtCurrentSlot(ArtifactColor color) {
        switch (currentSlotAtIntake) {
            case 1: slotOne = color; break;
            case 2: slotTwo = color; break;
            case 3: slotThree = color; break;
        }
    }

    /**
     * Manually load an artifact into a specific slot (for testing and autonomous preload)
     * @param slotNumber 1, 2, or 3
     * @param color GREEN or PURPLE
     */
    public void loadArtifactManual(int slotNumber, ArtifactColor color) {
        switch (slotNumber) {
            case 1: slotOne = color; break;
            case 2: slotTwo = color; break;
            case 3: slotThree = color; break;
        }
    }

    /**
     * Advance to the next empty slot
     */
    private void advanceToNextEmptySlot() {
        // Find next empty slot starting from current position
        for (int i = 1; i <= 3; i++) {
            int checkSlot = currentSlotAtIntake + i;
            if (checkSlot > 3) checkSlot -= 3;

            if (getSlotColor(checkSlot) == ArtifactColor.NONE) {
                rotateSlotToIntake(checkSlot);
                return;
            }
        }
        // If we get here, all slots are full
        loadingState = LoadingState.COMPLETE;
    }

    /**
     * Get the color of artifact in a specific slot
     */
    public ArtifactColor getSlotColor(int slotNumber) {
        switch (slotNumber) {
            case 1: return slotOne;
            case 2: return slotTwo;
            case 3: return slotThree;
            default: return ArtifactColor.NONE;
        }
    }

    /**
     * Clear a specific slot
     */
    public void clearSlot(int slotNumber) {
        switch (slotNumber) {
            case 1: slotOne = ArtifactColor.NONE; break;
            case 2: slotTwo = ArtifactColor.NONE; break;
            case 3: slotThree = ArtifactColor.NONE; break;
        }
    }

    /**
     * Clear all slots
     */
    public void clearAllSlots() {
        slotOne = ArtifactColor.NONE;
        slotTwo = ArtifactColor.NONE;
        slotThree = ArtifactColor.NONE;
    }

    /**
     * Get which slot is currently at intake
     */
    public int getCurrentSlotAtIntake() {
        return currentSlotAtIntake;
    }

    /**
     * Get which slot is currently at shooter
     */
    public int getCurrentSlotAtShooter() {
        int shooterSlot = currentSlotAtIntake + 1;
        if (shooterSlot > 3) shooterSlot = 1;
        return shooterSlot;
    }

    // ========== SERVO CONTROL METHODS ==========

    /**
     * Rotate a slot to the shooter position (180° from intake)
     */
    public void rotateSlotToShooter(int slotNumber) {
        if (slotNumber < 1 || slotNumber > 3) return;

        double targetPosition;
        switch (slotNumber) {
            case 1: targetPosition = SHOOTER_SLOT_ONE; break;
            case 2: targetPosition = SHOOTER_SLOT_TWO; break;
            case 3: targetPosition = SHOOTER_SLOT_THREE; break;
            default: return;
        }

        double newPosition = calculateShortestPath(targetPosition);
        indexerServo.setPosition(newPosition);
        currentPosition = newPosition;

        currentSlotAtIntake = slotNumber - 1;
        if (currentSlotAtIntake < 1) currentSlotAtIntake = 3;
    }

    /**
     * Rotate a slot to the intake position
     */
    private void rotateSlotToIntake(int slotNumber) {
        if (slotNumber < 1 || slotNumber > 3) return;

        double targetPosition;
        switch (slotNumber) {
            case 1: targetPosition = INTAKE_SLOT_ONE; break;
            case 2: targetPosition = INTAKE_SLOT_TWO; break;
            case 3: targetPosition = INTAKE_SLOT_THREE; break;
            default: return;
        }

        double newPosition = calculateShortestPath(targetPosition);
        indexerServo.setPosition(newPosition);
        currentPosition = newPosition;
        currentSlotAtIntake = slotNumber;
    }

    /**
     * Calculate shortest path to target position
     */
    private double calculateShortestPath(double targetPosition) {
        double currentDegrees = (currentPosition * 1800.0) % 360.0;
        double targetDegrees = (targetPosition * 1800.0) % 360.0;

        double delta = targetDegrees - currentDegrees;
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;

        totalRotation += delta;
        double newPosition = totalRotation / 1800.0;

        if (newPosition < 0.0) {
            newPosition = 0.0;
            totalRotation = 0.0;
        } else if (newPosition > 1.0) {
            newPosition = 1.0;
            totalRotation = 1800.0;
        }

        return newPosition;
    }

    /**
     * Reset to home position
     */
    public void resetToHome() {
        totalRotation = 0.0;
        currentPosition = INTAKE_SLOT_ONE;
        currentSlotAtIntake = 1;
        indexerServo.setPosition(INTAKE_SLOT_ONE);
        lifterServo.setPosition(LIFTER_HOME);
        loadingState = LoadingState.IDLE;
        shootingState = ShootingState.IDLE;
    }

    /**
     * Get status string for telemetry
     */
    public String getStatusString() {
        return String.format("Slots: %s|%s|%s | Intake=Slot%d Shooter=Slot%d",
                getColorChar(slotOne), getColorChar(slotTwo), getColorChar(slotThree),
                currentSlotAtIntake, getCurrentSlotAtShooter());
    }

    private char getColorChar(ArtifactColor color) {
        switch (color) {
            case GREEN: return 'G';
            case PURPLE: return 'P';
            default: return '-';
        }
    }
}