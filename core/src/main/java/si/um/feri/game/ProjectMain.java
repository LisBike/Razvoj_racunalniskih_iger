package si.um.feri.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.IOException;
import java.net.MalformedURLException;

import si.um.feri.game.data.Station;
import si.um.feri.game.data.Stations;
import si.um.feri.game.utils.Assets;
import si.um.feri.game.utils.Geolocation;
import si.um.feri.game.utils.MapRasterTiles;
import si.um.feri.game.utils.PixelPosition;
import si.um.feri.game.utils.ZoomXY;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class ProjectMain extends ApplicationAdapter implements GestureDetector.GestureListener, InputProcessor {
	private SpriteBatch batch;
	private Sprite sprite;
	private ShapeRenderer shapeRenderer;
	private Vector3 touchPosition;
	public BitmapFont font;
	private Rectangle rect;
	private InputMultiplexer inputMultiplexer;

	private TiledMap tiledMap;
	private TiledMapRenderer tiledMapRenderer;
	private OrthographicCamera camera;

	private Texture[] mapTiles;
	private ZoomXY beginTile;   // top left tile

	private final int NUM_TILES = 3;
	private final int ZOOM = 14;
	private final Geolocation CENTER_GEOLOCATION = new Geolocation(46.554650, 15.644451);
	private final int WIDTH = MapRasterTiles.TILE_SIZE * NUM_TILES;
	private final int HEIGHT = MapRasterTiles.TILE_SIZE * NUM_TILES;

	// Stations
	Stations stations;
	private Stage stage;
	boolean isStation = false;
	String message = "";
	private Animation<TextureRegion> bikeAnimation;
	private float stateTime;

	@Override
	public void create() {
		Assets.load();
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		stage = new Stage();
		try {
			stations = new Stations("http://164.8.200.26:3000/locations");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		camera = new OrthographicCamera();
		camera.setToOrtho(false, WIDTH, HEIGHT);
		camera.position.set(WIDTH / 2f, HEIGHT / 2f, 0);
		camera.viewportWidth = WIDTH / 2f;
		camera.viewportHeight = HEIGHT / 2f;
		camera.zoom = 2f;
		camera.update();

		font = new BitmapFont();
		font.getData().setScale(2);

		inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);

		touchPosition = new Vector3();
		inputMultiplexer.addProcessor(new GestureDetector(this));
		Gdx.input.setInputProcessor(inputMultiplexer);

		try {
			//in most cases, geolocation won't be in the center of the tile because tile borders are predetermined (geolocation can be at the corner of a tile)
			ZoomXY centerTile = MapRasterTiles.getTileNumber(CENTER_GEOLOCATION.lat, CENTER_GEOLOCATION.lng, ZOOM);
			mapTiles = MapRasterTiles.getRasterTileZone(centerTile, NUM_TILES);
			//you need the beginning tile (tile on the top left corner) to convert geolocation to a location in pixels.
			beginTile = new ZoomXY(ZOOM, centerTile.x - ((NUM_TILES - 1) / 2), centerTile.y - ((NUM_TILES - 1) / 2));
		} catch (IOException e) {
			e.printStackTrace();
		}

		tiledMap = new TiledMap();
		MapLayers layers = tiledMap.getLayers();

		TiledMapTileLayer layer = new TiledMapTileLayer(NUM_TILES, NUM_TILES, MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE);
		int index = 0;
		for (int j = NUM_TILES - 1; j >= 0; j--) {
			for (int i = 0; i < NUM_TILES; i++) {
				TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
				cell.setTile(new StaticTiledMapTile(new TextureRegion(mapTiles[index], MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE)));
				layer.setCell(i, j, cell);
				index++;
			}
		}
		layers.add(layer);


		tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
		TextureRegion[] bikeFrames = new TextureRegion[2];
		bikeFrames[0] = new TextureRegion(new Texture(Gdx.files.internal("assets/bike1.png")));
		bikeFrames[1] = new TextureRegion(new Texture(Gdx.files.internal("assets/bike2.png")));


		bikeAnimation = new Animation<>(0.5f, bikeFrames);
		stateTime = 0f;
	}

	@Override
	public void render() {
		ScreenUtils.clear(0, 0, 0, 1);

		handleInput();

		camera.update();

		tiledMapRenderer.setView(camera);
		tiledMapRenderer.render();

		stage.act();
		stage.draw();
		Gdx.gl.glEnable(GL30.GL_BLEND);
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		if(isStation) {
			drawMessage();
		}
		shapeRenderer.end();

		Gdx.gl.glDisable(GL30.GL_BLEND);
		batch.setProjectionMatrix(camera.combined);

		batch.begin();
		updateBikeRotations(Gdx.graphics.getDeltaTime());
		drawStations();
		if(isStation) {
			font.setColor(Color.GREEN);
			font.draw(batch, message, WIDTH / 3f, 110);
		}
		batch.end();
	}

	private void drawStations() {
		for (int i = 0; i < stations.stationArray.size; i++) {
			Station station = stations.stationArray.get(i);
			PixelPosition marker = MapRasterTiles.getPixelPosition(station.latitude, station.longitude, MapRasterTiles.TILE_SIZE, ZOOM, beginTile.x, beginTile.y, HEIGHT);


			TextureRegion currentFrame = bikeAnimation.getKeyFrame(stateTime, true);


			batch.draw(currentFrame, marker.x - (currentFrame.getRegionWidth() / 2), marker.y - (currentFrame.getRegionHeight() / 2));
		}
	}
	private void updateBikeRotations(float deltaTime) {
		stateTime += deltaTime; // Update the state time for animation
	}


	private void handleInput() {
		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			camera.zoom += 0.02;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
			camera.zoom -= 0.02;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			camera.translate(-3, 0, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			camera.translate(3, 0, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			camera.translate(0, -3, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			camera.translate(0, 3, 0);
		}
		TiledMapTileLayer layer = (TiledMapTileLayer)tiledMap.getLayers().get(0); // get the first layer of the map

		if (Gdx.input.justTouched()) {
			isStation = false;
			Vector3 clickCoordinates = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
			Vector3 position = camera.unproject(clickCoordinates);
			int x = (int) (position.x / layer.getTileWidth());
			int y = (int)(position.y / layer.getTileHeight());

			inputMultiplexer.addProcessor(stage);

			for (Station station : stations.stationArray) {
				PixelPosition marker = MapRasterTiles.getPixelPosition(station.latitude, station.longitude, MapRasterTiles.TILE_SIZE, ZOOM, beginTile.x, beginTile.y, HEIGHT);

				// Check if the click coordinates are within the bounds of the bike
				if (isClickedOnBike(clickCoordinates.x, clickCoordinates.y, marker.x, marker.y, Assets.bikeImg.getWidth(), Assets.bikeImg.getHeight())) {
					isStation = true;
					message = showClickedMessage(station.name,station.stands);
				}
			}
		}


		camera.zoom = MathUtils.clamp(camera.zoom, 0.5f, 2f);

		float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
		float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

		camera.position.x = MathUtils.clamp(camera.position.x, effectiveViewportWidth / 2f, WIDTH - effectiveViewportWidth / 2f);
		camera.position.y = MathUtils.clamp(camera.position.y, effectiveViewportHeight / 2f, HEIGHT - effectiveViewportHeight / 2f);
	}
	private boolean isClickedOnBike(float clickX, float clickY, float bikeX, float bikeY, float bikeWidth, float bikeHeight) {
		return clickX >= bikeX - bikeWidth / 2 && clickX <= bikeX + bikeWidth / 2
				&& clickY >= bikeY - bikeHeight / 2 && clickY <= bikeY + bikeHeight / 2;
	}

	private void drawMessage() {
		shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.7f);
		rect = new Rectangle(WIDTH / 2f - 850 / 2f, 30, 850, 100);
		shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
	}
	private String showClickedMessage(String stationName, int stands) {
		@SuppressWarnings("DefaultLocale") String message = String.format("%s\nFREE PARKING: %s\n", stationName, stands);
		System.out.println(message);
		return message;
	}


	@Override
	public void dispose() {
		batch.dispose();
		shapeRenderer.dispose();
		Assets.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}

	@Override
	public boolean touchDown(float x, float y, int pointer, int button) {
		return false;
	}

	@Override
	public boolean tap(float x, float y, int count, int button) {
		return false;
	}

	@Override
	public boolean longPress(float x, float y) {
		return false;
	}

	@Override
	public boolean fling(float velocityX, float velocityY, int button) {
		return false;
	}

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		camera.translate(-deltaX, deltaY);
		return false;
	}

	@Override
	public boolean panStop(float x, float y, int pointer, int button) {
		return false;
	}

	@Override
	public boolean zoom(float initialDistance, float distance) {
		if (initialDistance >= distance)
			camera.zoom += 0.02;
		else
			camera.zoom -= 0.02;
		return false;
	}

	@Override
	public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
		return false;
	}

	@Override
	public void pinchStop() {

	}
}