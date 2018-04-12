package edu.utep.cs.cs4330.sudoku;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.utep.cs.cs4330.sudoku.model.Board;

import static android.provider.Settings.NameValueTable.NAME;

/**
 * HW1 template for developing an app to play simple Sudoku games.
 * You need to write code for three callback methods:
 * newClicked(), numberClicked(int) and squareSelected(int,int).
 * Feel free to improved the given UI or design your own.
 *
 * <p>
 *  This template uses Java 8 notations. Enable Java 8 for your project
 *  by adding the following two lines to build.gradle (Module: app).
 * </p>
 *
 * <pre>
 *  compileOptions {
 *  sourceCompatibility JavaVersion.VERSION_1_8
 *  targetCompatibility JavaVersion.VERSION_1_8
 *  }
 * </pre>
 *
 * @author Yoonsik Cheon
 */
public class MainActivity extends AppCompatActivity {

    private Board board;

    private BoardView boardView;

    /**
     * All the number buttons.
     */
    private List<View> numberButtons;
    private static final int[] numberIds = new int[]{
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4,
            R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9,
    };

    /**
     * Array to remember where to insert number
     */
    private int[] selected = {-1, -1};

    /**
     * Unmodifiable spaces
     */
    private ArrayList<int[]> hint;

    /**
     * Sounds for the game
     */
    SoundPool effects;
    int win;
    int error;
    int place;
    int restart;

    /**
     * Width of number buttons automatically calculated from the screen size.
     */
    private static int buttonWidth;

    //private NetworkUtil utilities;

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
    public static final UUID MY_UUID = UUID.fromString("1a9a8d20-3db7-11e8-b467-0ed5f89f718b");
    private NetworkAdapter connection;
    private NetworkAdapter.MessageListener heyListen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //TODO: Fix toolbar placement in activity_settings.xml (probably)
        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        int sizePref = Integer.parseInt(sharedPref.getString("list_board_size", "4"));
        int diffPref = Integer.parseInt(sharedPref.getString("list_difficulty", "1"));

        board = new Board(sizePref, diffPref);
        boardView = findViewById(R.id.boardView);
        boardView.setBoard(board);
        boardView.addSelectionListener(this::squareSelected);

        numberButtons = new ArrayList<>(numberIds.length);
        for (int i = 0; i < numberIds.length; i++) {
            if (i <= 9) {
                final int number = i; // 0 for delete button
                View button = findViewById(numberIds[i]);
                button.setOnClickListener(e -> numberClicked(number));
                numberButtons.add(button);
                setButtonWidth(button);
            }
        }

        /**if (board.size == 4){
         for (int i = 5; i < numberIds.length; i++) {
         View button = findViewById(numberIds[i]);
         button.setEnabled(false);
         }
         }*/
        board.size = sizePref;
        board.difficulty = diffPref;
        hint = new ArrayList<>();
        for (int i = 0; i < board.size; i++) {
            for (int j = 0; j < board.size; j++) {
                if (board.player[i][j] > 0) {
                    int[] temp = {i, j};
                    hint.add(temp);
                }
            }
        }
        boardView.setHint(hint);

        effects = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        win = effects.load(this, R.raw.tada, 1);
        error = effects.load(this, R.raw.incorrect, 1);
        place = effects.load(this, R.raw.boop, 1);
        restart = effects.load(this, R.raw.pageflip, 1);

        //utilities = new NetworkUtil();

