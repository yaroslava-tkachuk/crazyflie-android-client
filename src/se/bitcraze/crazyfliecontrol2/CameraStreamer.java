package se.bitcraze.crazyfliecontrol2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;

public class CameraStreamer implements Runnable {

    private int port = 5000;
    private String ipAddress = "192.168.4.1";
    private int imageSizeBytes = 512;
    private byte frameStart[] = {(byte) 0xff, (byte) 0xd8};
    private byte frameEnd[] = {(byte) 0xff, (byte) 0xd9};
    MainActivity mainActivity;

    CameraStreamer(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
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

    public static byte[] concatenateBuffers(byte[] buffer0, byte[] buffer1) {
        byte[] result = new byte[buffer0.length + buffer1.length];
        System.arraycopy(buffer0, 0, result, 0, buffer0.length);
        System.arraycopy(buffer1, 0, result, buffer0.length, buffer1.length);

        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        boolean run = true;
        byte[] imageBuffer = new byte[0];
        byte[] buffer = new byte[this.imageSizeBytes];
        byte[] frame = new byte[0];
        try {
            Socket socket = new Socket(this.ipAddress, this.port);
            DataInputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            while (run) {
                inputStream.read(buffer, 0, this.imageSizeBytes);

                imageBuffer = concatenateBuffers(imageBuffer, buffer);

                int startFrameIdx = findFrameSequence(imageBuffer, this.frameStart);
                int endFrameIdx = findFrameSequence(imageBuffer, this.frameEnd);

                if( (endFrameIdx > -1) && (startFrameIdx > -1) && (endFrameIdx < startFrameIdx) ) {
                    imageBuffer = Arrays.copyOfRange(imageBuffer, startFrameIdx, imageBuffer.length);
                }

                if( (startFrameIdx > -1) && (endFrameIdx > -1) && (endFrameIdx > startFrameIdx) ) {
                    frame = Arrays.copyOfRange(imageBuffer, startFrameIdx, endFrameIdx+2);
                    imageBuffer = Arrays.copyOfRange(imageBuffer, endFrameIdx+2, imageBuffer.length);

                    Bitmap imageBmp = BitmapFactory.decodeByteArray(frame, 0, frame.length);
                    ImageView cameraImageView = this.mainActivity.getCameraImageView();
                    cameraImageView.setImageBitmap(Bitmap.createScaledBitmap(imageBmp,
                        cameraImageView.getWidth(), cameraImageView.getHeight(), false));
                }

//
            }
        } catch (Exception e) {
            Log.e("TCP client Error", "error: ", e);
            run = false;
        }
    }
}
