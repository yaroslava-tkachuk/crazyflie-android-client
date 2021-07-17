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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class CameraStreamer implements Runnable {

    Socket socket;
    private int port = 5000;
    private String ipAddress = "192.168.4.1";
    private int imageSizeBytes = 512;
    private byte frameStart[] = {(byte) 0xff, (byte) 0xd8};
    private byte frameEnd[] = {(byte) 0xff, (byte) 0xd9};
    MainActivity mainActivity;

    CameraStreamer(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        OpenCVLoader.initDebug();

    }

    private static int findFrameSequence(byte[] buffer, byte[] searchSequence) {
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
        byte[] result = new byte[buffer0.length + buffer1Stop];
        System.arraycopy(buffer0, 0, result, 0, buffer0.length);
        System.arraycopy(buffer1, 0, result, buffer0.length, buffer1Stop);

        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        boolean run = true;
        byte[] imageBuffer = new byte[0];
        byte[] buffer = new byte[this.imageSizeBytes];
        int bytesRead;
        byte[] frame;
        try {
            this.socket = new Socket(this.ipAddress, this.port);
            socket.setSoTimeout(3000);
            DataInputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            while (!Thread.interrupted() && run) {
                // Read data from socket
                bytesRead = inputStream.read(buffer, 0, this.imageSizeBytes);
                imageBuffer = concatenateBuffers(imageBuffer, buffer, bytesRead);

                // Find start and end of JPEG image frame markers
                int startFrameIdx = findFrameSequence(imageBuffer, this.frameStart);
                int endFrameIdx = findFrameSequence(imageBuffer, this.frameEnd);

                // If end of frame marker precedes start of frame marker, cut off the buffer before
                // start of frame marker
                if( (endFrameIdx > -1) && (startFrameIdx > -1) && (endFrameIdx < startFrameIdx) ) {
                    imageBuffer = Arrays.copyOfRange(imageBuffer, startFrameIdx, imageBuffer.length);
                }

                // If buffer contains both start and end of frame markers and start of frame marker
                // precedes end of frame marker, prepare and render an image
                if( (startFrameIdx > -1) && (endFrameIdx > -1) && (endFrameIdx > startFrameIdx) ) {
                    frame = Arrays.copyOfRange(imageBuffer, startFrameIdx, endFrameIdx+2);
                    imageBuffer = Arrays.copyOfRange(imageBuffer, endFrameIdx+2, imageBuffer.length);

                    // Create a bitmap from byte array
                    final Bitmap imageBmp = BitmapFactory.decodeByteArray(frame, 0, frame.length);
                    final ImageView cameraImageView = this.mainActivity.getCameraImageView();


                    // Convert Bitmap into Mat
                    Mat imageMat = new Mat(imageBmp.getHeight(), imageBmp.getWidth(), CvType.CV_8UC4);
                    Utils.bitmapToMat(imageBmp, imageMat);
                    // Detect faces
                    MatOfRect faces = new MatOfRect();
                    if (this.mainActivity.getCascadeClassifier() != null) {
                        this.mainActivity.getCascadeClassifier().detectMultiScale(imageMat, faces);
                    }

                    // Draw rectangles around faces
                    Rect[] facesRectangles = faces.toArray();
                    for (int i = 0; i < facesRectangles.length; i++)
                        Imgproc.rectangle(imageMat, facesRectangles[i].tl(), facesRectangles[i].br(), new Scalar(0, 255, 0, 255), 3);

                    // Convert Mat to Bitmap
                    Bitmap imageWithFaces = imageBmp;
                    Utils.matToBitmap(imageMat, imageWithFaces);
                    
                    final Bitmap imageRender = imageWithFaces;

                    // Render bitmap
                    this.mainActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                cameraImageView.setImageBitmap(Bitmap.createScaledBitmap(imageRender,
                                    cameraImageView.getWidth(), cameraImageView.getHeight(), false));
                            }
                            catch(Exception e) {
                                Log.e("Image rendering error", "Huston, we have a problem: ", e);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e("TCP connection error", "Huston, we have problem: ", e);
            this.mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mainActivity.getApplicationContext(), "Lost connection with the camera.", Toast.LENGTH_SHORT).show();
                }
            });
            this.mainActivity.setCameraStreamEnabled(false);
            try {
                this.socket.close();
            } catch (IOException ioException) {
                Log.e("Socket closing error", "Huston, we have problem: ", e);
            }
            run = false;
        }
    }
}
