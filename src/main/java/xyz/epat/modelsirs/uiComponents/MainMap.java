package xyz.epat.modelsirs.uiComponents;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public class MainMap extends Region {

    private static final Logger logger = LoggerFactory.getLogger(MainMap.class);
    private final Canvas canvas;

    private int mapHeight = 400;
    private int mapWidth = 450;

    private boolean [][] agents;

    private Point2D mapStart;
    private double scaleFactor = 1;
    private int zoomLevel = 4;
    // 0.25, 0.33, 0.5, 0.75, 1, 1.5, 2, 3, 4, 5

    private Point2D translateBy = new Point2D(0,0);
    private Point2D translateByStartingPoint = new Point2D(0,0);

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

        agents = new boolean[mapWidth][mapHeight];

        for (int w = 0; w < mapWidth; w++) {
            for (int h = 0; h < mapHeight; h++) {
                agents[w][h] = (w + h) % 4 == 0;
            }
        }

        draw();

        setOnScroll(se -> {

            if((se.getDeltaY() < 0 || se.getDeltaX() < 0) && zoomLevel > 0 ){
                zoomLevel--;
            }
            else if((se.getDeltaY() > 0 || se.getDeltaX() > 0) && zoomLevel < 9){
                zoomLevel++;
            }
        });

        AtomicReference<Point2D> point = new AtomicReference<Point2D>(new Point2D(0, 0));
        setOnMousePressed(me -> {
            point.set(new Point2D(me.getX(), me.getY()));
        });

        setOnMouseDragged(me -> {
            var delta = new Point2D(me.getX(), me.getY()).subtract(point.get());
            translateBy = translateByStartingPoint.add(delta);

        });


        setOnMouseReleased(me -> {
            translateByStartingPoint = translateBy;

        });
//        setOnMouseEntered(me -> getScene().setCursor(Cursor.OPEN_HAND));
//        setOnMouseExited(me -> getScene().setCursor(Cursor.DEFAULT));
//        setOnMousePressed(me -> getScene().setCursor(Cursor.CLOSED_HAND));
    }



    private void draw(){
        var atomicLong = new AtomicLong(0);
        var rnd = new Random();

        var animationTimer = new AnimationTimer() {

            @Override
            public void handle(long now) {

                var canvasWidth = canvas.getWidth();
                var canvasHeight = canvas.getHeight();
                var context = canvas.getGraphicsContext2D();

                scaleFactor = calculateScaleFactor();

                drawBackground(context, canvasWidth, canvasHeight);
                
                drawMapBackground(context, canvasWidth, canvasHeight);


                drawZoom(context, canvasHeight);

            }
        };
        animationTimer.start();

    }

    private double calculateScaleFactor(){
        // initial scale factor
        // match heights first and check if it fits
        var scaleFactor = canvas.getHeight() / mapHeight;

        if(mapWidth * scaleFactor > canvas.getWidth()){
            // map doesn't fit lengthwise,
            // match widths
            scaleFactor = canvas.getWidth() / mapWidth;
        }
        return scaleFactor;
    }

    private double relativeMapWidth(){
        return relativeLength(mapWidth);
    }

    private double relativeMapHeight(){
        return relativeLength(mapHeight);
    }

    private double relativeLength(double n){
        return n * scaleFactor * getZoomScale();
    }

    private double relativeX(double x){
        return mapStart.getX() + relativeLength(x);
    }

    private double relativeY(double y){
        return mapStart.getY() + relativeLength(y);
    }

    private double getZoomScale() {
        return switch (zoomLevel) {
            case 0 -> 0.25;
            case 1 -> 0.33;
            case 2 -> 0.5;
            case 3 -> 0.75;
            case 4 -> 1;
            case 5 -> 1.5;
            case 6 -> 2;
            case 7 -> 3;
            case 8 -> 4;
            case 9 -> 5;
            default -> 1; //this shouldn't happen
        };
    }


    private void drawZoom(GraphicsContext context, double canvasHeight){
        var zoom = getZoomScale() * 100;
        context.setFill(Color.BLACK);
        context.fillText(zoom + "%", 0, canvasHeight);
    }

    private void drawMapBackground(GraphicsContext context, double canvasWidth, double canvasHeight) {
        var canvasCenterX = canvasWidth / 2;
        var canvasCenterY = canvasHeight / 2;

        // scale width and height of map to fit inside canvas
        mapStart = new Point2D(canvasCenterX - (relativeMapWidth() / 2), canvasCenterY - (relativeMapHeight() / 2)).add(translateBy);

        //draw the map background
        context.setFill(Color.WHITE);
        context.fillRect(mapStart.getX(), mapStart.getY(), relativeMapWidth(), relativeMapHeight());

        context.setFill(Color.VIOLET);

        for (int w = 0; w < mapWidth; w++) {
            for (int h = 0; h < mapHeight; h++) {
                if(agents[w][h])
                {
                    context.fillRect(relativeX(w), relativeY(h), relativeLength(1), relativeLength(1));
                }
            }
        }
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
