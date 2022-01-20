package xyz.epat.modelsirs;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.epat.modelsirs.agents.Agent;
import xyz.epat.modelsirs.agents.AgentState;
import xyz.epat.modelsirs.uiComponents.MainMap;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainViewController {
    @FXML
    public Label simulationState;
    public TextField mapHeightInput;
    public TextField mapWidthInput;
    public TextField mapCoverageInput;
    public TextField infectionRangeInput;
    public TextField chanceToGetInfectedInput;
    public TextField chanceToRecoverInput;
    public TextField chanceToDieInput;
    public TextField chanceToGetSusceptibleInput;
    public TextField infectiousAgentsInput;

    @FXML
    private MainMap mainMap;

    Agent[][] agents = new Agent[10][10];

    private static final Logger logger = LoggerFactory.getLogger(MainMap.class);

    private int infectionRange = 2;
    private double chanceToGetInfected = 4;
    private double chanceToDie = 1;
    private double chanceToRecover = 5;
    private double chanceToGetSusceptible = 5;

    // what percentage of the map should agents cover
    private double coverageByAgents = 0.8;

    // just to show some stats
    private final AtomicInteger susceptibleTotal = new AtomicInteger(0);
    private final AtomicInteger infectiousTotal = new AtomicInteger(0);
    private final AtomicInteger deadTotal = new AtomicInteger(0);
    private final AtomicInteger recoveredTotal = new AtomicInteger(0);
    private final AtomicInteger generationStep = new AtomicInteger(0);

    // used to block simulation or end it altogether
    private final AtomicBoolean resetingSimulation = new AtomicBoolean(false);
    private final AtomicBoolean simulationStep = new AtomicBoolean(false);
    private final AtomicBoolean cancelExecution = new AtomicBoolean(false);

    private final Random random = new Random();
    /**
     * Get a double value between 0 and 100
     * @return random value between 0 and 100
     */
    double nextPercentage() {
        return random.nextDouble(0, 100);
    }


    private static final DecimalFormat df = new DecimalFormat("0.00");
    /**
     * timer used to update informations in the UI
     */
    private final AnimationTimer animationTimer = new AnimationTimer() {

        @Override
        public void handle(long now) {

            if (cancelExecution.get()) {
                animationTimer.stop();
                return;
            }

            if (cancelExecution.get() && animationTimer != null) {
                animationTimer.stop();
                return;
            }

            var size = mainMap.getMapDimentions();
            var totalNumberOfAgents = (int) (size.getX() * size.getY() * coverageByAgents);
            if (resetingSimulation.get()) {
                simulationState.setText("Generowanie nowych agentów: " + generationStep.get() + " z " + totalNumberOfAgents);
                return;
            }

            var susceptible = susceptibleTotal.get();
            var infectious = infectiousTotal.get();
            var recovered = recoveredTotal.get();
            var dead = deadTotal.get();

            simulationState.setText(" Wszyscy: " + totalNumberOfAgents + "; Podatni: " + susceptible + "; Zarażeni: " + infectious +
                    ";\n Ozdrowieni: " + recovered + "; Martwi: " + dead +
                    ";\n Pokrycie agentami: " + df.format(coverageByAgents * 100) + "%; Zasięg zarażania: " + infectionRange +
                    ";\n Szansa na: [ zarażenie: " + df.format(chanceToGetInfected) + "%; wyzdrowienie: " + df.format(chanceToRecover) + "%; śmierć: " + df.format(chanceToDie) + "%; ponowną podatność: " + df.format(chanceToGetSusceptible) + "% ]");
        }
    };

    /**
     * this gets called after UI is initialized
     */
    @FXML
    public void initialize() {
        animationTimer.start();

        resetSimulation();
    }

    private Thread simThread = null;
    /**
     * Spawns a new thread to simulate on and runs it
     */
    private void initializeSimThread() {

        simThread = new Thread(() -> {

            if (cancelExecution.get()) // stop! someone is closing a window
                return;

            // rangeMask is used to determine which agents around current one can infect it
            //
            // the maximum distance to the agent able to infect the current one may change
            // so not only agents placed right next to the current one may infect it
            //
            var infectionDiameter = infectionRange * 2 + 1;
            boolean[][] rangeMask = new boolean[infectionDiameter][infectionDiameter];

            // fill the mask
            for (int x = 0; x < infectionDiameter; x++) {
                for (int y = 0; y < infectionDiameter; y++) {

                    // that's the center of rangeMask, no agent can infect themselves
                    if (x == infectionRange && y == infectionRange) {
                        rangeMask[x][y] = false;
                        continue;
                    }

                    // agents that are further away than the distance defined in infectionRadius
                    // can infect the agent in the center of a mask
                    var xSquared = (x - infectionRange) * (x - infectionRange);
                    var ySquared = (y - infectionRange) * (y - infectionRange);
                    var distance = Math.sqrt(xSquared + ySquared);

                    rangeMask[x][y] = distance <= infectionRange;
                }
            }

            while (true) {

                // stop! someone's either closing a window or resetting the simulation
                if (cancelExecution.get() || resetingSimulation.get())
                    return;

                // is a simulation step already running?
                // while operating on a single thread this shouldn't ever happen, but it's here just in case
                if (simulationStep.get())
                    continue;

                simulationStep.set(true);

                // may become useful in case of slowing the simulation down
                //var now = LocalDateTime.now().getNano();

                var previous = agents;
                var current = new Agent[agents.length][agents[0].length];

                for (int x = 0; x < agents.length; x++) {
                    for (int y = 0; y < agents[0].length; y++) {

                        // stop! someone's either closing a window or resetting the simulation
                        if (cancelExecution.get() || resetingSimulation.get()) {
                            simulationStep.set(false);
                            return;
                        }

                        // there's no agent in this spot, we can move on
                        if (previous[x][y] == null)
                            continue;

                        // current state is empty at the beginning of a step, so it would be nice to populate it
                        current[x][y] = previous[x][y].copy();
                        var state = previous[x][y].getState();
                        switch (state) {
                            case Susceptible -> {

                                //using a rangeMask defined earlier check if current agent is getting infected
                                maskloops:
                                for (int innerX = 0; innerX < infectionDiameter; innerX++) {
                                    for (int innerY = 0; innerY < infectionDiameter; innerY++) {

                                        // let's skip agents that are too far away and ourselves
                                        if (!rangeMask[innerX][innerY])
                                            continue;

                                        var testX = x - infectionRange + innerX;
                                        var testY = y - infectionRange + innerY;

                                        // skip if we're outside bounds of the array
                                        if (testX < 0 || testY < 0 || testX >= agents.length || testY >= agents[0].length)
                                            continue;

                                        // skip if agent doesn't exist or isn't infectious
                                        var testedAgent = previous[testX][testY];
                                        if (testedAgent == null || testedAgent.getState() != AgentState.Infectious)
                                            continue;

                                        // let's "roll a dice" and find out if the agent gets infected and if it does,
                                        // then we can just skip the rest of the mask
                                        if (nextPercentage() < chanceToGetInfected)
                                        {
                                            current[x][y].setState(AgentState.Infectious);
                                            break maskloops;
                                        }
                                    }
                                }

                            }

                            case Infectious -> {
                                // infectious agents can either recover, die or stay infectious
                                if (nextPercentage() < chanceToDie) {
                                    current[x][y].setState(AgentState.Dead);
                                } else if (nextPercentage() < chanceToRecover) {
                                    current[x][y].setState(AgentState.Recovered);
                                }
                            }
                            case Recovered -> {
                                // recovered agents can get susceptible again
                                if (nextPercentage() < chanceToGetSusceptible) {
                                    current[x][y].setState(AgentState.Susceptible);
                                }
                            }
                            // Dead agents aren't going to do anything, so that case is missing
                        }
                    }
                }

                // useful to show some additional informations about current state of simulation
                int susceptibleTotal = 0;
                int infectiousTotal = 0;
                int deadTotal = 0;
                int recoveredTotal = 0;

                // count agents per state
                for (Agent[] value : current) {
                    for (Agent agent : value) {
                        if (agent == null) {
                            continue;
                        }
                        switch (agent.getState()) {
                            case Susceptible -> susceptibleTotal++;
                            case Infectious  -> infectiousTotal++;
                            case Recovered   -> recoveredTotal++;
                            case Dead        -> deadTotal++;
                        }
                    }

                }

                // set fields so the UI can display those numbers
                this.susceptibleTotal.set(susceptibleTotal);
                this.infectiousTotal.set(infectiousTotal);
                this.recoveredTotal.set(recoveredTotal);
                this.deadTotal.set(deadTotal);

                // send the current state to be displayed and save it for the next step
                mainMap.lazySetAgents(current);
                agents = current;

                simulationStep.set(false);

                // there's no point of continuing when no-one is infected and no-one is recovered
                // the state of the simulation won't change at this point
                if(infectiousTotal == 0 && recoveredTotal == 0)
                {
                    return;
                }
            }
        });
        simThread.start();
    }

    /**
     * called by "Zresetuj pozycję mapy"
     */
    @FXML
    protected void onResetMapPositionButtonClick() {
        mainMap.resetMapPosition();
        //mainMap.drawRect();
    }

    /**
     * called by "Zresetuj symulację"
     */
    @FXML
    public void onResetSimulationButtonClick() {
        resetSimulation();
    }

    private void showError(String message){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText("Błąd");
            alert.setContentText(message);
            alert.showAndWait();
        });

    }

    /**
     * stops the simulation, resets state of the simulation and starts a new one
     */
    private void resetSimulation() {

        var thread = new Thread(() -> {
            if(cancelExecution.get())
                return;

            resetingSimulation.set(true);
            while(simulationStep.get()){
                // wait until simulation exits
            }

            String mapWidthS = mapWidthInput.getText();
            String mapHeightS = mapHeightInput.getText();
            String coverageS = mapCoverageInput.getText();
            String rangeS = infectionRangeInput.getText();
            String chanceToGetInfectedS = chanceToGetInfectedInput.getText();
            String chanceToDieS = chanceToDieInput.getText();
            String chanceToRecoverS = chanceToRecoverInput.getText();
            String chanceToGetSusceptibleS = chanceToGetSusceptibleInput.getText();
            String infectionsAgentsNumberS = infectiousAgentsInput.getText();

            int mapWidth = 0;
            int mapHeight = 0;
            double coverage = 0;
            int range = 0;
            double chanceInfected = 0;
            double chanceDie = 0;
            double chanceRecover = 0;
            double chanceSusceptible = 0;

            double infectionsAgentsNumber = 0.003;

            try {
                mapWidth = Integer.parseUnsignedInt(mapWidthS);
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania szerokości mapy do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            try {
                mapHeight = Integer.parseUnsignedInt(mapHeightS);
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania wysokości mapy do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }


            try {
                coverage = Double.parseDouble(coverageS) / 100;
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania pokrycia mapy agentami do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            try {
                range = Integer.parseUnsignedInt(rangeS);
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania zasięgu zarażania do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            try {
                chanceInfected = Double.parseDouble(chanceToGetInfectedS);
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania szansy na zarażenie do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            try {
                chanceDie = Double.parseDouble(chanceToDieS);
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania szansy na śmierć do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            try {
                chanceRecover = Double.parseDouble(chanceToRecoverS);
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania szansy na wyzdrowienie do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            try {
                chanceSusceptible = Double.parseDouble(chanceToGetSusceptibleS);
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania szansy na ponowną podatność do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            try {
                infectionsAgentsNumber = Double.parseDouble(infectionsAgentsNumberS) / 100;
            }
            catch (NumberFormatException ex){
                showError("Wystąpił błąd konwertowania początkowej ilości zarażonych do liczby\n" +
                        "Upewnij się, że wprowadzona wartość jest liczbą i spróbuj ponownie\n\n" + ex.getLocalizedMessage());
                return;
            }

            infectionsAgentsNumber = limit(infectionsAgentsNumber, 100.0, 0.0);

            mainMap.setMap(mapWidth, mapHeight);



            coverageByAgents       = limit(coverage, 1.0, 0.0);
            chanceToGetInfected    = limit(chanceInfected, 100.0, 0.0);
            chanceToGetSusceptible = limit(chanceSusceptible, 100.0, 0.0);
            chanceToRecover        = limit(chanceRecover, 100.0, 0.0);
            chanceToDie            = limit(chanceDie, 100.0, 0.0);

            infectionRange = range;


            var size = new Point2D(mapWidth, mapHeight);


            // determine the size of simulation and prepare new agent array

            var numberOfAgents = (int)(size.getX() * size.getY() * coverageByAgents);

            var agents = new Agent[(int)size.getX()][(int)size.getY()];

            //agents[1] = new Agent[(int)size.getX()][(int)size.getY()];
            for (int index = 0; index < numberOfAgents; index++){

                // in case of larger simulations this should indicate to the user how many agents is yet to be generated
                // in smaller ones it probably would be too quick to notice
                generationStep.lazySet(index);

                // create new agent and place it in some empty spot
                // two agents cannot share the same spot
                var agent = new Agent("Agent " + index + 1, 0, AgentState.Susceptible);

                int x = 0;
                int y = 0;
                do {
                    x = random.nextInt((int)size.getX());
                    y = random.nextInt((int)size.getY());
                } while (agents[x][y] != null);

                agents[x][y] = agent;
            }

            // 0.3% (effectively rounded down to the nearest integer) of agents gets infected,
            // unless that number is smaller than one in such case only one of them gets infected
            for (int i = 0; i < (agents.length * agents[0].length * infectionsAgentsNumber > 1 ? agents.length * agents[0].length * infectionsAgentsNumber : 1); i++) {
                int x = 0;
                int y = 0;
                do {
                    x = random.nextInt(agents.length);
                    y = random.nextInt(agents[x].length);
                } while (agents[x][y] == null);
                agents[x][y].setState(AgentState.Infectious);
                //agents[1][x][y].setState(AgentState.Infectious);
            }

            // update agents to display
            mainMap.lazySetAgents(agents);
            this.agents = agents;

            resetingSimulation.set(false);

            // and start the simulation
            initializeSimThread();
        });
        thread.start();
    }



    private <T extends Number & Comparable<T>> T limit(T number, T max, T min) {
        if(number.compareTo(max) > 0){
            number = max;
        }
        else if(number.compareTo(min) < 0){
            number = min;
        }
        return number;
    }


    /**
     * Stops the simulation and UI updates
     */
    public void stop() {
        cancelExecution.set(true);
        mainMap.stop();
    }
}