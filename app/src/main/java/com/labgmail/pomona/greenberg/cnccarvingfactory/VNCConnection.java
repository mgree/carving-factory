package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.realvnc.vncsdk.DirectTcpConnector;
import com.realvnc.vncsdk.ImmutableDataBuffer;
import com.realvnc.vncsdk.Library;
import com.realvnc.vncsdk.Viewer;

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

        try {
            viewer = new Viewer();

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
    }

    /**
     * Sends a given line of MDI. Only call after startConnection. CNC machine must be on MAN screen for MDI commands.
     *
     * @param code
     */
    private void sendMDI(String code) {
        if (!connected) { return; }

        // TODO assuming we're on the MDI screen, click the right buttons to send a single line of G code
        //enter, clear all, each letter/number, ok, execute
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
                Log.d("LIVE","disconnected");
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
                            viewer.sendPeerVerificationResponse(true);
                        } catch (Library.VncException e) {
                            Log.d("LIVE", e.getMessage());
                        }
                    }
                });
            }
        });

        // actually connect
        tcpConnector = new DirectTcpConnector();
        tcpConnector.connect(host, port, viewer.getConnectionHandler());
    }

    public void disconnect() {
        connected = false;

        if (SdkThread.getInstance().initComplete()) {
            SdkThread.getInstance().post(new Runnable() {
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
    }

}
