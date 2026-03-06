// BluetoothPrinter.java
package com.shiv.shop;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothPrinter {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice printerDevice;
    private BluetoothSocket socket;
    private OutputStream outputStream;

    private static final String PRINTER_NAME = "YourPrinterName"; // Replace with actual printer name
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public boolean connect(Context context) {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return false;

            // Runtime permission check for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            1002
                    );
                    return false; // Permission not granted
                }
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equalsIgnoreCase(PRINTER_NAME)) {
                    printerDevice = device;
                    break;
                }
            }

            if (printerDevice == null) return false;

            socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID);
            socket.connect();
            outputStream = socket.getOutputStream();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void print(String text) {
        try {
            if (outputStream != null) {
                outputStream.write(text.getBytes());
                outputStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
