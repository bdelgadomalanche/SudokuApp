package edu.utep.cs.cs4330.sudoku;

import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
public class MainActivity extends AppCompatActivity {

    private Board board;

    private BoardView boardView;

    /** All the number buttons. */
    private List<View> numberButtons;
    private static final int[] numberIds = new int[] {
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4,
            R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9
    };

    /** Array to remember where to insert number */
    private int[] selected = {-1, -1};

    /** Unmodifiable spaces */
    private ArrayList<int[]> hint;

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

        board = new Board(9);
        boardView = findViewById(R.id.boardView);
        boardView.setBoard(board);
        boardView.addSelectionListener(this::squareSelected);

        numberButtons = new ArrayList<>(numberIds.length);
        for (int i = 0; i < numberIds.length; i++) {
            final int number = i; // 0 for delete button
            View button = findViewById(numberIds[i]);
            button.setOnClickListener(e -> numberClicked(number));
            numberButtons.add(button);
            setButtonWidth(button);
        }
        hint = new ArrayList<>();
        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
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
    }

    /** Callback to be invoked when the new button is tapped. */
    public void newClicked(View view) {
        // WRITE YOUR CODE HERE ...
        //
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
        toast(String.format("Square selected: (%d, %d)", x, y));
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
}
