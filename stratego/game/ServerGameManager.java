package edu.asu.stratego.game;

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import edu.asu.stratego.game.board.ServerBoard;

/**
 * Task to manage a Stratego game between two clients.
 */
public class ServerGameManager implements Runnable {

    private final String session;

    private ServerBoard board = new ServerBoard();

    //Nuevos metodos;
    private GamePlayer gamePlayerOne = new GamePlayer();
    private GamePlayer gamePlayerTwo = new GamePlayer();

    private PieceColor turn;
    private Move move;

    private Socket socketOne;
    private Socket socketTwo;

    /**
     * Creates a new instance of ServerGameManager.
     *
     * @param sockOne    socket connected to Player 1's client
     * @param sockTwo    socket connected to Player 2's client
     * @param sessionNum the nth game session created by Server.
     * @see edu.asu.stratego.Server
     */
    public ServerGameManager(Socket sockOne, Socket sockTwo, int sessionNum) {
        this.session = "Session " + sessionNum + ": ";
        this.socketOne = sockOne;
        this.socketTwo = sockTwo;

        if (Math.random() < 0.5)
            this.turn = PieceColor.RED;
        else
            this.turn = PieceColor.BLUE;
    }

    /**
     * See ClientGameManager's run() method to understand how the server
     * interacts with the client.
     *
     * @see ClientGameManager
     */
    @Override
    public void run() {
        createIOStreams();
        exchangePlayers();
        exchangeSetup();

        playGame();
    }

    /**
     * Establish IO object streams to facilitate communication between the
     * client and server.
     */
    private void createIOStreams() {
        try {
            // NOTE: ObjectOutputStreams must be constructed before the 
            //       ObjectInputStreams so as to prevent a remote deadlock.
            gamePlayerOne.setToPlayer(socketOne);
            gamePlayerOne.setFromPlayer(socketOne);
            gamePlayerTwo.setToPlayer(socketTwo);
            gamePlayerTwo.setFromPlayer(socketTwo);
        } catch (IOException e) {
            // TODO Handle this exception somehow...
            e.printStackTrace();
        }
    }

    /**
     * Receive player information from the clients. Determines the players'
     * colors, and sends the player information of the opponents back to the
     * clients.
     */
    private void exchangePlayers() {
        try {
            gamePlayerOne.setPlayer();
            gamePlayerTwo.setPlayer();

            if (Math.random() < 0.5) {
                gamePlayerOne.setColor(PieceColor.RED);
                gamePlayerTwo.setColor(PieceColor.BLUE);
            } else {
                gamePlayerOne.setColor(PieceColor.BLUE);
                gamePlayerTwo.setColor(PieceColor.RED);
            }

            gamePlayerOne.setOppositePlayer(gamePlayerTwo.getPlayer());
            gamePlayerTwo.setOppositePlayer(gamePlayerOne.getPlayer());

        } catch (ClassNotFoundException | IOException e) {
            // TODO Handle this exception somehow...
            e.printStackTrace();
        }
    }

    private void exchangeSetup() {
        try {
            gamePlayerOne.setBoard();
            gamePlayerTwo.setBoard();

            // Register pieces on the server board.
            for (int row = 0; row < 4; ++row) {
                for (int col = 0; col < 10; ++col) {
                    board.getSquare(row, col).setPiece(gamePlayerOne.getBoard().getPiece(3 - row, 9 - col));
                    board.getSquare(row + 6, col).setPiece(gamePlayerTwo.getBoard().getPiece(row, col));

                    if (gamePlayerOne.getBoard().getPiece(3 - row, 9 - col).getPieceType() == PieceType.FLAG)
                    { gamePlayerOne.setPoint(new Point(row, col));}
                    if (gamePlayerOne.getBoard().getPiece(row, col).getPieceType() == PieceType.FLAG)
                    {gamePlayerTwo.setPoint(new Point(row, col));}

                }
            }

            // Rotate pieces by 180 degrees.
            for (int row = 0; row < 2; ++row) {
                for (int col = 0; col < 10; ++col) {
                    // Player One
                    Piece temp = gamePlayerOne.getBoard().getPiece(row, col);
                    gamePlayerOne.getBoard().setPiece(gamePlayerOne.getBoard().getPiece(3 - row, 9 - col), row, col);
                    gamePlayerOne.getBoard().setPiece(temp, 3 - row, 9 - col);

                    // Player Two
                    temp = gamePlayerTwo.getBoard().getPiece(row, col);
                    gamePlayerTwo.getBoard().setPiece(gamePlayerTwo.getBoard().getPiece(3 - row, 9 - col), row, col);
                    gamePlayerTwo.getBoard().setPiece(temp, 3 - row, 9 - col);
                }
            }

            GameStatus winCondition = checkWinCondition();

            gamePlayerOne.setBoard(gamePlayerTwo.getBoard());
            gamePlayerTwo.setBoard(gamePlayerOne.getBoard());

            gamePlayerOne.setGameStatus(winCondition);
            gamePlayerTwo.setGameStatus(winCondition);
        } catch (ClassNotFoundException | IOException e) {
            // TODO Handle this exception somehow...
            e.printStackTrace();
        }

    }

