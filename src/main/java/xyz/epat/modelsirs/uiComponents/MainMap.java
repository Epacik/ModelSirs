package xyz.epat.modelsirs.uiComponents;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.epat.modelsirs.agents.Agent;
import xyz.epat.modelsirs.agents.AgentState;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * A field that
 */
public class MainMap extends Region {

    public final double infoHeight = 20;
    public final double crossHairSize = 40;
    private static final Logger logger = LoggerFactory.getLogger(MainMap.class);
    private final Canvas canvas;

    private int mapHeight = 60;
    private int mapWidth = 90;

    /**
     *
     */
    private final AtomicReference<Agent[][]> agents = new AtomicReference(new Agent[0][0]);

    private Point2D mapStart;
    private double scaleFactor = 1;
    /**
     * By how much the map should be zoomed
     * Levels: (level - percent)
     *  0 - 100%;
     *  1 - 150%;
     *  2 - 200%;
     *  3 - 300%;
     *  4 - 400%;
     *  5 - 500%;
     *  6 - 600%;
     *  7 - 700%;
     *  8 - 800%;
     *  9 - 900%;
     *  100 - 1000%;
     */
    private int zoomLevel = 0; // zoom levels (level 0 = 100%): 1, 1.5, 2, 3, 4, 5, 6, 7, 8, 9, 10

    // used for panning
    /**
     * by how much drawn map should be translated in x and y axis
     */
    private Point2D translateBy = Point2D.ZERO;
    /**
     * value is updated on Left Mouse Button release so the map can be moved while dragging without it flying to the moon
     */
    private Point2D translateByStartingPoint = Point2D.ZERO;

    public MainMap(){
        super();

        // create a canvas, bind width and height to the whole component and show it
        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        getChildren().add(canvas);

        //set some default values 
        prefHeight(300);
        prefWidth(400);
        canvas.prefWidth(300);
        canvas.prefHeight(400);

        // idk if that's necessary
        super.layoutChildren();

        startAnimation();

        /// --------------------------------------------
        /// |||||||||||||||| EVENTS ||||||||||||||||||||
        /// --------------------------------------------
        
        // Zooming in and out
        setOnScroll(se -> {
            if((se.getDeltaY() < 0 || se.getDeltaX() < 0) && zoomLevel > 0 ){
                logger.debug("Zooming out");
                zoomLevel--;

                translateBy = snapMapBack(translateBy);
                translateByStartingPoint = snapMapBack(translateByStartingPoint);
            }
            else if((se.getDeltaY() > 0 || se.getDeltaX() > 0) && zoomLevel < 10){
                logger.debug("Zooming in");
                zoomLevel++;

                translateBy = snapMapBack(translateBy);
                translateByStartingPoint = snapMapBack(translateByStartingPoint);
            }
        });

        // Panning
        // there's probably a much better way of doing that, but idc

        // just a reference point, from where user is dragging
        var point = new AtomicReference<Point2D>(new Point2D(0, 0));
        setOnMousePressed(me -> {
            logger.debug("start a map pan");
            point.set(new Point2D(me.getX(), me.getY()));
        });

        setOnMouseDragged(me -> {
            logger.debug("dragging ");
            var delta = translateByStartingPoint.add(new Point2D(me.getX(), me.getY()).subtract(point.get()));

            translateBy = snapMapBack(delta);
        });

        setOnMouseReleased(me -> {
            translateByStartingPoint = translateBy;
        });
    }


    private final AnimationTimer animationTimer = new AnimationTimer() {

        @Override
        public void handle(long now) {
            if(cancelExecution.get())
            {
                animationTimer.stop();
                return;
            }

            var context = canvas.getGraphicsContext2D();

            scaleFactor = calculateScaleFactor();

            drawBackground(context);

            calculateAndSetMapStartingPoint();

            drawMapBackground(context);

            drawAgents(context);

            drawInfo(context);

            //drawCrosshair(context);
        }
    };

