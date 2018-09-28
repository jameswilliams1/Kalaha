package ai;

import ai.Global;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;

import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 *
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable {
    private int player;
    private JTextArea text;

    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;

    /**
     * Creates a new client.
     */
    public AIClient() {
        player = -1;
        connected = false;

        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();

        try {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        } catch (Exception ex) {
            addText("Unable to connect to server");
            return;
        }
    }

    /**
     * Starts the client thread.
     */
    public void start() {
        //Don't change this
        if (connected) {
            thr = new Thread(this);
            thr.start();
        }
    }

    /**
     * Creates the GUI.
     */
    private void initGUI() {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420, 250));
        frame.getContentPane().setLayout(new FlowLayout());

        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));

        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setVisible(true);
    }

    /**
     * Adds a text string to the GUI textarea.
     *
     * @param txt The text to add
     */
    public void addText(String txt) {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }

    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run() {
        String reply;
        running = true;

        try {
            while (running) {
                //Checks which player you are. No need to change this.
                if (player == -1) {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);

                    addText("I am player " + player);
                }

                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if (reply.equals("1") || reply.equals("2")) {
                    int w = Integer.parseInt(reply);
                    if (w == player) {
                        addText("I won!");
                    } else {
                        addText("I lost...");
                    }
                    running = false;
                }
                if (reply.equals("0")) {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running) {
                    int nextPlayer = Integer.parseInt(reply);

                    if (nextPlayer == player) {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove) {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);

                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double) tot / (double) 1000;

                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR")) {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }

                //Wait
                Thread.sleep(100);
            }
        } catch (Exception ex) {
            running = false;
        }

        try {
            socket.close();
            addText("Disconnected from server");
        } catch (Exception ex) {
            addText("Error closing connection: " + ex.getMessage());
        }
    }

    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     *
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard) {

        int bestMove = 0;
        int maxEval = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        //Find move with max eval score at depth (ai score - opponent score)
        for (int move = 1; move <= 6; move++) {
            if (currentBoard.moveIsPossible(move)) {
                GameState copy = currentBoard.clone();
                copy.makeMove(move);
                int eval = miniMax(copy, 5, alpha, beta, true);
                if (eval > maxEval) {
                    maxEval = eval;
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }

    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     *
     * @return Random ambo number
     */
    public int getRandom() {
        return 1 + (int) (Math.random() * 6);
    }


    /**
     * @param position  A GameState to be evaluated
     * @param depth     Depth of recursion to use
     * @param maxPlayer True if player is trying to maximise the evaluation
     * @param alpha     Int must be set to -inf (for pruning)
     * @param beta      Int must be set to +inf (for pruning)
     * @return eval The maximum evaluation score found from all possible games
     */
    public int miniMax(GameState position, int depth, int alpha, int beta, boolean maxPlayer) {
        //Determine other player's number
        int otherPlayer = 1;
        if (player == 1) {
            otherPlayer = 2;
        }
        //Return evaluation (AI score - opponent score) if end of tree reached or game ended
        if (depth == 0 || position.gameEnded()) {
            int eval = position.getScore(player) - position.getScore(otherPlayer);
            return eval;
        }

        //Finds max eval out of children
        if (maxPlayer) {
            int maxEval = Integer.MIN_VALUE;
            //Finds possible moves of parent then creates game copies and makes moves
            for (int move = 1; move <= 6; move++) {
                if (position.moveIsPossible(move)) {
                    GameState thisPosition = position.clone();
                    thisPosition.makeMove(move);
                    //Finds minimax of all children
                    int eval = miniMax(thisPosition, depth - 1, alpha, beta, AITurn(thisPosition));
                    if (eval > maxEval) {
                        maxEval = eval;
                    }
                    if (eval > alpha) {
                        alpha = eval;
                    }
                    if (beta <= alpha) {
                        break;
                    }
                }

            }
            return maxEval;
            //Finds min eval out of children
        } else {
            int minEval = Integer.MAX_VALUE;
            //Finds possible moves of parent then creates game copies and makes moves
            for (int move = 1; move <= 6; move++) {
                if (position.moveIsPossible(move)) {
                    GameState thisPosition = position.clone();
                    thisPosition.makeMove(move);
                    //Finds minimax of all children
                    int eval = miniMax(thisPosition, depth - 1, alpha, beta, AITurn(thisPosition));
                    if (eval < minEval) {
                        minEval = eval;
                    }
                    if (eval < beta) {
                        beta = eval;
                    }
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            return minEval;
        }

    }

    /**
     * Determines whether it is the AI's turn
     *
     * @param position (GameState)
     * @return true/false
     */
    public boolean AITurn(GameState position) {
        if (position.getNextPlayer() == player) {
            return true;
        } else {
            return false;
        }
    }
}