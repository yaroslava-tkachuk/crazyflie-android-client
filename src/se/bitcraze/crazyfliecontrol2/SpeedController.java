/* Copyright 2022 Yaroslava Tkachuk. All rights reserved. */

package se.bitcraze.crazyfliecontrol2;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import static java.lang.Math.abs;


public class SpeedController {

    /* Class for controlling an autonomous way-point flight of Crazyflie UAV.
     *
     * Responsible for:
     *   - control commands calculation;
     *   - take off;
     *   - moving along Y axis;
     *   - intelligent flight along X axis;
     *   - landing. */

    // Speed values ranges:
    // thrust: 0 ... 52000
    // pitch: -20 ... 20
    // roll: -20 ... 20

    //----------------------------------------------------------------------------------------------
    // Attributes
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // End Attributes
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // End Constructors
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Getters
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // End Getters
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Setters
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // End Setters
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Methods
    //----------------------------------------------------------------------------------------------

    private double clip(double minVal, double maxVal, double val) {

        /* Method for keeping a numeric value in a given range.
        *
        * IN:
        * minVal - double - minimum range value;
        * maxVal -  double - maximum range value;
        * val - double - value to be clipped.
        *
        * OUT:
        * val - double - value in range [minVal, maxVal]. */

        if (val > maxVal) {
            val = maxVal;
        } else if (val < minVal) {
            val = minVal;
        }

        return val;
    }

    private void sendSpeed(MainActivity mainActivity, float roll, float pitch, float yaw,
                           float thrust, int time) {

        /* Method for sending speed value to Crazyflie using MainPresenter.
         *
         * IN:
         * mainActivity - MainActivity - MainActivity instance;
         * roll - float - roll velocity value;
         * pitch - float - pitch velocity value;
         * yaw - float - yaw velocity value;
         * thrust - float - thrust velocity value;
         * time - int - time during which given velocities will be sent to the UAV [ms]. */

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < time) {
            mainActivity.getPresenter().sendPacket(new CommanderPacket(roll, pitch, yaw,
                    (char) thrust, false));
        }
    }

    public void takeOff(MainActivity mainActivity) {

        /* Method for performing Crazyflie takeoff. */

        this.sendSpeed(mainActivity, -1, 0, 0, 47000, 700);
        this.sendSpeed(mainActivity, -1, 0, 0, 45000, 700);
    }

    public void land(MainActivity mainActivity) {

        /* Method for performing Crazyflie landing. */

        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 45000, 400);
        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 30000, 800);
        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 15000, 800);
        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 5000, 400);
    }

    public void goToFace(Mat imageMat, Rect detectedFace, MainActivity mainActivity) {

        /* Method for performing "go-to-face" Crazyflie way-point flight.
         *
         * Uses face detection results to calculate flight command for X axis.
         * IN:
         * imageMat - Mat - current frame from the UAV on-board camera;
         * detectedFace - Rect - face's bounding box;
         * mainActivity - MainActivity - MainActivity instance. */

        // Maximum roll velocity for the test field area: -4 ... 4, 500 ms.

        this.takeOff(mainActivity);
        // Calculate turning speed.
        double centerX = detectedFace.br().x - detectedFace.size().width / 2;
        double errorRoll = imageMat.size().width / 2 - centerX;
        double turningTime = 500 * abs(errorRoll) / 162;
        // Turn.
        if(errorRoll > 30) {
            this.sendSpeed(mainActivity, -4, (float)0.5, 0, 45000, (int)turningTime);
        }
        else if(errorRoll < -30) {
            this.sendSpeed(mainActivity, 4, (float)0.5, 0, 45000, (int)turningTime);
        }
        // Go straight.
        this.sendSpeed(mainActivity, 0, 5, 0, 45000, 600);
        this.land(mainActivity);
        mainActivity.setAutonomousFlightEnabled(false);
    }

    public void goToCircle(Mat imageMat, double[] detectedCircle, MainActivity mainActivity) {

        /* Method for performing "go-to-circle" Crazyflie way-point flight.
         *
         * Uses circle detection results to calculate flight command for X axis.
         * IN:
         * imageMat - Mat - current frame from the UAV on-board camera;
         * detectedCircle - double[] - circle's bounding box;
         * mainActivity - MainActivity - MainActivity instance. */

        // Maximum roll velocity for the test field area: -4 ... 4, 500 ms.

        this.takeOff(mainActivity);
        // Calculate turning speed.
        double circleX = detectedCircle[0];
        double errorRoll = imageMat.size().width / 2 - circleX;
        double turningTime = 500 * abs(errorRoll) / 162;
        // Turn.
        if(errorRoll > 30) {
            this.sendSpeed(mainActivity, -4, (float)0.5, 0, 45000, (int)turningTime);
        }
        else if(errorRoll < -30) {
            this.sendSpeed(mainActivity, 4, (float)0.5, 0, 45000, (int)turningTime);
        }
        // Go straight.
        this.sendSpeed(mainActivity, 0, 5, 0, 45000, 700);
        this.land(mainActivity);
        mainActivity.setAutonomousFlightEnabled(false);
    }

    //----------------------------------------------------------------------------------------------
    // End Methods
    //----------------------------------------------------------------------------------------------
}
