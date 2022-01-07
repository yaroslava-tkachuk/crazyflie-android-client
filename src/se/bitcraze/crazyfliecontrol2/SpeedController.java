package se.bitcraze.crazyfliecontrol2;

import android.graphics.ImageDecoder;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;

import static java.lang.Math.abs;

public class SpeedController {

    // thrust: 0 ... 52000
    // pitch: -20 ... 20
    // roll: -20 ... 20

    private double clip(double minVal, double maxVal, double val) {
        if (val > maxVal) {
            val = maxVal;
        } else if (val < minVal) {
            val = minVal;
        }

        return val;
    }

    private void sendSpeed(MainActivity mainActivity, float roll, float pitch, float yaw, float thrust, int time) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < time) {
            mainActivity.getPresenter().sendPacket(new CommanderPacket(roll, pitch, yaw, (char) thrust, false));
        }
    }

    public void takeOff(MainActivity mainActivity) {
        this.sendSpeed(mainActivity, -1, 0, 0, 47000, 700);
        this.sendSpeed(mainActivity, -1, 0, 0, 45000, 700);
    }

    public void land(MainActivity mainActivity) {
        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 45000, 400);
        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 30000, 800);
        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 15000, 800);
        this.sendSpeed(mainActivity, -1, (float)0.5, 0, 5000, 400);
    }

    public void goToFace(Mat imageMat, Rect detectedFace, MainActivity mainActivity) {
        // max side speed for the test area: -4 ... 3, 500 ms

        this.takeOff(mainActivity);
        // Calculate turning speed
        double centerX = detectedFace.br().x - detectedFace.size().width / 2;
        double errorRoll = imageMat.size().width / 2 - centerX;
        double turningTime = 500 * abs(errorRoll) / 162;
        // Turn
        if(errorRoll > 30) {
            this.sendSpeed(mainActivity, -4, (float)0.5, 0, 45000, (int)turningTime);
        }
        else if(errorRoll < -30) {
            this.sendSpeed(mainActivity, 4, (float)0.5, 0, 45000, (int)turningTime);
        }
        // Go straight
        this.sendSpeed(mainActivity, 0, 5, 0, 45000, 600);
        this.land(mainActivity);
        mainActivity.setAutonomousFlightEnabled(false);
    }

    public void goToCircle(Mat imageMat, double[] detectedCircle, MainActivity mainActivity) {
        // max side speed for the test area: -4 ... 3, 500 ms

        this.takeOff(mainActivity);
        // Calculate turning speed
        double circleX = detectedCircle[0];
        double errorRoll = imageMat.size().width / 2 - circleX;
        double turningTime = 500 * abs(errorRoll) / 162;
        // Turn
        if(errorRoll > 30) {
            this.sendSpeed(mainActivity, -4, (float)0.5, 0, 45000, (int)turningTime);
        }
        else if(errorRoll < -30) {
            this.sendSpeed(mainActivity, 4, (float)0.5, 0, 45000, (int)turningTime);
        }
        // Go straight
        this.sendSpeed(mainActivity, 0, 5, 0, 45000, 700);
        this.land(mainActivity);
        mainActivity.setAutonomousFlightEnabled(false);
    }
}
