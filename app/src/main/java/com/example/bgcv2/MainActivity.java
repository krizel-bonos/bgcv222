package com.example.bgcv2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String NODEMCU_IP = "http://192.168.5.177"; // Replace with your NodeMCU IP
    private static final String TARGET_SSID = "Esp8266_node"; // Wi-Fi SSID of the NodeMCU
    private DatabaseReference databaseReference;
    private boolean isConnected = false; // Track connection status
    private TextView connectionStatus; // Reference to connection status TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_main);

        Button buttonOn = findViewById(R.id.buttonOn);
        Button buttonOff = findViewById(R.id.buttonOff);
        Button buttonConnect = findViewById(R.id.buttonConnect);
        connectionStatus = findViewById(R.id.connectionStatus); // Initialize the TextView

        // Initialize databaseReference
        databaseReference = FirebaseDatabase.getInstance().getReference("switch");

        buttonOn.setOnClickListener(v -> {
            databaseReference.child("staton").setValue(1);
            databaseReference.child("statoff").setValue(0);
            sendRequest("/switch", "1");
        });

        buttonOff.setOnClickListener(v -> {
            databaseReference.child("staton").setValue(0);
            databaseReference.child("statoff").setValue(1);
            sendRequest("/switch", "0");
        });

        // Handle connect button click
        buttonConnect.setOnClickListener(v -> toggleConnection());

        // Read Firebase database values
        readFirebaseData();
    }

    private void toggleConnection() {
        if (isConnected) {
            isConnected = false; // Disconnect
            connectionStatus.setText("Disconnected");
            Log.d("MainActivity", "Disconnected"); // Debug log
            sendRequest("/connection", "0"); // Notify NodeMCU of disconnection
        } else {
            if (isConnectedToTargetSSID()) {
                isConnected = true; // Connect
                connectionStatus.setText("Connected to ESP");
                Log.d("MainActivity", "Connected to ESP"); // Debug log
                sendRequest("/connection", "1"); // Notify NodeMCU of connection
            } else {
                connectionStatus.setText("Not connected to ESP");
                Log.d("MainActivity", "Not connected to ESP"); // Debug log
            }
        }
    }

    private boolean isConnectedToTargetSSID() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String currentSSID = wifiManager.getConnectionInfo().getSSID();
        // Check if the current SSID matches the target SSID
        return currentSSID != null && currentSSID.equals("\"" + TARGET_SSID + "\"");
    }

    private void readFirebaseData() {
        databaseReference.child("staton").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int state = snapshot.getValue(Integer.class);
                    sendRequest("/switch", String.valueOf(state));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors
            }
        });
    }

    private void sendRequest(String path, String state) {
        new Thread(() -> {
            try {
                URL url = new URL(NODEMCU_IP + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(state.getBytes());
                os.flush();
                os.close();
                conn.getResponseCode(); // Trigger the request
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
