package net.bajobongo.ofg;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Main extends ApplicationAdapter {
    private TiledMapRenderer renderer;
    private FitViewport viewport;

    // view:
    private TiledMapTileLayer racket;
    private TiledMapTileLayer ball;

    // model
    private Map<TiledMapTileLayer.Cell, Rectangle> collideables;

    private Rectangle racketPosition = new Rectangle(10, 0, 24, 8);
    private Rectangle ballPosition = new Rectangle(60, 60, 8, 8);
    private Vector2 ballVelocity = new Vector2(30, 30);

    private boolean collidesWithRacket = false;


    @Override
    public void create() {
        TiledMap tiledMap = new TmxMapLoader().load("breakout/breakout.tmx");
        TiledMapTileLayer backgroundLayer = (TiledMapTileLayer) tiledMap.getLayers().get("background");
        racket = (TiledMapTileLayer) tiledMap.getLayers().get("racket");
        ball = (TiledMapTileLayer) tiledMap.getLayers().get("ball");

        retrieveCollideables(tiledMap);

        renderer = new OrthogonalTiledMapRenderer(tiledMap);
        viewport = new FitViewport(backgroundLayer.getWidth() * backgroundLayer.getTileWidth(),
            backgroundLayer.getHeight() * backgroundLayer.getTileHeight(),
            new OrthographicCamera());
    }

    private void retrieveCollideables(TiledMap tiledMap) {
        collideables = new HashMap<>();

        TiledMapTileLayer collisionsLayer = (TiledMapTileLayer) tiledMap.getLayers().get("things");
        int gridTileWidth = collisionsLayer.getTileWidth();
        int gridTileHeight = collisionsLayer.getTileHeight();
        for (int row = 0; row < collisionsLayer.getHeight(); row++) {
            for (int col = 0; col < collisionsLayer.getWidth(); col++) {
                TiledMapTileLayer.Cell cell = collisionsLayer.getCell(col, row);

                if (cell == null) continue;

                TextureRegion cellTextureRegion = cell.getTile().getTextureRegion();
                Rectangle cellRectangle = new Rectangle(
                    col * gridTileWidth - collisionsLayer.getOffsetX(),
                    row * gridTileHeight + collisionsLayer.getOffsetY(),
                    cellTextureRegion.getRegionWidth(),
                    cellTextureRegion.getRegionHeight());

                collideables.put(cell, cellRectangle);
            }
        }
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        if (Gdx.input.isTouched()) {
            Vector3 res = viewport.unproject(new Vector3(Gdx.input.getX(), 0, 0));
            racketPosition.x = res.x - racketPosition.getWidth() / 2;
        }

        Rectangle tmpBallPosition = new Rectangle(ballPosition);

        tmpBallPosition.y += ballVelocity.y * deltaTime;

        boolean bounceY = move(tmpBallPosition);
        if (bounceY) {
            ballVelocity.y = -ballVelocity.y;
            tmpBallPosition.y = ballPosition.y;
        }

        if (!collidesWithRacket && tmpBallPosition.overlaps(racketPosition)) {
            collidesWithRacket = true;

            float racketCenter = racketPosition.x + racketPosition.getWidth() / 2;
            float ballCenter = ballPosition.x + ballPosition.getWidth() / 2;

            float ratio = (ballCenter - racketCenter) / (racketPosition.width / 2);

            ballVelocity.y = -ballVelocity.y;
            tmpBallPosition.y = racketPosition.y + racketPosition.getHeight();
            ballVelocity.x = ballVelocity.x + (ratio * 20);
        } else {
            collidesWithRacket = false;
        }

        tmpBallPosition.x += ballVelocity.x * deltaTime;
        boolean bounceX = move(tmpBallPosition);

        if (bounceX) {
            ballVelocity.x = -ballVelocity.x;
            tmpBallPosition.x = ballPosition.x;
        }

        ballPosition.set(tmpBallPosition);

        alignLayer(ball, ballPosition);
        alignLayer(racket, racketPosition);

        renderer.setView((OrthographicCamera) viewport.getCamera());
        renderer.render();
    }

    private boolean move(Rectangle tmpBallPosition) {
        boolean collision = false;
        for (Map.Entry<TiledMapTileLayer.Cell, Rectangle> cellRectangleEntry : collideables.entrySet()) {
            TiledMapTileLayer.Cell cell = cellRectangleEntry.getKey();
            Rectangle rectangle = cellRectangleEntry.getValue();
            if (cell.getTile() != null && rectangle.overlaps(tmpBallPosition)) {
                collision = true;
                if ("brick".equals(cell.getTile().getProperties().get("type"))) {
                    cell.setTile(null);
                }
            }
        }
        return collision;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    private void alignLayer(MapLayer mapLayer, Rectangle rect) {
        mapLayer.setOffsetX(rect.x);
        mapLayer.setOffsetY(-rect.y);
    }
}
