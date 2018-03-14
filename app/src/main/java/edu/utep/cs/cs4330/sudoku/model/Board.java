package edu.utep.cs.cs4330.sudoku.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/** An abstraction of Sudoku puzzle
 * @author Brandon Delgado Malanche
 */
public class Board {

    /** Size of this board (number of columns/rows). */
    public final int size;

    /** Value Randomizer */
    private Random rand = new Random();

    /** List of available spaces for board generation */
    private ArrayList<ArrayList<Integer>> Available = new ArrayList<ArrayList<Integer>>();

    /** Array Representation of Board*/
    private int[][] board;

    /** Player Board */
    public int[][] player;

    /** Create a new board of the given size. */
    public Board(int size) {
        this.size = size;
        // WRITE YOUR CODE HERE ...
        board = new int[size][size];
        int currentPos = 0;

        while(currentPos < 81){
            if(currentPos == 0){
                clearGrid(board);
            }

            if(Available.get(currentPos).size() != 0){
                int i = rand.nextInt(Available.get(currentPos).size());
                int number = Available.get(currentPos).get(i);

                if(!checkConflict(board, currentPos, number)){
                    int xPos = currentPos % 9;
                    int yPos = currentPos / 9;

                    board[xPos][yPos] = number;

                    Available.get(currentPos).remove(i);

                    currentPos++;
                }
                else{
                    Available.get(currentPos).remove(i);
                }

            }
            else{
                for(int i = 1; i <= 9; i++){
                    Available.get(currentPos).add(i);
                }
                currentPos--;
            }
        }

        createPlayerBoard();

    }

    /** Return the size of this board. */
    public int size() {
        return size;
    }

    // WRITE YOUR CODE HERE ..

    /** Calls other helper methods to check conflicts with other sections in the puzzle for board generation */
    private boolean checkConflict(int[][] board, int currentPos , int number){
        int x = currentPos % 9;
        int y = currentPos / 9;

        if(checkHor(board, x, y, number) || checkVer(board, x, y, number) || checkArea(board, x, y, number) ){
            return true;
        }

        return false;
    }

    /** Calls other helper methods to check conflicts with other sections in the puzzle for gameplay purposes */
    public boolean checkConflict(int[][] board, int x, int y, int number){
        if(checkHor(board, x, y, number) || checkVer(board, x, y, number) || checkArea(board, x, y, number) ){
            return true;
        }

        return false;
    }

    /** Checks conflicts with other horizontal numbers */
    private boolean checkHor(int[][] board, int x, int y, int number){
        for( int i = x - 1; i >= 0 ; i--){
            if(number == board[i][y]){
                return true;
            }
        }

        return false;
    }

    /** Checks conflicts with other vertical numbers */
    private boolean checkVer(int[][] board, int x, int y, int number){
        for( int i = y - 1; i >= 0 ; i--){
            if(number == board[x][i]){
                return true;
            }
        }

        return false;
    }

    /** Checks conflicts with other numbers in the same square area */
    private boolean checkArea(int[][] board, int x, int y, int number){
        int xRegion = x / 3;
        int yRegion = y / 3;

        for(int i = xRegion * 3 ; i < xRegion * 3 + 3 ; i++){
            for(int j = yRegion * 3 ; j < yRegion * 3 + 3 ; j++){
                if ((i != x || j != y) && number == board[i][j]) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Clears grid by initializing all spaces at -1 for easier board manipulation */
    private void clearGrid(int[][] board){
        Available.clear();

        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                board[j][i] = -1;
            }
        }

        for(int i = 0; i < 81; i++){
            Available.add(new ArrayList<Integer>());
            for(int j = 1; j <= 9; j++){
                Available.get(i).add(j);
            }
        }
    }

    /** Removes elements from the player board so the board does not display the complete solution */
    public int[][] removeElements(int[][] board){
        int i = 0;

        while(i < 50){
            int x = rand.nextInt(9);
            int y = rand.nextInt(9);

            if(board[x][y] != 0){
                board[x][y] = 0;
                i++;
            }
        }
        return board;

    }

    /** Creates a copy of the solution board to keep track of player's progress */
    public void createPlayerBoard(){
        player = new int[board.length][board[0].length];
        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < board[i].length; j++){
                player[i][j] = board[i][j];
            }
        }

        player = removeElements(player);
    }

    /** Removes number from selected position */
    public void removeFromPlayer(int x, int y){
        player[x][y] = 0;
    }

    /** Checks for win by comparing all elements from the solution array to the player array */
    public boolean puzzleSolved(){
        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                if(!(player[i][j] == board[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }

}
