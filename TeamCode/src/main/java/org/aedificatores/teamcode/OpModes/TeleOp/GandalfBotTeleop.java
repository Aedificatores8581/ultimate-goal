package org.aedificatores.teamcode.OpModes.TeleOp;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.aedificatores.teamcode.Mechanisms.Components.GandalfIntake.GandalfIntakeLift;
import org.aedificatores.teamcode.Mechanisms.Components.GandalfIntake.GandalfTransfer.TransferPriority;
import org.aedificatores.teamcode.Mechanisms.Robots.GandalfBot;
import org.aedificatores.teamcode.Universal.OpModeGroups;

@TeleOp(group = OpModeGroups.GANDALF)
public class GandalfBotTeleop extends OpMode {
    GandalfBot bot;

    Gamepad prev1 = new Gamepad(), prev2 = new Gamepad();

    enum DriveMode {
        TRIGGER_BASED,
        STICK_BASED,
    }

    enum ScoringMode {
        DRIVER_CONTROLLED,
        POWERSHOT_AUTOMATION
    }

    ScoringMode scoringMode = ScoringMode.DRIVER_CONTROLLED;

    public static final double SHOOTER_SPEED = 239.6;
    public static final double POWERSHOT_SPEED = 210;
    public static final double INTAKE_UP_ANGLE = Math.toRadians(100);
    public static final double INTAKE_DOWN_ANGLE = Math.toRadians(6);

    final Pose2d START_POSE = new Pose2d(0 - 17.5/2.0, -(72 - 18.0/2.0), 0);
    final Vector2d[] SHOT_POSITIONS = { new Vector2d(-3 - 17.5/2.0, -18),
                                        new Vector2d(-2 - 17.5/2.0, -12),
                                        new Vector2d(-2 - 17.5/2.0, -6)};
    Trajectory trajShoot;
    int currentShotCounter = 0;

    DriveMode driveMode;

    @Override
    public void init() {
        bot = new GandalfBot(hardwareMap, false);
        bot.intake.lift.setMode(GandalfIntakeLift.Mode.AUTO);
        driveMode = DriveMode.TRIGGER_BASED;
    }

    @Override
    public void init_loop() {
        if (gamepad1.a) {
            driveMode = DriveMode.TRIGGER_BASED;
        }
        if (gamepad1.b) {
            driveMode = DriveMode.STICK_BASED;
        }

        telemetry.addLine("Press A for Trigger based Driving (i.e. Gwen Mode)");
        telemetry.addLine("Press B for Stick based Driving (i.e. Non-Gwen Mode)");
        telemetry.addData("Current Mode: ", driveMode);
    }

    public void start() {
        try {
            prev1.copy(gamepad1);
            prev2.copy(gamepad2);
        } catch (RobotCoreException e) {
            telemetry.addLine("Tried to Copy gamepad.");
            telemetry.addLine(e.getMessage());
            requestOpModeStop();
        }
        trajShoot = bot.drivetrain.trajectoryBuilder(START_POSE)
                .splineToConstantHeading(SHOT_POSITIONS[0], Math.PI/2)
                .addDisplacementMarker(() -> bot.forceTransfer())
                .addDisplacementMarker(() -> bot.intake.forward())
                .build();

        bot.shooter.setSpeed(SHOOTER_SPEED);
    }

