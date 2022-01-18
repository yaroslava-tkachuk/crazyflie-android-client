/* Copyright 2022 Yaroslava Tkachuk. All rights reserved. */

package se.bitcraze.crazyfliecontrol2;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;


public class CameraStreamController implements Runnable {

    /* Class for receiving and formatting video stream from Crazyflie UAV camera.
     *
     * Responsible for:
     *   - receiving JPEG image frames over WiFi;
     *   - image formatting;
     *   - image rendering in Crazyflie Android Client UI. */

    //----------------------------------------------------------------------------------------------
    // Attributes
    //----------------------------------------------------------------------------------------------

    Socket socket;
    private int port = 5000;
    private String ipAddress = "192.168.4.1";
    private int imageSizeBytes = 512;
    private byte frameStart[] = {(byte) 0xff, (byte) 0xd8};
    private byte frameEnd[] = {(byte) 0xff, (byte) 0xd9};
    MainActivity mainActivity;
    private boolean run = false;
    private long startTime = 0;

    //----------------------------------------------------------------------------------------------
    // End Attributes
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------------------

    CameraStreamController(MainActivity mainActivity) {

        /* CameraStreamController class constructor.
         *
         * IN:
         * mainActivity - MainActivity - MainActivity instance. */

        this.mainActivity = mainActivity;
        OpenCVLoader.initDebug();
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

    public static int findFrameSequence(byte[] buffer, byte[] searchSequence) {

        /* Method for finding given byte sequence in a byte array.
         *
         * IN:
         * buffer - byte[] - byte array to be processed;
         * searchSequence - byte[] - sequence to be searched.
         *
         * OUT:
         * i - int - first searchSequence's occurence index (if was encountered);
         * -1 - int - if buffer does not contained the searchSequence. */

        if(searchSequence.length != 2) {
            return -1;
        }

        for(int i = 0; i < buffer.length - 1; i++) {
            if( (buffer[i] == searchSequence[0]) && (buffer[i+1] == searchSequence[1]) ) {
                return i;
            }
        }

        return -1;
    }

    public static byte[] concatenateBuffers(byte[] buffer0, byte[] buffer1, int buffer1Stop) {

        /* Method for concatenating two byte arrays.
         *
         * IN:
         * buffer0 - byte[] - first byte array to be processed;
         * buffer1 - byte[] - second byte array to be processed;
         * buffer1Stop - int - index of the last element in the second byte array which should be
         * copied.
         *
         * OUT:
         * result - byte[] - byte array containing first and second byte arrays. */

        byte[] result = new byte[buffer0.length + buffer1Stop];
        System.arraycopy(buffer0, 0, result, 0, buffer0.length);
        System.arraycopy(buffer1, 0, result, buffer0.length, buffer1Stop);

        return result;
    }

    private Bitmap processImage(byte[] frame) {

        /* Method for formatting and processing image frame from UAV's camera.
         *
         * IN:
         * frame - byte[] - byte array containing image frame.
         *
         * OUT:
         * imageWithFace - Bitmap - formatted image with face detection results. */

        // Create a Bitmap from byte array.
        Bitmap imageBmp = BitmapFactory.decodeByteArray(frame, 0, frame.length);

        // Convert Bitmap into Mat.
        Mat imageMat = new Mat(imageBmp.getHeight(), imageBmp.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(imageBmp, imageMat);

        // Detect and draw face.
        imageMat = this.mainActivity.getFaceDetector().detectFace(imageMat);

        // Process face detection results and invoke "go-to-face" functionality.
        Rect detectedFace = this.mainActivity.getFaceDetector().getFace();
        if(this.mainActivity.getAutonomousFlightEnabled() && (detectedFace != null)) {
            this.mainActivity.getSpeedController().goToFace(imageMat, detectedFace,
                    mainActivity);
        }

//        // Detect and draw circle.
//        imageMat = this.mainActivity.getFaceDetector().detectCircle(imageMat);

//        // Process circle detection results and invoke "go-to-circle" functionality.
//        double[] detectedCircle = this.mainActivity.getFaceDetector().getCircle();
//        if(this.mainActivity.getAutonomousFlightEnabled() && (detectedCircle != null)) {
//            this.mainActivity.getSpeedController().goToCircle(imageMat, detectedCircle,
//                    this.mainActivity);
//        }

        // Convert Mat to Bitmap.
        Bitmap imageWithFace = imageBmp;
        Utils.matToBitmap(imageMat, imageWithFace);

        return imageWithFace;
    }

    private void renderImage(final Bitmap imageToRender) {

        /* Method for rendering image with face detection results in application's UI.
         *
         * IN:
         * imageToRender - Bitmap - image to be rendered. */

        final ImageView cameraImageView = this.mainActivity.getCameraImageView();

        this.mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    cameraImageView.setImageBitmap(Bitmap.createScaledBitmap(imageToRender,
                            cameraImageView.getWidth(), cameraImageView.getHeight(), false));
                }
                catch(Exception e) {
                    Log.e("Image rendering error", "Huston, we have a problem: ", e);
                }
            }
        });
    }

    private void handleConnectionError(Exception e) {

        /* Method for handling connection errors.
         *
         * IN:
         * e - Exception - error to be handled. */

        // Log error message.
        Log.e("TCP connection error", "Huston, we have problem: ", e);
        this.mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mainActivity.getApplicationContext(),
                        "Lost connection with the camera.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set flags to false.
        this.mainActivity.setCameraStreamEnabled(false);
        try {
            if(this.socket != null){
                this.socket.close();
            }
        } catch (IOException ioException) {
            Log.e("Socket closing error", "Huston, we have problem: ", e);
        }
        this.run = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {

        /* Method for handling image receiving and processing logic. */

        this.run = true;
        this.startTime = System.currentTimeMillis();
        byte[] imageBuffer = new byte[0];
        byte[] buffer = new byte[this.imageSizeBytes];
        byte[] frame;
        int bytesRead;

        try {
            this.socket = new Socket(this.ipAddress, this.port);
            this.socket.setSoTimeout(3000);
            DataInputStream inputStream =
                    new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
            while (!Thread.interrupted() && this.run) {

                // Read data from socket.
                bytesRead = inputStream.read(buffer, 0, this.imageSizeBytes);
                imageBuffer = concatenateBuffers(imageBuffer, buffer, bytesRead);

                // Find start and end of JPEG image frame markers.
                int startFrameIdx = findFrameSequence(imageBuffer, this.frameStart);
                int endFrameIdx = findFrameSequence(imageBuffer, this.frameEnd);

                // If end of frame marker precedes start of frame marker, cut off the buffer before
                // start of frame marker.
                if( (endFrameIdx > -1) && (startFrameIdx > -1) && (endFrameIdx < startFrameIdx) ) {
                    imageBuffer = Arrays.copyOfRange(imageBuffer, startFrameIdx, imageBuffer.length);
                }

                // If buffer contains both start and end of frame markers, and start of frame marker
                // precedes end of frame marker, prepare and render the image.
                if( (startFrameIdx > -1) && (endFrameIdx > -1) && (endFrameIdx > startFrameIdx) ) {
                    frame = Arrays.copyOfRange(imageBuffer, startFrameIdx, endFrameIdx+2);
                    imageBuffer = Arrays.copyOfRange(imageBuffer, endFrameIdx+2,
                            imageBuffer.length);

                    // Detect face and render image every 100 ms.
                    if(System.currentTimeMillis() - startTime >=  100){
                        final Bitmap imageToRender = this.processImage(frame);
                        this.renderImage(imageToRender);
                        this.startTime = System.currentTimeMillis();
                    }
                }
            }
        } catch (Exception e) {
            this.handleConnectionError(e);
        }
    }

    //----------------------------------------------------------------------------------------------
    // End Methods
    //----------------------------------------------------------------------------------------------
}
