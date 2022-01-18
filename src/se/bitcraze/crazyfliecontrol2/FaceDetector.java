/* Copyright 2022 Yaroslava Tkachuk. All rights reserved. */

package se.bitcraze.crazyfliecontrol2;
import android.content.Context;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.opencv.core.Core.flip;


public class FaceDetector {

    /* Class for real-time face detection in Crazyflie UAV's camera video stream.
     *
     * Uses Haar Cascade classifier from OpenCV library. */

    //----------------------------------------------------------------------------------------------
    // Attributes
    //----------------------------------------------------------------------------------------------

    private CascadeClassifier frontalFacesClassifier;
    private CascadeClassifier profileFacesClassifier;
    private CascadeClassifier eyesClassifier;
    private Scalar green = new Scalar(49, 195, 39);
    private Scalar red = new Scalar(158, 0, 0);
    private int thickness = 3;
    private MainActivity mainActivity;
    private Rect face;
    double[] circle; // (x, y), radius
    private boolean frontalFaceDetected = false;
    private boolean leftProfileFaceDetected = false;
    private boolean rightProfileFaceDetected = false;
    private boolean eyesDetected = false;

    //----------------------------------------------------------------------------------------------
    // End Attributes
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------------------

    public FaceDetector(MainActivity mainActivity) throws IOException {

        /* FaceDetector class constructor.
         *
         * IN:
         * mainActivity - MainActivity - MainActivity instance. */

        this.mainActivity = mainActivity;
        OpenCVLoader.initDebug();
        this.frontalFacesClassifier = new CascadeClassifier(
                this.loadClassifierFile(R.raw.haarcascade_frontalface_alt2,
                        "haarcascade_frontalface_alt2.xml").getAbsolutePath());
        this.profileFacesClassifier = new CascadeClassifier(
                this.loadClassifierFile(R.raw.haarcascade_profileface,
                        "haarcascade_profileface.xml").getAbsolutePath());
        this.eyesClassifier = new CascadeClassifier(
                this.loadClassifierFile(R.raw.haarcascade_eye,
                        "haarcascade_eye.xml").getAbsolutePath());
    }

    //----------------------------------------------------------------------------------------------
    // End Constructors
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Getters
    //----------------------------------------------------------------------------------------------

    public boolean getFrontalFaceDetected() {
        return this.frontalFaceDetected;
    }

    public boolean getLeftProfileFaceDetected() {
        return this.leftProfileFaceDetected;
    }

    public boolean getRightProfileFaceDetected() {
        return this.rightProfileFaceDetected;
    }

    public boolean getEyesDetected() {
        return this.eyesDetected;
    }

    public Rect getFace() {
        return this.face;
    }

    public double[] getCircle() {
        return this.circle;
    }

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

    private File loadClassifierFile(int resourceId, String fileName) throws IOException {

        /* Method for loading OpenCV classifier files.
         *
         * IN:
         * resourceId - int - ID of the file to be loaded;
         * fileName - String - name of the file to be loaded.
         *
         * OUT:
         * cascadeFile - File - loaded classifier file. */

        // Copy resource into a file, so OpenCV can load it.
        InputStream inputStream = this.mainActivity.getResources().openRawResource(resourceId);
        File cascadeDir = this.mainActivity.getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(cascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }
        // Close streams.
        inputStream.close();
        fileOutputStream.close();

        return cascadeFile;
    }

    private void drawImageCenter(Mat image) {

        /* Method for marking image's central point.
         *
         * IN:
         * image - Mat - image whose central point will be marked. */

        // Draw image center.
        Point imageCenter = new Point(Math.round(image.size().width/2),
                Math.round(image.size().height/2));
        Imgproc.circle(image, imageCenter, 1, this.red, 3, 8, 0);
    }

    public Mat detectFrontalFace(Mat imageMat) {

        /* Method for detecting frontal face using Haar Cascade classifier.
         *
         * IN:
         * imageMat - Mat - image to be analyzed.
         *
         * OUT:
         * imageMat - Mat - image with face detection results. */

        MatOfRect frontalFaces = new MatOfRect();
        // Detect face.
        this.frontalFacesClassifier.detectMultiScale(imageMat, frontalFaces);

        if (!frontalFaces.empty()) {
            Rect frontalFace = frontalFaces.toArray()[0];
            // Draw face's bounding box.
            Imgproc.rectangle(imageMat, frontalFace.tl(), frontalFace.br(), this.green,
                    this.thickness);
            this.frontalFaceDetected = true;
            this.face = frontalFace;
        }

        return imageMat;
    }

