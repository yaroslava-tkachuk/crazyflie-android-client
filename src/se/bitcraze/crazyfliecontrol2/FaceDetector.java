package se.bitcraze.crazyfliecontrol2;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
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
    private CascadeClassifier frontalFacesClassifier;
    private CascadeClassifier profileFacesClassifier;
    private CascadeClassifier eyesClassifier;
    private Scalar color = new Scalar(49, 195, 39);
    private int thickness = 3;
    private MainActivity mainActivity;
    private boolean frontalFaceDetected = false;
    private boolean leftProfileFaceDetected = false;
    private boolean rightProfileFaceDetected = false;
    private boolean eyesDetected = false;

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

    public FaceDetector(MainActivity mainActivity) throws IOException {
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

    private File loadClassifierFile(int resourceId, String fileName) throws IOException {
        // Copy resource into a file, so OpenCV can load it
        InputStream inputStream = this.mainActivity.getResources().openRawResource(resourceId);
        File cascadeDir = this.mainActivity.getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(cascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }
        // Close streams
        inputStream.close();
        fileOutputStream.close();

        return cascadeFile;
    }

    public Mat detectFrontalFace(Mat imageMat) {
        MatOfRect frontalFaces = new MatOfRect();
        this.frontalFacesClassifier.detectMultiScale(imageMat, frontalFaces);

        if (!frontalFaces.empty()) {
            Rect frontalFace = frontalFaces.toArray()[0];
            Imgproc.rectangle(imageMat, frontalFace.tl(), frontalFace.br(), this.color, this.thickness);
            this.frontalFaceDetected = true;
        }

        return imageMat;
    }

    public Mat detectLeftProfileFace(Mat imageMat) {
        MatOfRect leftProfileFaces = new MatOfRect();
        this.profileFacesClassifier.detectMultiScale(imageMat, leftProfileFaces);

        if(!leftProfileFaces.empty()) {
            Rect leftProfileFace = leftProfileFaces.toArray()[0];
            this.leftProfileFaceDetected = true;
            Imgproc.rectangle(imageMat, leftProfileFace.tl(), leftProfileFace.br(), this.color, this.thickness);
        }

        return imageMat;
    }

    public Mat detectRightProfileFace(Mat imageMat) {
        // Flip image to do right side profile face detection
        Mat flippedImageMat = new Mat();
        flip(imageMat, flippedImageMat, 1);
        MatOfRect rightProfileFaces = new MatOfRect();
        this.profileFacesClassifier.detectMultiScale(flippedImageMat, rightProfileFaces);

        if(!rightProfileFaces.empty()) {
            Rect rightProfileFace = rightProfileFaces.toArray()[0];
            // Flip detected right profile face coordinates back to original state
            rightProfileFace.x = (int) imageMat.size().width - rightProfileFace.x - rightProfileFace.width;
            this.rightProfileFaceDetected = true;
            Imgproc.rectangle(imageMat, rightProfileFace.tl(), rightProfileFace.br(), this.color, this.thickness);
        }

        return imageMat;
    }

    public void resetFlags() {
        this.frontalFaceDetected = false;
        this.leftProfileFaceDetected = false;
        this.rightProfileFaceDetected = false;
        this.eyesDetected = false;
    }

    public Mat detectFace(Mat imageMat) {
        imageMat = this.detectFrontalFace(imageMat);

        if(!this.frontalFaceDetected) {
            imageMat = this.detectLeftProfileFace(imageMat);
            if(!this.leftProfileFaceDetected){
                imageMat = this.detectRightProfileFace(imageMat);
            }
        }
        this.resetFlags();

        return imageMat;
    }

    public Mat detectEyes(Mat imageMat) {
        MatOfRect eyes = new MatOfRect();
        this.eyesClassifier.detectMultiScale(imageMat, eyes);

        if(!eyes.empty()) {
            Rect[] eyesArray = eyes.toArray();
            this.eyesDetected = true;
            for(int i = 0; i < eyesArray.length; i++){
                Imgproc.rectangle(imageMat, eyesArray[i].tl(), eyesArray[i].br(), this.color, this.thickness);
            }
        }

        return  imageMat;
    }
}
