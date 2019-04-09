package edu.asu.stratego.game;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class GamePlayer {

    private ObjectOutputStream toPlayer;
    private ObjectInputStream fromPlayer;
    private Player player = new Player();
    private Point point;
    private SetupBoard board;
    private GameStatus gameStatus;
    private PieceColor turn;

    public void setOppositePlayer(Player player) throws IOException {
        toPlayer.writeObject(player);
    }

    public void setToPlayer(Socket socket) throws IOException {
        toPlayer = new ObjectOutputStream(socket.getOutputStream());
    }

    public void setFromPlayer(Socket socket) throws IOException {
        fromPlayer = new ObjectInputStream(socket.getInputStream());
    }

    public void setPlayer() throws IOException, ClassNotFoundException {
        this.player = (Player) fromPlayer.readObject();
    }

    public Player getPlayer()
    {
        return player;
    }

    public SetupBoard getBoard() {
        return board;
    }

    public void setBoard() throws IOException, ClassNotFoundException {
        board = (SetupBoard)fromPlayer.readObject();
        this.board = board;
    }

    public void setBoard(SetupBoard board)
    {
        this.board = board;
    }

    public void setColor(PieceColor color) {
        if (player == null) return;
        player.setColor(color);
    }

    public void setGameStatus(GameStatus gameStatus)
    {
        this.gameStatus = gameStatus;
    }

    public void setPoint(Point point)
    {
        this.point = point;
    }

    public Point getPoint() {
        return point;
    }

    public void setTurn(PieceColor turn) {
        this.turn = turn;
    }

    public Move getMove() throws IOException, ClassNotFoundException {
        return (Move)fromPlayer.readObject();
    }

    public void setMove(Move move) throws IOException {
        toPlayer.writeObject(move);
    }
}