    @Override
    public void loop() {
        if (scoringMode == ScoringMode.DRIVER_CONTROLLED) {
            telemetry.addLine("GAMEPAD 1:");
            telemetry.addLine("----------------------");
            if (driveMode == DriveMode.TRIGGER_BASED) {
                bot.drivetrain.setWeightedDrivePower(
                        new Pose2d(
                                -(gamepad1.left_trigger - gamepad1.right_trigger),
                                -gamepad1.left_stick_x,
                                -gamepad1.right_stick_x
                        )
                );
                telemetry.addLine("Left and right trigger for moving back/foreward");
                telemetry.addLine("left and right stick for strafe/turn");
            } else {
                bot.drivetrain.setWeightedDrivePower(
                        new Pose2d(
                                -gamepad1.left_stick_y,
                                -gamepad1.left_stick_x,
                                -gamepad1.right_stick_x
                        )
                );
                telemetry.addLine("left and right stick for omnidrectional movement/turn");
            }

            if (gamepad1.a) {
                // bot.intake.transfer.setPower(.75);
                bot.intake.transfer.queueSetPower(.75, TransferPriority.SHOOT_RING);
            }

            if (gamepad1.left_bumper && !prev1.left_bumper) {
                bot.intake.toggleOuttake();
            } else if (gamepad1.right_bumper && !prev1.right_bumper) {
                bot.intake.toggleIntake();
            }

            if (gamepad1.x && !prev1.x) {
                bot.wobbleGrabber.toggleGrabber();
            }

            try {
                prev1.copy(gamepad1);
            } catch (RobotCoreException e) {
                telemetry.addLine("Tried to Copy gamepad 1.");
                telemetry.addLine(e.getMessage());
                requestOpModeStop();
            }

            bot.wobbleGrabber.setPower(gamepad2.left_stick_y);

            if (gamepad2.a) {
                bot.intake.transfer.queueSetPower(.75, TransferPriority.SHOOT_RING);
            }

            if (gamepad2.dpad_left) {
                bot.intake.transfer.queueSetPower(-.75, TransferPriority.EMERGENCY);
            } else if (gamepad2.dpad_right) {
                bot.intake.transfer.queueSetPower(.75, TransferPriority.EMERGENCY);
            }

            if (gamepad2.dpad_up && !prev2.dpad_up) {
                bot.intake.lift.gotoAngle(INTAKE_UP_ANGLE);
            } else if(gamepad2.dpad_down && !prev2.dpad_down) {
                bot.intake.lift.gotoAngle(INTAKE_DOWN_ANGLE);
            }

            if (gamepad2.x && !prev2.x) {
                bot.wobbleGrabber.toggleGrabber();
            }

            if (gamepad2.b && !prev2.b) {
                bot.shootRings(SHOOTER_SPEED, 3);
            }

            if (gamepad2.y && !prev2.y) {
                bot.drivetrain.setPoseEstimate(START_POSE);
                trajShoot = bot.drivetrain.trajectoryBuilder(START_POSE)
                        .splineToConstantHeading(SHOT_POSITIONS[currentShotCounter], Math.PI/2)
                        .addDisplacementMarker(() -> bot.forceTransfer())
                        .addDisplacementMarker(() -> bot.intake.forward())
                        .build();
                bot.drivetrain.followTrajectoryAsync(trajShoot);
                bot.stopforceTransfer();
                scoringMode = ScoringMode.POWERSHOT_AUTOMATION;
                bot.shooter.setSpeed(POWERSHOT_SPEED);
            }

            try {
                prev2.copy(gamepad2);
            } catch (RobotCoreException e) {
                telemetry.addLine("Tried to Copy gamepad 2.");
                telemetry.addLine(e.getMessage());
                requestOpModeStop();
            }

            telemetry.addLine("Hold 'A' to force transfer to go on");
            telemetry.addLine("'X' to open/close wobble grabber");
            telemetry.addLine("Left and right bumper for outtake/intake");
            telemetry.addLine("\nGAMEPAD 2:");
            telemetry.addLine("----------------------");
            telemetry.addLine("Right stick for wobble grabber position");
            telemetry.addLine("'X' to open/close wobble grabber");
            telemetry.addLine("'B' to shoot 3 rings thru shooter automation");
            telemetry.addLine("dpad up/down to control intake angle");
            telemetry.addLine("dpad left/right to control transfer");
        } else {
            if (gamepad2.y && !prev2.y) {
                ++currentShotCounter;
                if (currentShotCounter > 2) {
                    currentShotCounter = 0;
                    bot.stopforceTransfer();
                    bot.drivetrain.setIdle();
                    scoringMode = ScoringMode.DRIVER_CONTROLLED;
                    bot.shooter.setSpeed(SHOOTER_SPEED);
                } else {
                    trajShoot = bot.drivetrain.trajectoryBuilder(bot.drivetrain.getPoseEstimate())
                            .splineToConstantHeading(SHOT_POSITIONS[currentShotCounter], Math.PI/2)
                            .addDisplacementMarker(() -> bot.forceTransfer())
                            .addDisplacementMarker(() -> bot.intake.forward())
                            .build();
                    bot.drivetrain.followTrajectoryAsync(trajShoot);
                    bot.stopforceTransfer();
                }
            }

            if (gamepad2.b && !prev2.b) {
                currentShotCounter = 0;
                bot.stopforceTransfer();
                bot.drivetrain.setIdle();
                scoringMode = ScoringMode.DRIVER_CONTROLLED;
                bot.shooter.setSpeed(SHOOTER_SPEED);
            }

            try {
                prev2.copy(gamepad2);
            } catch (RobotCoreException e) {
                telemetry.addLine("Tried to Copy gamepad 2.");
                telemetry.addLine(e.getMessage());
                requestOpModeStop();
            }
            telemetry.addData("current shot", currentShotCounter);
            telemetry.addLine("POWER SHOT MODE (Gamepad 2 only)");
            telemetry.addLine("----------------------");
            telemetry.addLine("Press 'Y' to advance to the next shot");
            telemetry.addLine("Press 'B' to Cancel ");

        }
        bot.update();
    }
}
