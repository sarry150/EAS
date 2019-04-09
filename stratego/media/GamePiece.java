package edu.asu.stratego.media;

import javafx.scene.image.Image;

public class GamePiece
{

    private final String PATH_IMAGE = "images/";

    private Image Image;
    private String Nombre;

    public GamePiece(String nombre)
    {
        Nombre = nombre;
        Image = new Image(GamePiece.class.getResource(PATH_IMAGE + nombre).toString());
    }

    public javafx.scene.image.Image getImage() {
        return Image;
    }

    public String getNombre() {
        return Nombre;
    }
}