    private void playGame() {
        while (true) {
            try {
                // Send player turn color to clients.
                gamePlayerOne.setTurn(turn);
                gamePlayerTwo.setTurn(turn);

                // Get turn from client.
                if (gamePlayerOne.getPlayer().getColor() == turn) {
                    move = gamePlayerOne.getMove();
                    move.setStart(9 - move.getStart().x, 9 - move.getStart().y);
                    move.setEnd(9 - move.getEnd().x, 9 - move.getEnd().y);
                } else {
                    move = gamePlayerTwo.getMove();
                }

                Move moveToPlayerOne = new Move(), moveToPlayerTwo = new Move();

                // Register move on the board.
                // If there is no piece at the end (normal move, no attack)
                if (board.getSquare(move.getEnd().x, move.getEnd().y).getPiece() == null) {
                    Piece piece = board.getSquare(move.getStart().x, move.getStart().y).getPiece();

                    board.getSquare(move.getStart().x, move.getStart().y).setPiece(null);
                    board.getSquare(move.getEnd().x, move.getEnd().y).setPiece(piece);

                    // Rotate the move 180 degrees before sending.
                    moveToPlayerOne.setStart(new Point(9 - move.getStart().x, 9 - move.getStart().y));
                    moveToPlayerOne.setEnd(new Point(9 - move.getEnd().x, 9 - move.getEnd().y));
                    moveToPlayerOne.setMoveColor(move.getMoveColor());
                    moveToPlayerOne.setStartPiece(null);
                    moveToPlayerOne.setEndPiece(piece);

                    moveToPlayerTwo.setStart(new Point(move.getStart().x, move.getStart().y));
                    moveToPlayerTwo.setEnd(new Point(move.getEnd().x, move.getEnd().y));
                    moveToPlayerTwo.setMoveColor(move.getMoveColor());
                    moveToPlayerTwo.setStartPiece(null);
                    moveToPlayerTwo.setEndPiece(piece);
                } else {
                    Piece attackingPiece = board.getSquare(move.getStart().x, move.getStart().y).getPiece();
                    Piece defendingPiece = board.getSquare(move.getEnd().x, move.getEnd().y).getPiece();

                    BattleOutcome outcome = attackingPiece.getPieceType().attack(defendingPiece.getPieceType());

                    moveToPlayerOne.setAttackMove(true);
                    moveToPlayerTwo.setAttackMove(true);

                    if (outcome == BattleOutcome.WIN) {
                        board.getSquare(move.getEnd().x, move.getEnd().y).setPiece(board.getSquare(move.getStart().x, move.getStart().y).getPiece());
                        board.getSquare(move.getStart().x, move.getStart().y).setPiece(null);

                        // Rotate the move 180 degrees before sending.
                        moveToPlayerOne.setStart(new Point(9 - move.getStart().x, 9 - move.getStart().y));
                        moveToPlayerOne.setEnd(new Point(9 - move.getEnd().x, 9 - move.getEnd().y));
                        moveToPlayerOne.setMoveColor(move.getMoveColor());
                        moveToPlayerOne.setStartPiece(null);
                        moveToPlayerOne.setEndPiece(attackingPiece);
                        moveToPlayerOne.setAttackWin(true);
                        moveToPlayerOne.setDefendWin(false);

                        moveToPlayerTwo.setStart(new Point(move.getStart().x, move.getStart().y));
                        moveToPlayerTwo.setEnd(new Point(move.getEnd().x, move.getEnd().y));
                        moveToPlayerTwo.setMoveColor(move.getMoveColor());
                        moveToPlayerTwo.setStartPiece(null);
                        moveToPlayerTwo.setEndPiece(attackingPiece);
                        moveToPlayerTwo.setAttackWin(true);
                        moveToPlayerTwo.setDefendWin(false);
                    } else if (outcome == BattleOutcome.LOSE) {
                        board.getSquare(move.getStart().x, move.getStart().y).setPiece(null);

                        // Rotate the move 180 degrees before sending.
                        moveToPlayerOne.setStart(new Point(9 - move.getStart().x, 9 - move.getStart().y));
                        moveToPlayerOne.setEnd(new Point(9 - move.getEnd().x, 9 - move.getEnd().y));
                        moveToPlayerOne.setMoveColor(move.getMoveColor());
                        moveToPlayerOne.setStartPiece(null);
                        moveToPlayerOne.setEndPiece(defendingPiece);
                        moveToPlayerOne.setAttackWin(false);
                        moveToPlayerOne.setDefendWin(true);

                        moveToPlayerTwo.setStart(new Point(move.getStart().x, move.getStart().y));
                        moveToPlayerTwo.setEnd(new Point(move.getEnd().x, move.getEnd().y));
                        moveToPlayerTwo.setMoveColor(move.getMoveColor());
                        moveToPlayerTwo.setStartPiece(null);
                        moveToPlayerTwo.setEndPiece(defendingPiece);
                        moveToPlayerTwo.setAttackWin(false);
                        moveToPlayerTwo.setDefendWin(true);
                    } else if (outcome == BattleOutcome.DRAW) {
                        board.getSquare(move.getStart().x, move.getStart().y).setPiece(null);
                        board.getSquare(move.getEnd().x, move.getEnd().y).setPiece(null);

                        // Rotate the move 180 degrees before sending.
                        moveToPlayerOne.setStart(new Point(9 - move.getStart().x, 9 - move.getStart().y));
                        moveToPlayerOne.setEnd(new Point(9 - move.getEnd().x, 9 - move.getEnd().y));
                        moveToPlayerOne.setMoveColor(move.getMoveColor());
                        moveToPlayerOne.setStartPiece(null);
                        moveToPlayerOne.setEndPiece(null);
                        moveToPlayerOne.setAttackWin(false);
                        moveToPlayerOne.setDefendWin(false);

                        moveToPlayerTwo.setStart(new Point(move.getStart().x, move.getStart().y));
                        moveToPlayerTwo.setEnd(new Point(move.getEnd().x, move.getEnd().y));
                        moveToPlayerTwo.setMoveColor(move.getMoveColor());
                        moveToPlayerTwo.setStartPiece(null);
                        moveToPlayerTwo.setEndPiece(null);
                        moveToPlayerTwo.setAttackWin(false);
                        moveToPlayerTwo.setDefendWin(false);
                    }
                }

                GameStatus winCondition = checkWinCondition();


                gamePlayerOne.setMove(moveToPlayerOne);
                gamePlayerTwo.setMove(moveToPlayerTwo);

                gamePlayerOne.setGameStatus(winCondition);
                gamePlayerTwo.setGameStatus(winCondition);

                // Change turn color.
                if (turn == PieceColor.RED)
                    turn = PieceColor.BLUE;
                else
                    turn = PieceColor.RED;

                // Check win conditions.
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(session + "Error occured during network I/O");
                return;
            }
        }
    }

