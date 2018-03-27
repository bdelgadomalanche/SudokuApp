package edu.utep.cs.cs4330.sudoku.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/** An abstraction of Sudoku puzzle
 * @author Brandon Delgado Malanche
 */
public class Board {

    /** Difficulty modifier. */
    public int difficulty;

    /** Size of this board (number of columns/rows). */
    public int size;

    /** Value Randomizer */
    private Random rand = new Random();

    /** List of available spaces for board generation */
    private ArrayList<ArrayList<Integer>> Available = new ArrayList<ArrayList<Integer>>();

    /** Array Representation of Board*/
    private int[][] board;

    /** Player Board */
    public int[][] player;

    /** Create a new board of the given size. */
    public Board(int size, int difficulty) {
        this.size = size;
        this.difficulty = difficulty;
        // WRITE YOUR CODE HERE ...
        board = new int[size][size];
        int currentPos = 0;

        while(currentPos < (size * size)){
            if(currentPos == 0){
                clearGrid(board);
            }

            if(Available.get(currentPos).size() != 0){
                int i = rand.nextInt(Available.get(currentPos).size());
                int number = Available.get(currentPos).get(i);

                if(!checkConflict(board, currentPos, number)){
                    int xPos = currentPos % size;
                    int yPos = currentPos / size;

                    board[xPos][yPos] = number;

                    Available.get(currentPos).remove(i);

                    currentPos++;
                }
                else{
                    Available.get(currentPos).remove(i);
                }

            }
            else{
                for(int i = 1; i <= size; i++){
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
        int x = currentPos % size;
        int y = currentPos / size;

        if(checkHor(board, x, y, number) || checkVer(board, x, y, number) || checkArea(board, x, y, number) ){
            return true;
        }

        return false;
    }

    /** Calls other helper methods to check conflicts with other sections in the puzzle for gameplay purposes */
    public boolean checkConflict(int[][] board, int x, int y, int number){
        if(checkHor(board, x, y, number) || checkVer(board, x, y, number) || checkArea(board, x, y, number) ){
            Log.i("Check Sudoku", "True");
            return true;
        }
        Log.i("Check Sudoku", "False");
        return false;
    }

    /** Checks conflicts with other horizontal numbers */
    private boolean checkHor(int[][] board, int x, int y, int number){
        for( int i = size - 1; i >= 0 ; i--){
            if(board[x][i]==number)
            {
                return true;
            }
        }
        return false;
    }

    /** Checks conflicts with other vertical numbers */
    private boolean checkVer(int[][] board, int x, int y, int number){
        for( int i = size - 1; i >= 0 ; i--){
            if(board[i][y]==number)
            {
                return true;
            }
        }
        return false;
    }

    /** Checks conflicts with other numbers in the same square area */
    private boolean checkArea(int[][] board, int x, int y, int number){
        int xRegion = x / (int)Math.sqrt(size);
        int yRegion = y / (int)Math.sqrt(size);

        for(int i = xRegion * (int)Math.sqrt(size) ; i < xRegion * (int)Math.sqrt(size) + (int)Math.sqrt(size); i++){
            for(int j = yRegion * (int)Math.sqrt(size) ; j < yRegion * (int)Math.sqrt(size) + (int)Math.sqrt(size); j++){
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

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                board[j][i] = -1;
            }
        }

        for(int i = 0; i < (size*size); i++){
            Available.add(new ArrayList<Integer>());
            for(int j = 1; j <= size; j++){
                Available.get(i).add(j);
            }
        }
    }

    /** Removes elements from the player board so the board does not display the complete solution */
    public int[][] removeElements(int[][] board){
        int i = 0;
        int toRemove = 50;

        if (difficulty == 1){
            toRemove = (int)(Math.pow(size, 2) * .6);
        }
        else if (difficulty == 2){
            toRemove = (int)(Math.pow(size, 2) * .7);
        }
        else if (difficulty == 3){
            toRemove = (int)(Math.pow(size, 2) * .8);
        }


        while(i < toRemove){
            int x = rand.nextInt(size);
            int y = rand.nextInt(size);

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

    /** Checks for win by checking that all spaces have been filled */
    public boolean puzzleSolved(){
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(player[i][j] < 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Checks if user solution is similar to the real solution to determine if the puzzle is still solvable **/
    public boolean solvable(){
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if((!(player[i][j] == board[i][j])) && player[i][j] > 0) {
                    return false;
                }
            }
        }
        return true;
    }


    public int[][] copyBoard(int[][] copy, int[][] original){
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                copy[i][j] = original[i][j];
            }
        }
        return copy;
    }

    //TODO: add return for checking if it solved the player board
    /** Solve the Sudoku puzzle for the user **/
    public boolean solveForUser(boolean willSolve) {
        int [][] tempBoard = new int[board.length][board[0].length];
        tempBoard = copyBoard(tempBoard, player);
        boolean solved = solveForUserHelper(tempBoard);

        // Solve player board if player chose to solve & solution found
        if(willSolve && solved) {
            copyBoard(player, tempBoard);
        }
        return solved;

    }

    public boolean solveForUserHelper(int[][] s) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (s[i][j] != 0) {
                    continue;
                }
                for (int num = 1; num <= size; num++) {
                    if (!checkConflict(s, i, j, num)) {
                        s[i][j] = num;
                        if (solveForUserHelper(s)) {
                            return true;
                        } else {
                            s[i][j] = 0;
                        }
                    }
                }
                return false;
            }
        }

        return true;
    }

}
