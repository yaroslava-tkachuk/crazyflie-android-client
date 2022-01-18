/* Copyright 2022 Yaroslava Tkachuk. All rights reserved. */

package se.bitcraze.crazyfliecontrol2;
import android.os.Build;
import android.support.annotation.RequiresApi;


public class AutonomousFlightController implements Runnable {

    /* Class responsible for handling autonomous way-point flight of Crazyflie UAV. */

    //----------------------------------------------------------------------------------------------
    // Attributes
    //----------------------------------------------------------------------------------------------

    private MainActivity mainActivity;
    private boolean run = false;

    //----------------------------------------------------------------------------------------------
    // End Attributes
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------------------

    AutonomousFlightController(MainActivity mainActivity) {

        /* AutonomousFlightController class constructor.
         *
         * IN:
         * mainActivity - MainActivity - MainActivity instance. */

        this.mainActivity = mainActivity;
    }

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {

        /* Method for handling autonomous way-point flight logic. */

        this.run = true;
        this.mainActivity.getSpeedController().takeOff(this.mainActivity);
        while(!Thread.interrupted() && this.run) {
//             this.mainActivity.getSpeedController().sendSpeedData(this.mainActivity);
             this.run = false;
        }
        //this.mainActivity.getPidController().land(this.mainActivity);
        this.run = false;
//        this.mainActivity.getSpeedController().reset();
        this.mainActivity.setAutonomousFlightEnabled(false);
    }

    //----------------------------------------------------------------------------------------------
    // End Methods
    //----------------------------------------------------------------------------------------------
}
