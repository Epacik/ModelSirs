package xyz.epat.modelsirs;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.epat.modelsirs.agents.Agent;
import xyz.epat.modelsirs.agents.AgentState;
import xyz.epat.modelsirs.uiComponents.MainMap;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainViewController {
    @FXML
    public Label simulationState;

    @FXML
    private MainMap mainMap;

    Agent[][][] agents = new Agent[2][10][10];

    private static final Logger logger = LoggerFactory.getLogger(MainMap.class);

    private final int infectionRadius = 2;
    private final double chanceToGetInfected = 20;
    private final double chanceToDie = 2;
    private final double chanceToRecover = 5;
    private final double chanceToGetSusceptible = 5;

    private final double coverageByAgents = 0.8;

    private final AtomicInteger susceptibleTotal = new AtomicInteger(0);
    private final AtomicInteger infectiousTotal = new AtomicInteger(0);
    private final AtomicInteger deadTotal = new AtomicInteger(0);
    private final AtomicInteger recoveredTotal = new AtomicInteger(0);

    private final Random random = new Random();

    double nextPercentage() {
        return random.nextDouble(100);
    }

    private final AnimationTimer animationTimer = new AnimationTimer() {

        @Override
        public void handle(long now) {

            if(cancelExecution.get())
            {
                animationTimer.stop();
                return;
            }

            if(cancelExecution.get() && animationTimer != null) {
                animationTimer.stop();
                return;
            }

            var size = mainMap.getMapDimentions();
            var totalNumberOfAgents = (int)(size.getX() * size.getY() * coverageByAgents);
            if(resetingSimulation.get()){
                simulationState.setText("Generowanie nowych agentów: " + generationStep.get() + " z " + totalNumberOfAgents);
                return;
            }

            var susceptible = susceptibleTotal.get();
            var infectious = infectiousTotal.get();
            var recovered = recoveredTotal.get();
            var dead = deadTotal.get();

            simulationState.setText(" Wszyscy: " + totalNumberOfAgents + "; Podatni: " + susceptible + "; Zarażeni: " + infectious + ";\n Ozdrowieni: " + recovered + "; Martwi: " + dead);
        }
    };

    @FXML
    public void initialize() {

        resetSimulation();
        var size = mainMap.getMapDimentions();

        var thread = new Thread(() -> {
            if(cancelExecution.get())
                return;

            int currentAgents = 0;
            int previousAgents = 1;

            // no need to recalculate it each time
            var infectionDiameter = infectionRadius * 2 + 1;
            boolean [][] rangeMask = new boolean[infectionDiameter][infectionDiameter];
            {
                for (int x = 0; x < infectionDiameter; x++) {
                    for (int y = 0; y < infectionDiameter; y++) {
                        if(x == infectionRadius && y == infectionRadius)
                        {
                            rangeMask[x][y] = false;
                            continue;
                        }

                        var xSquared = (x - infectionRadius) * (x - infectionRadius);
                        var ySquared = (y - infectionRadius) * (y - infectionRadius);
                        var distance = Math.sqrt(xSquared + ySquared);

                        rangeMask[x][y] = distance <= infectionRadius;
                    }
                }

            }

            while(true){

                if(cancelExecution.get())
                    return;

                if(resetingSimulation.get() || simulationStep.get())
                    continue;

                simulationStep.set(true);

                var now = LocalDateTime.now().getNano();

                int susceptibleTotal = 0;
                int infectiousTotal = 0;
                int deadTotal = 0;
                int recoveredTotal = 0;

                var previous = agents[previousAgents];
                var current = agents[currentAgents];

                for (int x = 0; x < agents[0].length; x++) {
                    for (int y = 0; y < agents[0][0].length; y++) {

                        if(previous[x][y] == null || current[x][y] == null)
                            continue;

                        var state = previous[x][y].getState();
                        switch (state){

                            case Susceptible -> {
                                for (int innerX = 0; innerX < infectionDiameter; innerX++) {
                                    for (int innerY = 0; innerY < infectionDiameter; innerY++) {
                                        if(!rangeMask[innerX][innerY])
                                            continue;

                                        var testX = x - infectionRadius + innerX;
                                        var testY = y - infectionRadius + innerY;

                                        if(testX < 0 || testY < 0 || testX >= agents[0].length || testY >= agents[0][0].length)
                                            continue;

                                        var testedAgent = previous[testX][testY];
                                        if(testedAgent == null || testedAgent.getState() != AgentState.Infectious)
                                            continue;

                                        if(nextPercentage() < chanceToGetInfected)
                                            current[x][y].setState(AgentState.Infectious);
                                    }
                                }
                            }
                            case Infectious -> {
                                if (nextPercentage() < chanceToDie) {
                                    current[x][y].setState(AgentState.Dead);
                                } else if (nextPercentage() < chanceToRecover) {
                                    current[x][y].setState(AgentState.Recovered);
                                }
                            }
                            case Recovered -> {
                                if (nextPercentage() < chanceToGetSusceptible) {
                                    current[x][y].setState(AgentState.Susceptible);
                                }
                            }
                        }
                    }
                }
                // count agents per state
                for (Agent[] value : current) {
                    for (Agent agent : value) {
                        if (agent == null) {
                            continue;
                        }
                        var state = agent.getState();
                        switch (state) {
                            case Susceptible -> susceptibleTotal++;
                            case Infectious -> infectiousTotal++;
                            case Recovered -> recoveredTotal++;
                            case Dead -> deadTotal++;
                        }
                    }

                }

                this.susceptibleTotal.set(susceptibleTotal);
                this.infectiousTotal.set(infectiousTotal);
                this.recoveredTotal.set(recoveredTotal);
                this.deadTotal.set(deadTotal);

                mainMap.lazySetAgents(previous);

                var xSize = agents[currentAgents].length;
                var ySize = agents[currentAgents][0].length;
                agents[previousAgents] = new Agent[xSize][ySize];
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        if(agents[currentAgents][x][y] != null)
                            agents[previousAgents][x][y] = agents[currentAgents][x][y].copy();
                    }
                }

                simulationStep.set(false);

                currentAgents = (currentAgents + 1) % 2;
                previousAgents = (previousAgents + 1) % 2;

                // there's no point of simulation when no-one is infected
//                if(infectiousTotal == 0)
//                {
//                    return;
//                }
            }
        });

        thread.start();

        animationTimer.start();

    }

    @FXML
    protected void onResetButtonClick() {
        mainMap.resetMapPosition();
        //mainMap.drawRect();
    }

    @FXML
    public void onResetSimulationButtonClick() {
        resetSimulation();
    }

    private final AtomicBoolean resetingSimulation = new AtomicBoolean(false);
    private final AtomicBoolean simulationStep = new AtomicBoolean(false);
    private final AtomicInteger generationStep = new AtomicInteger(0);
    private final AtomicBoolean cancelExecution = new AtomicBoolean(false);


    private void resetSimulation() {

        var thread = new Thread(() -> {
            if(cancelExecution.get())
                return;

            while(simulationStep.get()){

            }

            resetingSimulation.set(true);
            var rnd = new Random();

            var size = mainMap.getMapDimentions();
            var numberOfAgents = (int)(size.getX() * size.getY() * coverageByAgents);
            agents[0] = new Agent[(int)size.getX()][(int)size.getY()];
            agents[1] = new Agent[(int)size.getX()][(int)size.getY()];
            for (int index = 0; index < numberOfAgents; index++){
                generationStep.lazySet(index);

                var agent = new Agent("Agent " + index + 1, 0, AgentState.Susceptible);


                int x = 0;
                int y = 0;
                do {
                    x = rnd.nextInt((int)size.getX());
                    y = rnd.nextInt((int)size.getY());
                } while (agents[0][x][y] != null);

                agents[0][x][y] = agent;
                agents[1][x][y] = agent.copy();
            }

            for (int i = 0; i < (agents[0].length * 0.003 > 1 ? agents[0].length * 0.003 : 1); i++) {
                int x = 0;
                int y = 0;
                do {
                    x = rnd.nextInt(agents[0].length);
                    y = rnd.nextInt(agents[0][x].length);
                } while (agents[0][x][y] == null);
                agents[0][x][y].setState(AgentState.Infectious);
                agents[1][x][y].setState(AgentState.Infectious);
            }

            mainMap.lazySetAgents(agents[0]);
            resetingSimulation.set(false);
        });
        thread.start();
    }

    public void stopThreads() {
        cancelExecution.set(true);
        mainMap.stop();
    }
}