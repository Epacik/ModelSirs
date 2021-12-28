package xyz.epat.modelsirs.uiComponents;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;


public class MainMap extends Region {

    private static final Logger logger = LoggerFactory.getLogger(MainMap.class);
    private final Canvas canvas;

    private int mapHeight = 400;
    private int mapWidth = 450;


    public MainMap(){
        super();

        prefHeight(300);
        prefWidth(400);

        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        canvas.prefWidth(300);
        canvas.prefHeight(400);

        getChildren().add(canvas);

        super.layoutChildren();

        drawRect();

        setOnMouseEntered(me -> getScene().setCursor(Cursor.OPEN_HAND));
        setOnMouseExited(me -> getScene().setCursor(Cursor.DEFAULT));
        setOnMousePressed(me -> getScene().setCursor(Cursor.CLOSED_HAND));
    }



    private Point2D rectStart;

    private void drawRect(){
        rectStart = new Point2D(2,2);
        var atomicLong = new AtomicLong(0);
        var rnd = new Random();

        var animationTimer = new AnimationTimer() {

            @Override
            public void handle(long now) {

                var canvasWidth = canvas.getWidth();
                var canvasHeight = canvas.getHeight();
                var context = canvas.getGraphicsContext2D();

                drawBackground(context, canvasWidth, canvasHeight);
                
                drawMapBackground(context, canvasWidth, canvasHeight);


//                context.setFill(Color.TRANSPARENT);
//                context.setStroke(new Color(rnd.nextDouble(1), rnd.nextDouble(1), rnd.nextDouble(1), 1));
//                context.setLineWidth(2);
//                context.strokeRect(0,0,rectStart.getX(),rectStart.getY());
//                rectStart = rectStart.add(1, 1);
//
//                if(rectStart.getX() > canvas.getWidth() && rectStart.getY() > canvas.getWidth()){
//                    rectStart = new Point2D(2,2);
//                }
            }
        };
        animationTimer.start();

    }

    private void drawMapBackground(GraphicsContext context, double canvasWidth, double canvasHeight) {
        var canvasCenterX = canvasWidth / 2;
        var canvasCenterY = canvasHeight / 2;

        // let's try to fit the map

        // initial scale factor
        // match heights first and check if it fits
        var scaleFactor = canvasHeight / mapHeight;

        if(mapWidth * scaleFactor > canvasWidth){
            // map doesn't fit lengthwise,
            // match widths
            scaleFactor = canvasWidth / mapWidth;
        }

        // scale width and height of map to fit inside canvas
        var scaledMapWidth = mapWidth * scaleFactor;
        var scaledMapHeight = mapHeight * scaleFactor;

        // find starting point of map
        var mapStartX = canvasCenterX - (scaledMapWidth / 2);
        var mapStartY = canvasCenterY - (scaledMapHeight / 2);

        //draw the map background
        context.setFill(Color.WHITE);
        context.fillRect(mapStartX, mapStartY, scaledMapWidth, scaledMapHeight);
    }

    private void drawBackground(GraphicsContext context, double canvasWidth, double canvasHeight) {
        // fill the whole screen
        context.setFill(Color.LIGHTGRAY);
        context.setStroke(Color.LIGHTGRAY);
        context.fillRect(0,0, canvasWidth, canvasHeight);
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public void setMapHeight(int mapHeight) {
        this.mapHeight = mapHeight;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public void setMapWidth(int mapWidth) {
        this.mapWidth = mapWidth;
    }
}
