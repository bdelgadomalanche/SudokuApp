package edu.utep.cs.cs4330.sudoku;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.NameValueTable.NAME;



public class NetworkUtil extends MainActivity {

    private BluetoothAdapter adapter;
    private BluetoothDevice peer;
    private NetworkAdapter netAd;
    private BluetoothServerSocket server;
    private BluetoothSocket client;
    private List<BluetoothDevice> listDevices;
    private ArrayList<String> nameDevices;
    private int temp;
    private PrintStream logger;
    private OutputStream outSt;
    public static final java.util.UUID MY_UUID = java.util.UUID.fromString("1a9a8d20-3db7-11e8-b467-0ed5f89f718b");

    public NetworkUtil(){
        listDevices = new ArrayList<BluetoothDevice>();
        nameDevices = new ArrayList<String>();
        peer = null;
        adapter = BluetoothAdapter.getDefaultAdapter();
        outSt = new ByteArrayOutputStream(1024);
        logger = new PrintStream(outSt);
    }

    //Server Functions
    public void onServer(View v) throws IOException {
        if (!adapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(getVisible, 0);
            AcceptThread();
            runServer();
        }
    }

    public void AcceptThread() {
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            tmp = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
            Log.e("Not listening", "Socket's listen() method failed", e);
        }
        server = tmp;
    }

    public void runServer() throws IOException {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = server.accept();
            } catch (IOException e) {
                Log.e("Not accepting", "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                toast("Connected");
                NetworkAdapter connection = new NetworkAdapter(socket, logger);
                server.close();
                break;
            }
            else {
                toast("Null socket");
            }
        }
    }

    public void serverClicked(View view) throws IOException {
        onServer(view);
    }

    //Client Functions
    public void onClient(View v){
        if (!adapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            listDevices = new ArrayList<BluetoothDevice>();
            nameDevices = new ArrayList<String>();
            for (BluetoothDevice b : adapter.getBondedDevices()) {
                listDevices.add(b);
                nameDevices.add(b.getName());
            }
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            listDevices = new ArrayList<BluetoothDevice>();
            nameDevices = new ArrayList<String>();
            for (BluetoothDevice b : adapter.getBondedDevices()) {
                listDevices.add(b);
                nameDevices.add(b.getName());
            }
        }
    }


    public void ConnectThread(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        peer = device;
        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException e) {
            Log.e("error_socket", "Socket: " + tmp.toString() + " create() failed", e);
        }

        client = tmp;
        Log.d("socket", peer.toString());
    }

    public void runClient() {
        // Cancel discovery because it otherwise slows down the connection.
        adapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            client.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                client.close();
            } catch (IOException closeException) {
                Log.e("Close socket", "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.

        toast("Connected");
        if(client == null){
            toast("Null client");
        }else {
            NetworkAdapter connection = new NetworkAdapter(client, logger);
        }
    }

    public void off(View v){
        adapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    public void clientClicked(View view) {
        onClient(view);
        // setup the alert builder
        if(listDevices.isEmpty()){
            Intent turnOn = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivityForResult(turnOn, 0);
            onClient(view);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paired Devices");

        String[] arrDevices = nameDevices.toArray(new String[nameDevices.size()]);
        int checkedItem = 0;
        builder.setSingleChoiceItems(arrDevices, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                toast(arrDevices[which]);
                temp = which;
            }
        });

        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                peer = listDevices.get(temp);
                Log.d("devices", peer.getAddress());
                ConnectThread(peer);
                runClient();
            }
        });
        builder.setNeutralButton("PAIR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent turnOn = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivityForResult(turnOn, 0);
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();

    }
}
