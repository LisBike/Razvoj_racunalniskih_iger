package si.um.feri.game.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class ParkingIndicator extends Actor {
    private Texture squareTexture;

    public ParkingIndicator(float size, Color color) {
        squareTexture = Assets.createSquareTexture(size, color);
        setSize(size, size);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(squareTexture, getX(), getY(), getWidth(), getHeight());
    }
}

