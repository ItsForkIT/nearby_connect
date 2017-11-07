package com.example.bishakh.nearby_connect;

import android.Manifest;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private String SERVICE_ID = "nearby_connect";
    private PersistentLogger persistentLogger;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(getBaseContext(), "G API connected successfully..", Toast.LENGTH_SHORT).show();
        persistentLogger.write("G API connected successfully");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getBaseContext(), "G API suspended " + i, Toast.LENGTH_SHORT).show();
        persistentLogger.write("G API suspended " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getBaseContext(), "G API failed " + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
        persistentLogger.write("G API failed " + connectionResult.getErrorCode());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request runtime permissions

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        // Initialize logger
        try {
            persistentLogger = new PersistentLogger();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Initialize google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();


        // Initialize buttons
        Button discoverButton = (Button) findViewById(R.id.discover_button);
        discoverButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startDiscovery();
                Toast.makeText(getBaseContext(), "Start discovery..", Toast.LENGTH_SHORT).show();
            }
        });

        Button advertiseButton = (Button) findViewById(R.id.advertrise_button);
        advertiseButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startAdvertising();
                Toast.makeText(getBaseContext(), "Start advertising..", Toast.LENGTH_SHORT).show();
            }
        });


    }



    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    private  String getUserNickname(){
        return "HardcodedUsername";
    }

    // Connection Lifecycle management ======================================

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Nearby.Connections.acceptConnection(
                            mGoogleApiClient, endpointId, mPayloadCallback);
                    Toast.makeText(getBaseContext(), "Connection accepted!: " + endpointId, Toast.LENGTH_SHORT).show();
                    persistentLogger.write("Connection accepted " + endpointId);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Toast.makeText(getBaseContext(), "connected! : " + endpointId, Toast.LENGTH_SHORT).show();
                            persistentLogger.write("Connected " + endpointId);
                            // We're connected! Can now start sending and receiving data.
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                    }
                }
                @Override
                public void onDisconnected(String endpointId) {
                    Toast.makeText(getBaseContext(), "disconnected: " + endpointId, Toast.LENGTH_SHORT).show();
                    persistentLogger.write("Disconnected " + endpointId);
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };


    // Advertising =============================================

    private void startAdvertising() {
        Nearby.Connections.startAdvertising(
                mGoogleApiClient,
                getUserNickname(),
                SERVICE_ID,
                mConnectionLifecycleCallback,
                new AdvertisingOptions(Strategy.P2P_STAR))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    // We're advertising!
                                    Toast.makeText(getBaseContext(), "Advertising successfully..", Toast.LENGTH_SHORT).show();
                                    persistentLogger.write("Started Advertising");
                                } else {
                                    // We were unable to start advertising.
                                    persistentLogger.write("Failed to start Advertising");
                                }
                            }
                        });
        ((TextView) findViewById(R.id.status_text)).setText("Advertising..");
    }






    // Discovery ===========================================================

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {

                @Override
                public void onEndpointFound(
                        final String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    // request connection
                    Toast.makeText(getBaseContext(), "discovered.. requesting..: " + endpointId, Toast.LENGTH_SHORT).show();
                    persistentLogger.write("Endpoint discovered " + endpointId);
                    String name = getUserNickname();
                    Nearby.Connections.requestConnection(
                            mGoogleApiClient,
                            name,
                            endpointId,
                            mConnectionLifecycleCallback)
                            .setResultCallback(
                                    new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(@NonNull Status status) {
                                            if (status.isSuccess()) {
                                                // We successfully requested a connection. Now both sides
                                                // must accept before the connection is established.
                                                persistentLogger.write("Requested endpoint for connection " + endpointId);
                                            } else {
                                                // Nearby Connections failed to request the connection.
                                                persistentLogger.write("Failed to request endpoint for connection " + endpointId);
                                            }
                                        }
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                    persistentLogger.write("Endpoint lost " + endpointId);
                }
            };

    private void startDiscovery() {
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(Strategy.P2P_STAR))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Toast.makeText(getBaseContext(), "started discovery", Toast.LENGTH_SHORT).show();
                                    ((TextView) findViewById(R.id.status_text)).setText("Discovering..");
                                    persistentLogger.write("Started Discovery");

                                    // We're discovering!
                                } else {
                                    // We were unable to start discovering.
                                    Toast.makeText(getBaseContext(), "unable to start discovery", Toast.LENGTH_SHORT).show();
                                    persistentLogger.write("Failed to start Discovery");
                                }
                            }
                        });
    }


    // Payload ==========================
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {

                    //onReceive(mEstablishedConnections.get(endpointId), payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

                }
            };



}
