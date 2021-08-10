package se.bitcraze.crazyfliecontrol2;

import android.os.Build;
import android.support.annotation.RequiresApi;

public class AutonomousFlightController implements Runnable {

    MainActivity mainActivity;
    private boolean run = false;


    AutonomousFlightController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        this.run = true;
        this.mainActivity.getPidController().takeOff(this.mainActivity);
        while(!Thread.interrupted() && this.run) {
             this.mainActivity.getPidController().sendSpeedData(this.mainActivity);
        }
        this.run = false;
        this.mainActivity.getPidController().reset();
    }
}
