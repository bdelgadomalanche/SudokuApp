package edu.utep.cs.cs4330.sudoku;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import edu.utep.cs.cs4330.sudoku.model.Board;

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
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Board board;

    private BoardView boardView;

    /** All the number buttons. */
    private List<View> numberButtons;
    private static final int[] numberIds = new int[] {
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4,
            R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9,
            R.id.n10, R.id.n11
    };

    /** Array to remember where to insert number */
    private int[] selected = {-1, -1};

    /** Unmodifiable spaces */
    private ArrayList<int[]> hint;

    /** Size variable */
    int size = 9;

    /** Size variable */
    int difficulty = 1;

    /** Spinners for selecting game difficulty and board size **/
    Spinner spinner_difficult, spinner_board_size;

    /** Sounds for the game */
    SoundPool effects;
    int win;
    int error;
    int place;
    int restart;

    /** Width of number buttons automatically calculated from the screen size. */
    private static int buttonWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);

        board = new Board(size, difficulty);
        boardView = findViewById(R.id.boardView);
        boardView.setBoard(board);
        boardView.addSelectionListener(this::squareSelected);

        numberButtons = new ArrayList<>(numberIds.length);
        for (int i = 0; i < numberIds.length; i++) {
            if(i <= 9) {
                final int number = i; // 0 for delete button
                View button = findViewById(numberIds[i]);
                button.setOnClickListener(e -> numberClicked(number));
                numberButtons.add(button);
                setButtonWidth(button);
            }
            else if (i == 10){
                View button = findViewById(numberIds[i]);
                button.setOnClickListener(e -> solveClicked());
                numberButtons.add(button);
            }
            else{
                View button = findViewById(numberIds[i]);
                button.setOnClickListener(e -> isSolvable());
                numberButtons.add(button);
            }
        }

        /**if (board.size == 4){
            for (int i = 5; i < numberIds.length; i++) {
                View button = findViewById(numberIds[i]);
                button.setEnabled(false);
            }
        }*/
        hint = new ArrayList<>();
        for(int i = 0; i < board.size; i++){
            for(int j = 0; j < board.size; j++){
                if(board.player[i][j] > 0) {
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

        spinner_difficult = (Spinner) findViewById(R.id.spinner_difficulty);
        spinner_board_size = (Spinner) findViewById(R.id.spinner_size);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.difficulty_array, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,R.array.board_size_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner_difficult.setAdapter(adapter);
        spinner_difficult.setOnItemSelectedListener(this);
        spinner_board_size.setAdapter(adapter2);
        spinner_board_size.setOnItemSelectedListener(this);
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
        switch(item.getItemId()) {
            case R.id.action_settings:
                //TODO: call settings activity
                toast("Settings clicked");
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    /** Callback to be invoked when the new button is tapped. */
    public void newClicked(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to start a new game?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
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
    }

    /** Callback to be invoked when a number button is tapped.
     *
     * @param n Number represented by the tapped button
     *          or 0 for the delete button.
     */
    public void numberClicked(int n) {
        // WRITE YOUR CODE HERE ...
        //
        if(selected[0] >= 0 && selected[1] >= 0) {
            effects.play(place, 1, 1, 1, 0, 1);
            if (n > 0) {
                if (!board.checkConflict(board.player, selected[0], selected[1], n)) {
                    board.player[selected[0]][selected[1]] = n;
                    boardView.postInvalidate();
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
                boardView.postInvalidate();
                toast("Item removed");
            }
        }
    }

    /** Solve for the user button logic */
    public void solveClicked() {
        effects.play(place, 1, 1, 1, 0, 1);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Do you give up?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        board.solveForUser();
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
    }

    /** Check if the player's solution is still solvable */
    public void isSolvable() {
        effects.play(place, 1, 1, 1, 0, 1);
        boolean isIt = board.solvable();
        if(isIt){
            toast("Don't worry, you're on the right path!");
        }
        else{
            toast("Something must have gone wrong, you should go back and check.");
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
        for (int [] i : hint) {
            if(i[1] == x && i[0] == y){
                toast(String.format("Can't select this space!"));
                effects.play(error, 1, 1, 1, 0, 1);
                return;
            }
        }
        //toast(String.format("Square selected: (%d, %d)", x, y));
        selected[0] = y;
        selected[1] = x;
    }

    /** Show a toast message. */
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** Set the width of the given button calculated from the screen size. */
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


    /** Spinner Callbacks */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        /* Use the following switch-statement if you want to keep all spinner actions/callbacks together */
        /* The same can be done to the onNothingSelected callback */
        switch(parent.getId()) {
            case R.id.spinner_difficulty:
                // 1 for difficulty spinner
                difficulty = position + 1;
                //board.difficulty = this.difficulty;
                board = new Board(size, difficulty);
                boardView = findViewById(R.id.boardView);
                boardView.setBoard(board);
                boardView.addSelectionListener(this::squareSelected);

                numberButtons = new ArrayList<>(numberIds.length);
                for (int i = 0; i < numberIds.length; i++) {
                    if(i <= 9) {
                        final int number = i; // 0 for delete button
                        View button = findViewById(numberIds[i]);
                        button.setOnClickListener(e -> numberClicked(number));
                        numberButtons.add(button);
                        setButtonWidth(button);
                    }
                    else if (i == 10){
                        View button = findViewById(numberIds[i]);
                        button.setOnClickListener(e -> solveClicked());
                        numberButtons.add(button);
                    }
                    else{
                        View button = findViewById(numberIds[i]);
                        button.setOnClickListener(e -> isSolvable());
                        numberButtons.add(button);
                    }
                }

                if (board.size == 4){
                    for (int i = 0; i < 5; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(true);
                    }
                    for (int i = 5; i < 10; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(false);
                    }
                    for (int i = 10; i < 12; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(true);
                    }
                }
                else{
                    for (int i = 0; i < numberIds.length; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(true);
                    }
                }

                hint = new ArrayList<>();
                for(int i = 0; i < board.size; i++){
                    for(int j = 0; j < board.size; j++){
                        if(board.player[i][j] > 0) {
                            int[] temp = {i, j};
                            hint.add(temp);
                        }
                    }
                }
                boardView.setHint(hint);
                boardView.postInvalidate();
                break;

            case R.id.spinner_size:
            //  Do stuff for board size spinner
                size = (position * 5) + 4;
                //board.size = this.size;
                board = new Board(size, difficulty);
                boardView = findViewById(R.id.boardView);
                boardView.setBoard(board);
                boardView.addSelectionListener(this::squareSelected);

                numberButtons = new ArrayList<>(numberIds.length);
                for (int i = 0; i < numberIds.length; i++) {
                    if(i <= 9) {
                        final int number = i; // 0 for delete button
                        View button = findViewById(numberIds[i]);
                        button.setOnClickListener(e -> numberClicked(number));
                        numberButtons.add(button);
                        setButtonWidth(button);
                    }
                    else if (i == 10){
                        View button = findViewById(numberIds[i]);
                        button.setOnClickListener(e -> solveClicked());
                        numberButtons.add(button);
                    }
                    else{
                        View button = findViewById(numberIds[i]);
                        button.setOnClickListener(e -> isSolvable());
                        numberButtons.add(button);
                    }
                }

                if (board.size == 4){
                    for (int i = 0; i < 5; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(true);
                    }
                    for (int i = 5; i < 10; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(false);
                    }
                    for (int i = 10; i < 12; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(true);
                    }
                }
                else{
                    for (int i = 0; i < numberIds.length; i++) {
                        View button = findViewById(numberIds[i]);
                        button.setEnabled(true);
                    }
                }

                hint = new ArrayList<>();
                for(int i = 0; i < board.size; i++){
                    for(int j = 0; j < board.size; j++){
                        if(board.player[i][j] > 0) {
                            int[] temp = {i, j};
                            hint.add(temp);
                        }
                    }
                }
                boardView.setHint(hint);
                boardView.postInvalidate();
                break;
        }
        //toast("Spinner1: position=" + position + ", size= " + size + "x" + size + ", d = " + difficulty);

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Toast.makeText(this, "You selected nothing", Toast.LENGTH_LONG).show();
    }

}
