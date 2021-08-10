package se.bitcraze.crazyfliecontrol2;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;

public class PidController {

    // PID coefficients
    double KpThrust = 6;
    double KiThrust = 0.0001;
    double KdThrust = 0.001;

    double KpRoll = 0.005;
    double KiRoll = 0.00001;
    double KdRoll = 0.0001;

    // PID data
    double prevErrorThrust = 0;
    double iThrust = 0;

    double prevErrorRoll = 0;
    double iRoll = 0;

    double prevErrorPitch = 0;
    double iPitch = 0;

    double prevTime = 0;

    // Drone velocities
    float thrust = 0;
    float roll = 0;
    float pitch = 0;

    public void setTime(double newTime) {
        this.prevTime = newTime;
    }

    private double clip(double minVal, double maxVal, double val) {
        if (val > maxVal) {
            val = maxVal;
        } else if (val < minVal) {
            val = minVal;
        }

        return val;
    }

//public void processData(Mat imageMat, Rect detectedFace) {
//    if(detectedFace != null) {
//            double time = System.currentTimeMillis();
//            double centerY = detectedFace.br().y - detectedFace.size().height / 2;
//            double centerX = detectedFace.br().x - detectedFace.size().width / 2;
//
//            // Calculate errors
//            double errorThrust = imageMat.size().height / 2 - centerY;
//            double errorRoll = imageMat.size().width / 2 - centerX;
//
//            // TESTING
//            if(errorRoll > 30){
//                this.roll = (float) 7;
//                this.thrust = 46000;
//            }
//            else if(errorRoll < -30) {
//                this.roll = (float) -7;
//                this.thrust = 46000;
//            }
//
////            // Calculate PID components
////            double pThrust = this.KpThrust * errorThrust;
////            this.iThrust = this.iThrust + this.KiThrust * errorThrust * (time - this.prevTime);
////            double dThrust = this.KdThrust * (errorThrust - this.prevErrorThrust) / (time - this.prevTime);
////
////            double pRoll = this.KpRoll * errorRoll;
////            this.iRoll = this.iRoll + this.KiRoll * errorRoll * (time - this.prevTime);
////            double dRoll = this.KdRoll * (errorRoll - this.prevErrorRoll) / (time - this.prevTime);
////
////            // Calculate drone velocities
////            this.thrust = (float) this.clip(0, 52000, this.thrust + pThrust + this.iThrust + dThrust);
////            this.roll = (float) this.clip(-20, 20, this.roll + pRoll + this.iRoll + dRoll);
////
////            // Update data
////            this.prevErrorThrust = errorThrust;
////            this.prevErrorRoll = errorRoll;
////            this.prevTime = time;
//
////            Log.d("LOG", "Thrust: " + this.thrust);
////            Log.d("LOG", "errorThrust: " + errorThrust);
////            Log.d("LOG", "P: " + pThrust + " I: " + iThrust + " D: " + dThrust);
////            Log.d("LOG", "centerY: " + centerY);
//            Log.d("LOG", "Roll: " + this.roll);
////            Log.d("LOG", "errorRoll: " + errorRoll);
////            Log.d("LOG", "P: " + pRoll + " I: " + iRoll + " D: " + dRoll);
//            Log.d("LOG", "center: " + imageMat.size().width / 2 + "centerY: " + centerX);
//        }
//    }

    public void processData(Mat imageMat, double[] detectedCircle) {
        if(detectedCircle != null) {
            double circleX = detectedCircle[0];
            double circleY = detectedCircle[1];

            // Calculate errors
            double errorRoll = imageMat.size().width / 2 - circleX;
            double errorThrust = imageMat.size().height / 2 - circleY;

            if(errorRoll > 30) {
                this.roll = -8;
            }
            else if(errorRoll < -30) {
                this.roll = 8;
            }

            Log.d("LOG", "Roll: " + this.roll);
            Log.d("LOG", "center: " + imageMat.size().width / 2 + "circleX: " + circleX);
        }
    }

    public void sendSpeedData(MainActivity mainActivity) {
        if(this.roll != 0) {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 400) {
                mainActivity.getPresenter().sendPacket(new CommanderPacket(this.roll, 0, 0, (char) this.thrust, false));
            }
            startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 400) {
                mainActivity.getPresenter().sendPacket(new CommanderPacket(-1*this.roll, 0, 0, (char) this.thrust, false));
            }
            this.roll = 0;
            this.thrust = 45000;
        }
        else {
            mainActivity.getPresenter().sendPacket(new CommanderPacket(this.roll, 0, 0, (char) this.thrust, false));
        }
        Log.d("LOG", "SENDING roll: " + this.roll + " pitch: " + this.pitch + " thrust: " + this.thrust);
    }

    public void takeOff(MainActivity mainActivity) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 500) {
            mainActivity.getPresenter().sendPacket(new CommanderPacket(0, 0, 0, (char) 45000, false));
        }

        this.thrust = 45000;
    }

    public void reset(){
        this.thrust = 0;
        this.roll = 0;
        this.pitch = 0;
        this.prevErrorThrust = 0;
        this.iThrust = 0;
        this.prevErrorRoll = 0;
        this.iRoll = 0;
        this.prevErrorPitch = 0;
        this.iPitch = 0;
    }
}
