package edu.asu.stratego.media;

import java.util.Hashtable;

public class HashImages
{

    private Hashtable<String, GamePiece> GameImages = new Hashtable<>();

    public Hashtable<String, GamePiece> getGameImages() {
        return GameImages;
    }

    public HashImages()
    {
        CargarLago();
        CargarCesped();
    }

    private void CargarLago()
    {
        for (int i = 0; i<4; i++)
        {
            GamePiece lago = new GamePiece("lake_" + i);
            GameImages.put(lago.getNombre(), lago);
        }
    }

    private void CargarCesped()
    {

    }
}
