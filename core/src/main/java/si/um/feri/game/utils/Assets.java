package si.um.feri.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Assets {

    public static Texture bikeImg;
    public static Texture bike1Img;
    public static Texture bike2Img;
    public static Skin skin;

    public static void load() {
        bikeImg = new Texture("bike.png");
        bike1Img=new Texture("bike1.png");
        bike1Img=new Texture("bike2.png");

        //skin = new Skin(Gdx.files.internal("neon_skin/neon-ui.json"));
        skin = new Skin(Gdx.files.internal("default-skin/uiskin.json"));
    }

    public static void dispose() {
        bikeImg.dispose();
    }
}