        listDevices = new ArrayList<BluetoothDevice>();
        nameDevices = new ArrayList<String>();
        peer = null;
        adapter = BluetoothAdapter.getDefaultAdapter();
        outSt = new ByteArrayOutputStream(1024);
        logger = new PrintStream(outSt);
        heyListen = new NetworkAdapter.MessageListener() {
            @Override
            public void messageReceived(NetworkAdapter.MessageType type, int x, int y, int z, int[] others) {
                switch (type.header){
                    case "join:":
                        ArrayList<Integer> param = new ArrayList<Integer>();
                        for (int i = 0; i < board.size; i++) {
                            for (int j = 0; j < board.size; j++) {
                                if (board.player[i][j] > 0) {
                                    param.add(i);
                                    param.add(j);
                                    param.add(board.player[i][j]);
                                    int[] temp = {i, j};
                                    for (int[] space: hint) {
                                        if(space[0] == temp[0] && space[1] == temp[1]){
                                            param.add(1);
                                        }
                                    }
                                    if(param.size() % 4 != 0){
                                        param.add(0);
                                    }
                                }
                            }
                        }
                        int[] ret = new int[param.size()];
                        for (int i=0; i < ret.length; i++)
                        {
                            ret[i] = param.get(i).intValue();
                        }
                        connection.writeJoinAck(board.size, ret);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toast("Peer Joined");
                            }
                        });
                        break;
                    case "join_ack:":
                        board.size = y;
                        board.player = new int[y][y];
                        hint = new ArrayList<>();
                        for(int i = 0; i < others.length; i = i + 4){
                            board.player[others[i]][others[i + 1]] = others[i + 2];
                            if(others[i + 3] == 1){
                                int[] temp = {others[i], others[i + 1]};
                                hint.add(temp);
                            }
                        }
                        boardView = findViewById(R.id.boardView);
                        boardView.setBoard(board);
                        boardView.setHint(hint);
                        boardView.postInvalidate();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toast("Peer Joined");
                            }
                        });
                        break;
                    case "new:":
                        boolean[] nGame = {false};
                        runOnUiThread(new Runnable() {
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage("Do you want to share a new board?");
                                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        nGame[0] = true;
                                    }
                                }).setNegativeButton("No", null);
                                AlertDialog warning = builder.create();
                                warning.show();
                            }
                        });
                        Log.d("New", String.valueOf(nGame[0]));
                        if(nGame[0]){
                            board.size = x;
                            board.player = new int[x][x];
                            hint = new ArrayList<>();
                            for (int i = 0; i < others.length; i = i + 4) {
                                board.player[others[i]][others[i + 1]] = others[i + 2];
                                if (others[i + 3] == 1) {
                                    int[] temp = {others[i], others[i + 1]};
                                    hint.add(temp);
                                }
                            }
                            boardView = findViewById(R.id.boardView);
                            boardView.setBoard(board);
                            boardView.setHint(hint);
                            boardView.postInvalidate();
                            connection.writeNewAck(true);
                        }else {
                            connection.writeNewAck(false);
                        }
                        break;
                    case "new_ack:":
                        if(x == 1){
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toast("Peer accepted");
                                }
                            });
                        }else {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toast("Peer rejected");
                                }
                            });
                        }
                        break;
                    case "fill:":
                        board.player[x][y] = z;
                        connection.writeFillAck(x, y, z);
                        boardView.postInvalidate();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toast(String.format("Peer inserted %d at %d, %d", z, x, y));
                            }
                        });
                        break;
                    case "fill_ack:":
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toast(String.format("Peer received successfully"));
                            }
                        });
                        break;
                    case "quit:":

                        break;
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Actions when Toolbar option is selected
        switch (item.getItemId()) {
            case R.id.action_settings:
                //TODO: fix call settings activity
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;

            case R.id.action_solve_puzzle:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Do you give up?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                board.solveForUser(true);
                                boardView.postInvalidate();
                                for (int i = 0; i < numberIds.length; i++) {
                                    View button = findViewById(numberIds[i]);
                                    button.setEnabled(false);
                                }
                                toast("Here is the solution!");
                            }
                        })
                        .setNegativeButton("No", null);
                AlertDialog warning = builder.create();
                warning.show();

                break;
            case R.id.action_check_puzzle:
                effects.play(place, 1, 1, 1, 0, 1);
                if (board.solveForUser(false)) {
                    toast("Don't worry, you're on the right path!");
                } else {
                    toast("Something must have gone wrong, you should go back and check.");
                }
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback to be invoked when the new button is tapped.
     */
    public void newClicked(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to start a new game?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        effects.play(restart, 1, 1, 1, 0, 1);
                        recreate();
                        toast("New Game Started! Good Luck!");
                        ArrayList<Integer> param = new ArrayList<Integer>();
                        for (int i = 0; i < board.size; i++) {
                            for (int j = 0; j < board.size; j++) {
                                if (board.player[i][j] > 0) {
                                    param.add(i);
                                    param.add(j);
                                    param.add(board.player[i][j]);
                                    int[] temp = {i, j};
                                    for (int[] space: hint) {
                                        if(space[0] == temp[0] && space[1] == temp[1]){
                                            param.add(1);
                                        }
                                    }
                                    if(param.size() % 4 != 0){
                                        param.add(0);
                                    }
                                }
                            }
                        }
                        int[] ret = new int[param.size()];
                        for (int i=0; i < ret.length; i++)
                        {
                            ret[i] = param.get(i).intValue();
                        }
                        connection.writeNew(board.size, ret);
                    }
                })
                .setNegativeButton("No", null);
        AlertDialog warning = builder.create();
        warning.show();
    }

    /**
     * Callback to be invoked when a number button is tapped.
     *
     * @param n Number represented by the tapped button
     *          or 0 for the delete button.
     */
    public void numberClicked(int n) {
        // WRITE YOUR CODE HERE ...
        //
        if (selected[0] >= 0 && selected[1] >= 0) {
            effects.play(place, 1, 1, 1, 0, 1);
            if (n > 0) {
                if (!board.checkConflict(board.player, selected[0], selected[1], n)) {
                    board.player[selected[0]][selected[1]] = n;
                    boardView.postInvalidate();
                    if(connection != null){
                        connection.writeFill(selected[0], selected[1], n);
                    }
                    if (board.puzzleSolved()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Congratulations!!! You Won! Would you like to play again?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        effects.play(restart, 1, 1, 1, 0, 1);
                                        recreate();
                                        toast("New Game Started! Good Luck!");
                                    }
                                })
                                .setNegativeButton("No", null);
                        AlertDialog warning = builder.create();
                        warning.show();
                        effects.play(win, 1, 1, 1, 0, 1);
                    }
                } else {
                    toast("Input conflict, can't add that number to this space.");
                    effects.play(error, 1, 1, 1, 0, 1);
                }
            } else {
                board.removeFromPlayer(selected[0], selected[1]);
                if(connection != null){
                    connection.writeFill(selected[0], selected[1], n);
                }
                boardView.postInvalidate();
                toast("Item removed");
            }
        }
    }

    /**
     * Callback to be invoked when a square is selected in the board view.
     *
     * @param x 0-based column index of the selected square.
     * @param x 0-based row index of the selected square.
     */
    private void squareSelected(int x, int y) {
        // WRITE YOUR CODE HERE ...
        //
        for (int[] i : hint) {
            if (i[1] == x && i[0] == y) {
                toast(String.format("Can't select this space!"));
                effects.play(error, 1, 1, 1, 0, 1);
                return;
            }
        }
        //toast(String.format("Square selected: (%d, %d)", x, y));
        selected[0] = y;
        selected[1] = x;
    }

    /**
     * Show a toast message.
     */
    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Set the width of the given button calculated from the screen size.
     */
    private void setButtonWidth(View view) {
        if (buttonWidth == 0) {
            final int distance = 2;
            int screen = getResources().getDisplayMetrics().widthPixels;
            buttonWidth = (screen - ((9 + 1) * distance)) / 9; // 9 (1-9)  buttons in a row
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = buttonWidth;
        view.setLayoutParams(params);
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
                socket = server.accept(10000);
            } catch (IOException e) {
                Log.e("Not accepting", "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                toast("Connected");
                connection = new NetworkAdapter(socket, logger);
                connection.setMessageListener(heyListen);
                connection.receiveMessagesAsync();
                server.close();
                break;
            }
            else {
                toast("Null socket");
            }
        }
    }

    public void serverClickedApp(View view) throws IOException {
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
            connection = new NetworkAdapter(client, logger);
            connection.setMessageListener(heyListen);
            connection.receiveMessagesAsync();
            connection.writeJoin();
        }
    }

    public void off(View v){
        adapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    public void clientClickedApp(View view) {
        //utilities.clientClicked(view);
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