    public Mat detectLeftProfileFace(Mat imageMat) {

        /* Method for detecting left profile face using Haar Cascade classifier.
         *
         * IN:
         * imageMat - Mat - image to be analyzed.
         *
         * OUT:
         * imageMat - Mat - image with face detection results. */

        MatOfRect leftProfileFaces = new MatOfRect();
        // Detect face.
        this.profileFacesClassifier.detectMultiScale(imageMat, leftProfileFaces);

        if(!leftProfileFaces.empty()) {
            Rect leftProfileFace = leftProfileFaces.toArray()[0];
            // Draw face's bounding box.
            Imgproc.rectangle(imageMat, leftProfileFace.tl(), leftProfileFace.br(), this.green,
                    this.thickness);
            this.leftProfileFaceDetected = true;
            this.face = leftProfileFace;
        }

        return imageMat;
    }

    public Mat detectRightProfileFace(Mat imageMat) {

        /* Method for detecting right profile face using Haar Cascade classifier.
         *
         * IN:
         * imageMat - Mat - image to be analyzed.
         *
         * OUT:
         * imageMat - Mat - image with face detection results. */

        // Flip image to do right side profile face detection.
        Mat flippedImageMat = new Mat();
        flip(imageMat, flippedImageMat, 1);
        MatOfRect rightProfileFaces = new MatOfRect();
        // Detect face.
        this.profileFacesClassifier.detectMultiScale(flippedImageMat, rightProfileFaces);

        if(!rightProfileFaces.empty()) {
            Rect rightProfileFace = rightProfileFaces.toArray()[0];
            // Flip detected right profile face coordinates back to original state.
            rightProfileFace.x = (int) imageMat.size().width - rightProfileFace.x - rightProfileFace.width;
            // Draw face's bounding box.
            Imgproc.rectangle(imageMat, rightProfileFace.tl(), rightProfileFace.br(), this.green,
                    this.thickness);
            this.rightProfileFaceDetected = true;
            this.face = rightProfileFace;
        }

        return imageMat;
    }

    public void resetFlags() {

        /* Method for resetting object detection flags to their default false value. */

        this.frontalFaceDetected = false;
        this.leftProfileFaceDetected = false;
        this.rightProfileFaceDetected = false;
        this.eyesDetected = false;
    }

    public Mat detectFace(Mat imageMat) {

        /* Method for detecting face in an image using Haar Cascade classifier.
         *
         * Returns first detected face.
         *
         * IN:
         * imageMat - Mat - image to be processed.
         *
         * OUT:
         * imageMat - Mat - image with face detection results. */

        this.face = null;
        imageMat = this.detectFrontalFace(imageMat);
        if(!this.frontalFaceDetected) {
            imageMat = this.detectLeftProfileFace(imageMat);
            if(!this.leftProfileFaceDetected){
                imageMat = this.detectRightProfileFace(imageMat);
            }
        }
        this.drawImageCenter(imageMat);
        this.resetFlags();

        return imageMat;
    }

    public Mat detectEyes(Mat imageMat) {

        /* Method for detecting eyes in an image using Haar Cascade classifier.
         *
         * IN:
         * imageMat - Mat - image to be processed.
         *
         * OUT:
         * imageMat - Mat - image with eyes detection results. */

        MatOfRect eyes = new MatOfRect();
        this.eyesClassifier.detectMultiScale(imageMat, eyes);

        if(!eyes.empty()) {
            Rect[] eyesArray = eyes.toArray();
            this.eyesDetected = true;
            for(int i = 0; i < eyesArray.length; i++){
                Imgproc.rectangle(imageMat, eyesArray[i].tl(), eyesArray[i].br(), this.green,
                        this.thickness);
            }
        }

        return  imageMat;
    }

    public Mat detectCircle(Mat image) {

        /* Method for detecting circle in an image using Haar Cascade classifier.
         *
         * IN:
         * imageMat - Mat - image to be processed.
         *
         * OUT:
         * imageMat - Mat - image with circle detection results. */

        this.circle = null;
        // Convert image to grayscale.
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY);
        // Blur the image
        Mat blurredImage = new Mat();
        Imgproc.medianBlur(gray, blurredImage, 5);
        // Detect circles
        Mat circles = new Mat();
        Imgproc.HoughCircles(blurredImage, circles, Imgproc.HOUGH_GRADIENT, 1, 60,
                200, 40, 0, 0);
        if(circles.cols() > 0){
            this.circle = circles.get(0, 0);
            Point center = new Point(Math.round(this.circle[0]), Math.round(this.circle[1]));
            // Draw circle center.
            Imgproc.circle(image, center, 1, this.green, 3, 8, 0);
            // Draw circle outline.
            int radius = (int) Math.round(this.circle[2]);
            Imgproc.circle(image, center, radius, this.green, 3, 8, 0);
            // Draw image center.
            this.drawImageCenter(image);
        }

        return image;
    }

    //----------------------------------------------------------------------------------------------
    // End Methods
    //----------------------------------------------------------------------------------------------
}