    private GameStatus checkWinCondition() {
        if (!hasAvailableMoves(PieceColor.RED))
            return GameStatus.RED_NO_MOVES;

        else if (isCaptured(PieceColor.RED))
            return GameStatus.RED_CAPTURED;

        if (!hasAvailableMoves(PieceColor.BLUE))
            return GameStatus.BLUE_NO_MOVES;

        else if (isCaptured(PieceColor.BLUE))
            return GameStatus.BLUE_CAPTURED;

        return GameStatus.IN_PROGRESS;
    }

    private boolean isCaptured(PieceColor inColor) {
        if (gamePlayerOne.getPlayer().getColor() == inColor) {
            if (board.getSquare(gamePlayerOne.getPoint().x, gamePlayerOne.getPoint().y).getPiece().getPieceType() != PieceType.FLAG)
                return true;
        }
        if (gamePlayerTwo.getPlayer().getColor() == inColor) {
            if (board.getSquare(gamePlayerTwo.getPoint().x, gamePlayerTwo.getPoint().y).getPiece().getPieceType() != PieceType.FLAG)
                return true;
        }
        return false;
    }

    private boolean hasAvailableMoves(PieceColor inColor) {
        for (int row = 0; row < 10; ++row) {
            for (int col = 0; col < 10; ++col) {
                if (board.getSquare(row, col).getPiece() != null && board.getSquare(row, col).getPiece().getPieceColor() == inColor) {
                    if (computeValidMoves(row, col, inColor).size() > 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private ArrayList<Point> computeValidMoves(int row, int col, PieceColor inColor) {
        int max = 1;
        PieceType pieceType = board.getSquare(row, col).getPiece().getPieceType();
        if (pieceType == PieceType.SCOUT)
            max = 8;

        ArrayList<Point> validMoves = new ArrayList<Point>();

        if (pieceType != PieceType.BOMB && pieceType != PieceType.FLAG) {
            // Negative Row (UP)
            for (int i = -1; i >= -max; --i) {
                if (isInBounds(row + i, col) && (!isLake(row + i, col) || (!isNullPiece(row + i, col) && !isOpponentPiece(row + i, col, inColor)))) {
                    if (isNullPiece(row + i, col) || isOpponentPiece(row + i, col, inColor)) {
                        validMoves.add(new Point(row + i, col));

                        if (!isNullPiece(row + i, col) && isOpponentPiece(row + i, col, inColor))
                            break;
                    } else
                        break;
                } else
                    break;
            }
            // Positive Col (RIGHT)
            for (int i = 1; i <= max; ++i) {
                if (isInBounds(row, col + i) && (!isLake(row, col + i) || (!isNullPiece(row, col + i) && !isOpponentPiece(row, col + i, inColor)))) {
                    if (isNullPiece(row, col + i) || isOpponentPiece(row, col + i, inColor)) {
                        validMoves.add(new Point(row, col + i));

                        if (!isNullPiece(row, col + i) && isOpponentPiece(row, col + i, inColor))
                            break;
                    } else
                        break;
                } else
                    break;
            }
            // Positive Row (DOWN)
            for (int i = 1; i <= max; ++i) {
                if (isInBounds(row + i, col) && (!isLake(row + i, col) || (!isNullPiece(row + i, col) && !isOpponentPiece(row + i, col, inColor)))) {
                    if (isNullPiece(row + i, col) || isOpponentPiece(row + i, col, inColor)) {
                        validMoves.add(new Point(row + i, col));

                        if (!isNullPiece(row + i, col) && isOpponentPiece(row + i, col, inColor))
                            break;
                    } else
                        break;
                } else
                    break;
            }
            // Negative Col (LEFT)
            for (int i = -1; i >= -max; --i) {
                if (isInBounds(row, col + i) && (!isLake(row, col + i) || (!isNullPiece(row, col + i) && !isOpponentPiece(row, col + i, inColor)))) {
                    if (isNullPiece(row, col + i) || isOpponentPiece(row, col + i, inColor)) {
                        validMoves.add(new Point(row, col + i));

                        if (!isNullPiece(row, col + i) && isOpponentPiece(row, col + i, inColor))
                            break;
                    } else
                        break;
                } else
                    break;
            }
        }

        return validMoves;
    }

    private static boolean isLake(int row, int col) {
        if (col == 2 || col == 3 || col == 6 || col == 7) {
            if (row == 4 || row == 5)
                return true;
        }
        return false;
    }

    private static boolean isInBounds(int row, int col) {
        if (row < 0 || row > 9)
            return false;
        if (col < 0 || col > 9)
            return false;

        return true;
    }

    private boolean isOpponentPiece(int row, int col, PieceColor inColor) {
        return board.getSquare(row, col).getPiece().getPieceColor() != inColor;
    }

    private boolean isNullPiece(int row, int col) {
        return board.getSquare(row, col).getPiece() == null;
    }
}