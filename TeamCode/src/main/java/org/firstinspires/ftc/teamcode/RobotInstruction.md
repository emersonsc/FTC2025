# FTC Robot Driver Instructions
## Complete Driver's Guide for DECODE Season

---

## Quick Reference Card

### GAMEPAD 1 (DRIVER)
| Control | Function |
|---------|----------|
| **Left Stick** | Forward/Backward & Strafe Left/Right |
| **Right Stick** | Rotate/Turn |
| **Left Bumper** | Hold for Robot-Relative Drive |
| **A Button** | Reset IMU Heading to 0° |
| **X Button** | Auto-Drive to Scoring Position |

### GAMEPAD 2 (OPERATOR)
| Control | Function |
|---------|----------|
| **Left Bumper** | **HOLD TO INTAKE** (fully automatic) |
| **Right Bumper** | **HOLD TO SHOOT** (fully automatic) |
| **Y Button** | Clear All Indexer Slots (emergency) |
| **A Button** | Reset Indexer to Home Position |

---

## AUTONOMOUS MODE

### What Happens Automatically:
1. **Alliance Detection** - Camera looks up, detects Red/Blue AprilTag
2. **Turn to Obelisk** - Robot turns 45° (CCW for Red, CW for Blue)
3. **Motif Detection** - Camera angles at obelisk, reads pattern (GPP/PGP/PPG)
4. **Navigate to Scoring** - Camera looks up, robot drives to scoring position
5. **Save Position** - Final position saved for TeleOp handoff

### What You See:
- Alliance color displayed on indicator servo
- Pattern/motif displayed on 3 pattern servos
- Telemetry shows detected alliance and pattern
- Robot automatically positions for scoring

### If Detection Fails:
- Alliance stays "NotSet" - TeleOp will still work but no auto-scoring
- Pattern stays "NotSet" - You can still shoot, but order won't be optimized
- Robot will continue to TeleOp regardless

---

## TELEOP MODE - DRIVING (Gamepad 1)

### Standard Driving (Field-Relative)
**This is your default driving mode - easiest for drivers!**

- **Left Stick Up** = Robot moves toward AUDIENCE (regardless of robot orientation)
- **Left Stick Down** = Robot moves toward DRIVER STATIONS
- **Left Stick Left** = Robot strafes LEFT
- **Left Stick Right** = Robot strafes RIGHT
- **Right Stick Left/Right** = Robot ROTATES

**Key Concept:** Push the stick toward where you want to go on the FIELD, not relative to the robot. If you push forward, the robot goes toward the audience even if it's facing sideways!

### Robot-Relative Driving
**Hold LEFT BUMPER for robot-relative control (like a car)**