    /**
     * Start drawing the map
     */
    private void startAnimation(){
        animationTimer.start();
    }

    private Point2D snapMapBack(Point2D delta) {
        var bound = 10;
        var x = delta.getX();
        var y = delta.getY();

        var mapDefaultStartX = (canvas.getWidth() / 2) - (relativeMapWidth() / 2);
        var mapDefaultStartY = ((canvas.getHeight() - infoHeight) / 2) - (relativeMapHeight() / 2);

        // check if map isn't on the moon and snap it back if it is
        if(x > canvas.getWidth() - mapDefaultStartX - bound) {
            x = canvas.getWidth() - mapDefaultStartX - bound;
        }
        else if (x < -1 * relativeMapWidth() - mapDefaultStartX + bound) {
            x = -1 * relativeMapWidth() - mapDefaultStartX + bound;
        }

        if(y > canvas.getHeight() - mapDefaultStartY - bound - infoHeight) {
            y = canvas.getHeight() - mapDefaultStartY - bound - infoHeight;
        }
        else if(y < -1 * relativeMapHeight() - mapDefaultStartY + bound) {
            y = -1 * relativeMapHeight() - mapDefaultStartY + bound;
        }
        return new Point2D((int)x, (int)y);
    }

    public void resetMapPosition() {
        translateBy = Point2D.ZERO;
        translateByStartingPoint = Point2D.ZERO;
    }

    private void drawCrosshair(GraphicsContext context) {
        var canvasCenterX = canvas.getWidth() / 2;
        var canvasCenterY = (canvas.getHeight() - infoHeight) / 2;
        var crosshairHalfSize = crossHairSize / 2;
        context.setStroke(Color.BLACK);
        context.strokeLine(canvasCenterX,
                canvasCenterY - crosshairHalfSize,
                canvasCenterX,
                canvasCenterY + crosshairHalfSize);
        context.strokeLine(canvasCenterX - crosshairHalfSize,
                canvasCenterY,
                canvasCenterX + crosshairHalfSize,
                canvasCenterY);
    }

    /**
     * Calculate scale factor for current window size
     * at 100% zoom the drawn map should uniformly fit inside canvas
     * @return Scale factor
     */
    private double calculateScaleFactor(){
        // initial scale factor
        // match heights first and check if it fits
        var scaleFactor = (canvas.getHeight() - infoHeight) / mapHeight;

        if(scaleFactor < 0 || mapWidth * scaleFactor > canvas.getWidth()){
            // map doesn't fit lengthwise,
            // match widths
            scaleFactor = canvas.getWidth() / mapWidth;
        }
        return scaleFactor;
    }

    /**
     * Helper method used to calculate relative width of the map
     * @return width of the map relative to scale factor and zoom
     */
    private double relativeMapWidth(){
        return relativeLength(mapWidth);
    }

    /**
     * Helper method used to calculate relative height of the map
     * @return height of the map relative to scale factor and zoom
     */
    private double relativeMapHeight(){
        return relativeLength(mapHeight);
    }

    /**
     * Multiplies provided number/length with scale factor and the zoom scale
     * @param n provided number/length
     * @return provided length scaled by scale factor and zoom scale
     */
    private double relativeLength(double n){
        return n * scaleFactor * getZoomScale();
    }

    private double relativeX(double x){
        return mapStart.getX() + relativeLength(x);
    }
    private double relativeY(double y){
        return mapStart.getY() + relativeLength(y);
    }
    private Point2D relativePosition(double x, double y){
        return mapStart.add(relativeLength(x), relativeLength(y));
    }

    private double getZoomScale() {
        if(zoomLevel > 1) return zoomLevel;
        
        return switch (zoomLevel) {
            case 0  -> 1;
            case 1  -> 1.5;
            default -> throw new IllegalStateException("Unexpected value: " + zoomLevel);
        };
    }

