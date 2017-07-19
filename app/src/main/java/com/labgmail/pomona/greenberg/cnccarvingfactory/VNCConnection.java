package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.realvnc.vncsdk.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;


/**
 * Created by soniagrunwald on 7/13/17.
 */

public class VNCConnection {
    private DrawingView dv;
    private Viewer viewer;

    private boolean connected = false;
    private DirectTcpConnector tcpConnector;

    public VNCConnection(final DrawingView dv, final String host, final int port, final String username, final String password) {
        this.dv = dv;

        SdkThread.getInstance().init(dv.getContext().getFilesDir().getAbsolutePath() + "VNC");

        while (!SdkThread.getInstance().initComplete()) {
            try { Thread.sleep(10); }
            catch (InterruptedException e) { }
        }

        try {
            viewer = new Viewer();
            viewer.setPictureQuality(Viewer.PictureQuality.AUTO);

            connect(host, port, username, password);
        } catch (Library.VncException e) {
            Log.d("LIVE",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize a connection with, e.g., tool settings, etc. Called by the VNC Viewer when connection is ready.
     */
    private void startConnection() {
        connected = true;

        // TODO use a framebuffer to verify that we're on the right page
        // TODO navigate to MDI screen
        // TODO send prelude
        // TODO use sendMDI to grab the current tool (per DV) and spin it up, go back to the origin


        saveFrameBuffer();
        sendTestText("HELLOWORLD");
        saveFrameBuffer();

    }

    //Checks if you're on an input page. If so, send the specified command
    public void sendTestText(final String s) {
        sdk(new Runnable() {
            @Override
            public void run() {
                //Get the framebuffer and save into the "bitmap" object
                int width = viewer.getViewerFbWidth();
                int height = viewer.getViewerFbHeight();

                //IF YOU WANT TO SHOW ON THE SCREEN, USE THE BITMAP FROM DRAWING VIEW (I THINK)
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.setHasAlpha(false);

                try {
                    // getViewerFbData(int x, int y, int w, int h, Object bitmap, int targetX, int targetY)
                    viewer.getViewerFbData(0, 0, width, height, bitmap, 0, 0);
                } catch (Library.VncException e) {
                    Log.d("BUTTON", "ERROR " + e);
                }

                // examine and check if it's correct and if so send the string
                if (isInputPage(bitmap)) {
                    sdk(new Runnable() {
                        @Override
                        public void run() {
                            sendMDI(s);
                        }
                    });
                } else {
                    int color =  bitmap.getPixel(300, 75);
                    Log.d("BUTTON", String.format("Wrong screen. Color detected is (%s, %s, %s)",
                            (color >> 16) & 0xff, (color >>  8) & 0xff, color & 0xff));
                }
            }
        });
    }

    //TODO MAKE THIS CHECK BETTER
    //check you're on the input page
    private boolean isInputPage(Bitmap bitmap) {
        return bitmap.getPixel(300, 75) == new Color().rgb(241, 241, 241);
    }

    private boolean isMDIExecutePage(Bitmap bitmap){
        return (bitmap.getPixel(300, 50) == new Color().rgb(254, 203, 254)) &&
                (bitmap.getPixel(360, 280) == new Color().rgb(203, 153,152));
    }


    //Save the image in the framebuffer
    public void saveFrameBuffer() {
        sdk(new Runnable() {
            @Override
            public void run() {
                //Get the framebuffer and save into the "bitmap" object
                int width = viewer.getViewerFbWidth();
                int height = viewer.getViewerFbHeight();

                //IF YOU WANT TO SHOW ON THE SCREEN, USE THE BITMAP FROM DRAWING VIEW (I THINK)
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.setHasAlpha(false);

                try {
                    viewer.getViewerFbData(0, 0, width, height, bitmap, 0, 0);
                    saveImage(bitmap);
                } catch (Library.VncException e) {
                    Log.d("BUTTON", "ERROR " + e);
                }
            }
        });
    }


    //Saves a bitmap image to the pictures folder
    private void saveImage (Bitmap bitmap){
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        try {
            dir.mkdirs();
            StringBuilder filename = new StringBuilder();
            CharSequence timestamp = DateFormat.format("VNC_yyyy-MM-ddThh:mm:ss", new Date());
            filename.append(timestamp);
            filename.append("_Image.png");

            File file = new File(dir, filename.toString());
            FileOutputStream fOut = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaScannerConnection.scanFile(dv.getContext(), new String[]{file.getAbsolutePath()}, null, null);

            Toast.makeText(dv.getContext(), String.format("Saved image to %s", filename.toString()), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(dv.getContext(), "Couldn't save image (" + e.getLocalizedMessage() + ")", Toast.LENGTH_SHORT).show();
            Log.d("IO", e.toString());
        }
    }





    /**
     * Sends a given line of MDI. Only call after startConnection. CNC machine must be on MAN screen for MDI commands.
     *
     * @param code
     */
    private void sendMDI(String code) {
        if (!connected) { return; }

        // Assuming we're on the INPUT PAGE
        // click the right buttons to send a single line of G code

        //execute clicking the enter bar, clear all
        executeClick(new VNCButton(VNCButton.Button.ENTER));
        executeClick(new VNCButton(VNCButton.Button.CLEARALL));

        //break into character array
        String newCode = code.toUpperCase();
        char[] charArray = newCode.toCharArray();

        //for every character, translate it into a VNCButton and execute the click
        for(char c : charArray){
            if (Character.isDigit(c)){ //if it's an integer
                executeClick(new VNCButton(Character.digit(c, 10)));
            } else { //otherwise use the string version
                executeClick(new VNCButton(Character.toString(c)));
            }
        }

        //execute enter
        executeClick(new VNCButton(VNCButton.Button.ENTER));

    }


    private void executeClick (VNCButton button){
        //do all the clicks necessary in the coordinate list
        for (PointF p : button.getCoordinates()) {
            try {
                viewer.sendPointerEvent(Math.round(p.x), Math.round(p.y), EnumSet.of(Viewer.MouseButton.MOUSE_BUTTON_LEFT), false);
            } catch (Library.VncException e){
                Log.d("BUTTON", "ERROR: " + e);
            }
            Log.d("BUTTON", "Click at (" + p.x + ", " + p.y + ")" );
        }
    }




    /**
     * Set up handlers and have the viewer initialize a VNC connection.
     *
     * @param host
     * @param port
     * @param username
     * @param password
     * @throws Library.VncException
     */
    private void connect(final String host, final int port, final String username, final String password) throws Library.VncException {
        if (!SdkThread.getInstance().initComplete()) {
            Log.d("LIVE", "couldn't initialize VNCConnection, SDK isn't initialized");
            return;
        }

        SdkThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                try {
                    Library.enableAddOn("EGvlYZXW1wMWLdzWqAn6baMjUW50w+53E9qDUEseAP72TCJdwkwn2j9o19AyOjIA\n" +
                            "TYEXxkTZFbo78PkXKOb3MHWyNmyXeqBB3hKDKLQ4ZTReT0A57b4biUBFLu9D4aYU\n" +
                            "Q3rUk/MlnPusbGL1GPd6W9fDm7LawcSeJINp6/+3scnxO+EVE04EzrRNG1YDxVpL\n" +
                            "/GiwOGQHyFlBkuBNqIydC5iuTElYbiqzjxQqDpKPugR2Nxn4L9oWKMlF5/5rar9j\n" +
                            "eg0ImXZ0GKHtfrR8g6J9+C2NzIAZOkD5bqu330bGdX5hEWIHgZ5GL+nXUZ8Dc7zV\n" +
                            "Cmx6Lzvt5AlU502vqpU3PjMuVWHdsgJlqUt/53q6Zngspz4uIbg9yuoO7/9fWK0s\n" +
                            "nuSaTSlbSx5vDAp2e2EW9dXTAsIjFLnHAfIoLf9ctegCRmGq7CPOYE8o8GYdc0FV\n" +
                            "iF9iQNoiB9UUCNJPHb5BHVPcrj5trxnnt+pSe6xj4Q1L+Qp9hKof8WpgpKn/Ai3r\n" +
                            "ITdSYejDpsmIUN8tcGMMjUub5bk2H5YpEE9FVrAeaA4QXF47cURb8dwdTPuJuutS\n" +
                            "nvAKGHvqk1Sd5IDz/OHFPiSe6RPyiOQ6LnsjJVpWr9sF1qK9lJwRep+uvcmAizhi\n" +
                            "e65j0sSA0grDUoj7VK4t13XBYqGvstHSVMnWI9OQ9qo=");
                } catch (Library.VncException e) {
                    Log.d("LIVE", "couldn't load TCP add on key");
                    return;
                }
            }
        });

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) { }

        // set up handlers for login
        viewer.setConnectionCallback(new Viewer.ConnectionCallback() {
            @Override
            public void connected(Viewer viewer) {
                Log.d("LIVE","connected");
                Toast.makeText(dv.getContext(), "Connected to " + host + "!", Toast.LENGTH_SHORT).show();
                startConnection();
            }

            @Override
            public void connecting(Viewer viewer) {
                Log.d("LIVE","connecting");
            }

            @Override
            public void disconnected(Viewer viewer, String s, EnumSet<Viewer.DisconnectFlags> enumSet) {
                Log.d("LIVE","disconnected " + viewer.getDisconnectReason() + " " + viewer.getDisconnectMessage());

                Toast.makeText(dv.getContext(), "Disconnected from " + host, Toast.LENGTH_LONG).show();
                // TODO notify DrawingView somehow?
            }
        });

        viewer.setAuthenticationCallback(new Viewer.AuthenticationCallback() {
            @Override
            public void cancelUserCredentialsRequest(Viewer viewer) {
                Log.d("LIVE", "user credential request canceled (?)");
            }

            @Override
            public void requestUserCredentials(Viewer viewer, final boolean needUser, final boolean needPassword) {
                try {
                    Log.d("LIVE", "authenticating with " + username + " and " + password); // TODO security leak
                    viewer.sendAuthenticationResponse(true, username, password);
                } catch (Library.VncException e) {
                    Log.d("LIVE", "authentication failed: " + e.getMessage());
                    Toast.makeText(dv.getContext(), "Authentication failed", Toast.LENGTH_LONG).show();
                }
            }
        });

            /* TODO by rights, we should probably actually LOOK at the screen, by setting a framebuffer callback
             * but... what if we don't?
             */

        viewer.setPeerVerificationCallback(new Viewer.PeerVerificationCallback() {
            @Override
            public void cancelPeerVerification(Viewer viewer) {
                Log.d("LIVE", "peer verification canceled");
            }

            @Override
            public void verifyPeer(final Viewer viewer, String hexFingerprint, String catchphraseFingerprint, ImmutableDataBuffer serverRsaPublic) {
                SdkThread.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d("LIVE", "verifying peer");
                            viewer.sendPeerVerificationResponse(true);
                        } catch (Library.VncException e) {
                            Log.d("LIVE", e.getMessage());
                        }
                    }
                });
            }
        });

        // actually connect
        SdkThread.getInstance().post(new Runnable() {

            @Override
            public void run() {
                try {
                    tcpConnector = new DirectTcpConnector();
                    tcpConnector.connect(host, port, viewer.getConnectionHandler());
                } catch (Library.VncException e) {
                    Log.d("LIVE", e.getMessage());
                }
            }
        });
    }


    public void disconnect() {
        connected = false;

        sdk(new Runnable() {
                @Override
                public void run() {
                    if (viewer != null) {
                        try {
                            viewer.disconnect();
                        } catch (Library.VncException e) {
                            Log.d("LIVE",e.getMessage());
                        }

                        tcpConnector.destroy();
                        viewer.destroy();
                        viewer = null;
                    }
                }
            });
    }

    private boolean sdk(Runnable r) {
        if (SdkThread.getInstance().initComplete()) {
            SdkThread.getInstance().post(r);
            return true;
        } else {
            Log.d("LIVE", "SDK thread not initialized, couldn't run " + r.toString());
            return false;
        }
    }

}