- **Left Stick Up** = Robot moves FORWARD (in direction it's facing)
- **Left Stick Down** = Robot moves BACKWARD
- **Left Stick Left** = Robot strafes LEFT (relative to robot front)
- **Left Stick Right** = Robot strafes RIGHT (relative to robot front)
- **Right Stick Left/Right** = Robot ROTATES

**When to use:** Precise positioning, crawling through tight spaces, or when field-relative feels weird.

### Reset Heading (A Button)
- Press **A** to reset IMU heading to 0°
- Use this if field-relative driving feels "off"
- "Forward" becomes the direction the robot is currently facing
- Odometry position (X, Y) is preserved

### Auto-Drive to Scoring (X Button)
- Press **X** to automatically drive to scoring position
- Robot will:
    - Navigate to correct position based on alliance (Red/Blue)
    - Face the correct direction
    - Stop when within 2 inches and 5° of target
- You can take over control at any time by moving sticks
- Camera automatically points up during this for AprilTag navigation

**Requirements:**
- Alliance must be detected (Red or Blue)
- If alliance is "NotSet", you'll get an error message

---

## TELEOP MODE - INDEXER SYSTEM (Gamepad 2)

### INTAKE MODE (Left Bumper)

**HOLD LEFT BUMPER = Fully Automatic Loading**

**What Happens:**
1. Camera automatically tilts DOWN to look at intake
2. As artifacts pass through intake, Limelight detects color (Green/Purple)
3. Indexer automatically loads artifact into current slot
4. Indexer automatically rotates to next empty slot
5. Repeats until all 3 slots are full
6. Stops automatically when full

**What You Do:**
- Hold LEFT BUMPER down
- Feed artifacts through intake one at a time
- Watch telemetry for "Loading State"
- Release when done or when "ALL SLOTS FULL" appears

**Tips:**
- Feed artifacts slowly for best color detection
- Camera needs clear view of artifacts
- Good lighting helps (consider LED ring light)
- If color detection fails, artifact still gets indexed but as "NONE"

### SHOOTING MODE (Right Bumper)

**HOLD RIGHT BUMPER = Fully Automatic Shooting Sequence**

**What Happens:**
1. Camera automatically tilts UP for navigation
2. System calculates best shooting order based on motif
3. Flywheel spins up (1 second)
4. For each shot:
    - Indexer rotates correct artifact to shooter (rear position)
    - Lifter servo raises artifact into flywheel
    - Artifact shoots
    - Lifter lowers
    - Moves to next artifact
5. Shoots all 3 artifacts automatically
6. Stops when complete

**What You Do:**
- Position robot at scoring location (use X button or drive manually)
- Hold RIGHT BUMPER down
- Wait for sequence to complete
- Release when "COMPLETE - All shots fired!" appears

**Smart Fallback:**
- If artifacts don't match motif perfectly, robot shoots:
    1. Correct colors first (for motif points)
    2. Wrong colors after (for partial points)
    3. Always empties indexer completely

**If Pattern Not Detected:**
- Robot still shoots all artifacts in slot order (1-2-3)
- Won't get motif bonus, but gets points for artifacts

### Emergency Controls

**Y Button - Clear All Slots**
- Clears all slot data
- Use if indexer gets confused
- Does NOT physically move indexer
- Resets system to empty state

**A Button - Reset to Home**
- Returns indexer to home position (Slot 1 at intake)
- Resets lifter to down position
- Clears rotation tracking
- Use if indexer position is wrong

---

## CAMERA SYSTEM (Automatic)

### You Don't Control This - It's Automatic!

**Camera automatically moves based on what you're doing:**

| Your Action | Camera Position | Purpose |
|-------------|----------------|---------|
| Driving normally | UP (forward) | AprilTag detection, navigation, pose fusion |
| Holding LEFT BUMPER (intake) | DOWN (at intake) | Color detection for artifacts |
| Holding RIGHT BUMPER (shoot) | UP (forward) | Maintain pose tracking while shooting |
| Pressing X (auto-drive) | UP (forward) | AprilTag navigation |

**Camera Settle Time:**
- Camera takes ~300ms to move and settle
- System waits for camera to settle before using data
- "Camera: Moving..." means not ready yet
- "Camera: Ready" means detection is active

---

## TELEMETRY DISPLAY

### What You'll See On Driver Station:

```
Alliance: Red          (or Blue, or NotSet)
Pattern: GPP           (or PGP, PPG, or NotSet)
Camera: APRILTAG_VIEW (Ready)

Position: X: 45.2, Y: -12.8
Heading: 127.3°

Indexer: Slots: G|P|P | Intake=Slot1 Shooter=Slot2

Loading: WAITING_FOR_ARTIFACT
(or) ALL SLOTS FULL!

Shooting: ROTATING_TO_SLOT
Shot: 2 of 3

Flywheel: ACTIVE

Auto-Drive: Navigating to scoring position

GP1: Sticks: Drive | A: Reset Yaw | X: Auto-Score
GP2: L-Bump: Intake | R-Bump: Shoot | Y: Clear | A: Reset
```

### Key Telemetry Items:

**Alliance/Pattern:**
- Shows what was detected in Auto or during TeleOp
- "NotSet" means not detected yet

**Camera:**
- Shows current position and if it's ready
- "Moving..." means wait before expecting detection

**Indexer Status:**
- `G` = Green artifact, `P` = Purple artifact, `-` = Empty slot
- Shows which slot is at intake and shooter positions

**Loading State:**
- `IDLE` = Not loading
- `WAITING_FOR_ARTIFACT` = Ready to detect
- `ARTIFACT_DETECTED` = Just loaded one
- `COMPLETE` = All slots full

**Shooting State:**
- `IDLE` = Not shooting
- `ROTATING_TO_SLOT` = Moving artifact to shooter
- `LIFTING_ARTIFACT` = Raising into flywheel
- `SHOOTING` = Firing
- `LOWERING_LIFTER` = Returning lifter
- `COMPLETE` = All shots done

---

## COMMON SCENARIOS

### Starting a Match

**Autonomous Period:**
1. Initialize robot, verify all servos at home positions
2. Press START when ready
3. Watch for alliance detection (Red/Blue indicator lights)
4. Watch for motif detection (3 pattern servos show G/P)
5. Robot drives to scoring position automatically
6. Match transitions to TeleOp

**TeleOp Period:**
1. Robot position carries over from Auto
2. Alliance and Pattern already detected
3. Start driving immediately

### Loading Artifacts (First Time)

1. Drive to artifact collection area
2. **Hold gamepad2 LEFT BUMPER**
3. Feed first artifact through intake
4. Watch telemetry: "ARTIFACT_DETECTED" appears
5. Indexer automatically advances to Slot 2
6. Feed second artifact
7. Indexer automatically advances to Slot 3
8. Feed third artifact
9. "ALL SLOTS FULL!" appears
10. **Release LEFT BUMPER**

### Scoring Artifacts

**Method 1: Manual Drive + Auto Shoot**
1. Drive to scoring position manually
2. Line up with goal
3. **Hold gamepad2 RIGHT BUMPER**
4. Wait for all 3 shots to complete
5. **Release RIGHT BUMPER**

**Method 2: Auto-Drive + Auto Shoot**
1. Press **gamepad1 X** (auto-drive starts)
2. Robot navigates to scoring position
3. When stopped, **hold gamepad2 RIGHT BUMPER**
4. Wait for all 3 shots to complete
5. **Release RIGHT BUMPER**

### Reloading After Scoring

1. Drive back to artifact collection area
2. **Hold gamepad2 LEFT BUMPER**
3. Feed 3 more artifacts (same as before)
4. System automatically detects colors and loads
5. **Release LEFT BUMPER** when full
6. Return to scoring position

### If Alliance/Pattern Not Detected

**During Auto:**
- Alliance detection failed? Driver can manually identify
- Robot won't auto-drive to scoring position
- Everything else still works in TeleOp

**During TeleOp:**
- Drive near alliance AprilTag (Red #24 or Blue #20)
- Camera will detect it when in view
- Pattern can be detected by looking at obelisk tags
- Or just shoot anyway - robot empties indexer regardless

### Emergency Situations

**Indexer Jammed:**
1. Release all buttons
2. Press gamepad2 **A** (reset to home)
3. Press gamepad2 **Y** (clear slots)
4. Manually check physical mechanism
5. Resume normal operation

**Wrong Colors Loaded:**
- Don't worry! Robot will shoot whatever it can
- Correct colors shoot first for motif
- Wrong colors shoot after for partial points

**Camera Not Detecting:**
- Check camera position in telemetry
- Ensure good lighting
- Try moving closer to target
- May need to tune HSV ranges or servo positions

**Robot Driving Wrong Direction:**
- Press gamepad1 **A** to reset heading
- Check if you're in field-relative mode (default)
- Try robot-relative mode (hold LEFT BUMPER) if confused

---

## PRACTICE DRILLS

### Drill 1: Intake Practice
- Place 3 artifacts in front of robot
- Hold LEFT BUMPER
- Feed them through one at a time
- Goal: Load all 3 without releasing button

### Drill 2: Shooting Practice
- Load 3 artifacts
- Drive to scoring position
- Hold RIGHT BUMPER
- Goal: All 3 shots automatic, no intervention

### Drill 3: Full Cycle
- Load 3 artifacts (LEFT BUMPER)
- Auto-drive to scoring (X button)
- Shoot sequence (RIGHT BUMPER)
- Return and reload
- Goal: Complete cycle smoothly

### Drill 4: Field-Relative Driving
- Start facing sideways
- Push stick toward audience - robot should move there
- Push stick toward driver station - robot should move there
- Practice until intuitive

---

## TROUBLESHOOTING

| Problem | Solution |
|---------|----------|
| Robot not moving | Check that OpMode is running, motors are enabled |
| Driving feels backwards | Press A to reset heading |
| Can't auto-drive to score | Alliance must be detected first |
| Indexer not loading | Camera might not see artifacts - check lighting |
| Colors detected wrong | Tune HSV ranges in Limelight pipelines |
| Shooter not firing | Check that RIGHT BUMPER is held continuously |
| Camera stuck in wrong position | Press A on gamepad2 to reset |
| Position tracking drifting | Drive near AprilTags for automatic correction |

---

## PRE-MATCH CHECKLIST

### Before Autonomous:
- [ ] Robot positioned correctly on field
- [ ] All servos at home positions
- [ ] Camera pointing up (APRILTAG_VIEW)
- [ ] Indexer empty (all slots clear)
- [ ] IMU calibrated
- [ ] Limelight connected and streaming
- [ ] Controllers paired and working

### Before TeleOp:
- [ ] Check alliance was detected (Red/Blue)
- [ ] Check pattern was detected (GPP/PGP/PPG)
- [ ] Check robot position on field
- [ ] Indexer empty and ready to load
- [ ] Drivers understand controls

---

## TIPS FOR SUCCESS

1. **Practice the bumper holds** - Left for intake, Right for shooting
2. **Use auto-drive** (X button) when possible - more accurate than manual
3. **Feed artifacts slowly** - gives camera time to detect colors
4. **Trust the automation** - once you hold bumper, let it work
5. **Watch telemetry** - tells you exactly what's happening
6. **Field-relative is your friend** - easier to drive when you think about field directions
7. **Don't panic if colors wrong** - robot shoots best order automatically
8. **Camera is automatic** - you never need to think about it

---

## BUTTON SUMMARY CHART

```
╔══════════════════════════════════════════════════════════════╗
║                    GAMEPAD 1 - DRIVER                        ║
╠══════════════════════════════════════════════════════════════╣
║  Left Stick       │ Forward/Back & Strafe (Field-Relative)   ║
║  Right Stick      │ Rotate/Turn                              ║
║  Left Bumper      │ HOLD for Robot-Relative Drive            ║
║  A Button         │ Reset IMU Heading                        ║
║  X Button         │ Auto-Drive to Scoring Position           ║
╚══════════════════════════════════════════════════════════════╝

╔══════════════════════════════════════════════════════════════╗
║                   GAMEPAD 2 - OPERATOR                       ║
╠══════════════════════════════════════════════════════════════╣
║  Left Bumper      │ HOLD TO INTAKE (Fully Automatic)         ║
║  Right Bumper     │ HOLD TO SHOOT (Fully Automatic)          ║
║  Y Button         │ Clear All Indexer Slots (Emergency)      ║
║  A Button         │ Reset Indexer to Home                    ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Remember: HOLD THE BUMPERS!

**Most important rule: Hold LEFT BUMPER for entire intake, hold RIGHT BUMPER for entire shooting sequence. Don't tap - HOLD!**

The robot does everything else automatically. Trust the automation!