    /**
     * Draws informations about current zoom level and by how much the map is moved
     * @param context canvas's drawing context
     */
    private void drawInfo(GraphicsContext context){

        final DecimalFormat decimalFormat = new DecimalFormat("0.00");

        context.setFill(Color.WHITE);
        context.setStroke(Color.BLACK);
        context.setLineWidth(1);
        context.fillRect(0, canvas.getHeight() - infoHeight + 1, canvas.getWidth(), infoHeight - 1);
        context.strokeRect(-2, canvas.getHeight() - infoHeight + 1, canvas.getWidth() + 4, infoHeight);

        var zoom = getZoomScale() * 100;
        context.setFill(Color.BLACK);
        context.fillText(zoom + "%; " +
                        "Moved by: [" + translateBy.getX() + ", " + translateBy.getY() + "]; " +
                        "Map size: " + mapWidth + "x" + mapHeight + "; " +
                        "Map size on screen: " + decimalFormat.format(relativeMapWidth()) + "x" + decimalFormat.format(relativeMapHeight()) + "; ",
                0,
                canvas.getHeight() - 5);
    }

    private void calculateAndSetMapStartingPoint(){
        var canvasCenterX = canvas.getWidth() / 2;
        var canvasCenterY = canvas.getHeight() / 2;

        // scale width and height of map to fit inside canvas
        mapStart = new Point2D(canvasCenterX - (relativeMapWidth() / 2),
                canvasCenterY - (relativeMapHeight() / 2) - (infoHeight / 2)).add(translateBy);
    }

    private void drawMapBackground(GraphicsContext context) {
        context.setFill(Color.WHITE);
        context.fillRect(mapStart.getX(), mapStart.getY(), relativeMapWidth(), relativeMapHeight());
    }

    private void drawBackground(GraphicsContext context) {
        // fill the whole screen
        context.setFill(Color.LIGHTGRAY);
        context.setStroke(Color.LIGHTGRAY);
        context.fillRect(0,0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawAgents(GraphicsContext context){
        var agents = this.agents.get();

        if (agents.length == 0)
            return;

        var relativeOneUnit = relativeLength(1);
        var canvasWidth = canvas.getWidth();
        var canvasHeight = canvas.getHeight();

        for (int i = 0; i < 4; i++) {

            var state = switch (i){
                case 0 -> AgentState.Susceptible;
                case 1 -> AgentState.Infectious;
                case 2 -> AgentState.Recovered;
                case 3 -> AgentState.Dead;
                default -> throw new IllegalStateException("Unexpected value: " + i);
            };

            // set color of agent based on it's state
            context.setFill(switch (state) {
                case Susceptible -> Color.DEEPSKYBLUE;
                case Infectious -> Color.RED;
                case Recovered -> Color.YELLOW;
                case Dead -> Color.DARKGRAY;
            });


            for (int x = 0; x < agents.length; x++) {
                for (int y = 0; y < agents[x].length; y++) {
                    var agent = agents[x][y];
                    if(agent == null || agent.getState() != state)
                        continue;

                    var relativeX = relativeX(x);
                    var relativeY = relativeY(y);

                    // skip drawing agent if it's outside bounds of canvas
                    if(relativeX < -1 * relativeOneUnit ||
                            relativeY < -1 * relativeOneUnit ||
                            relativeX > canvasWidth + relativeOneUnit ||
                            relativeY > canvasHeight + relativeOneUnit)
                        continue;

                    // draw agent
                    context.fillRect(relativeX, relativeY, relativeOneUnit, relativeOneUnit);
                }
            }
        }



    }

    public Point2D getMapDimentions() {
        return new Point2D(mapWidth, mapHeight);
    }

    public void setMap(int mapWidth, int mapHeight){
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    public void lazySetAgents(Agent[][] agents) {
        this.agents.lazySet(agents);
    }

    private final AtomicBoolean cancelExecution = new AtomicBoolean(false);
    public void stop() {
        cancelExecution.set(true);
    }
}
