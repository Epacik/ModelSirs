package xyz.epat.modelsirs.agents;

import javafx.geometry.Point2D;

import java.util.List;
import java.util.Random;
import java.util.Vector;

public class Agent {
    public Agent(String name, float age, AgentState state) {
        this.name = name;
        this.age = age;
        this.state = state;
    }

    public Agent(String name, float age, AgentState state, AgentModifier[] modifiers) {
        this(name, age, state);
        for (var modifier : modifiers) {
            this.modifiers.add(modifier);
            modifiersBackup.add(modifier);
        }
    }
    private final String name;
    private float age;
    private AgentState state = AgentState.Susceptible;
    private final Vector<AgentModifier> modifiers = new Vector<AgentModifier>();
    private final Vector<AgentModifier> modifiersBackup = new Vector<AgentModifier>();
    private Point2D position = new Point2D(12, 32);

    public String getName() { return name; }
    public float getAge() { return age; }
    public void setAge(float newAge) { age = newAge; }

    public AgentState getState() { return state; }

    public List<AgentModifier> getModifiers() { return modifiers; }

    public void addModifier(AgentModifier modifier){
        modifiers.add(modifier);
        modifiersBackup.add(modifier);
    }

    public boolean removeModifier(AgentModifier modifier){
        if(modifier.isPermanent())
        {
            return false;
        }
        modifiers.remove(modifier);
        modifiersBackup.remove(modifier);

        return true;
    }

    public boolean removeModifier(int index){
        var modifier = modifiers.get(index);
        var modifier2 = modifiersBackup.get(index);

        if(!modifier.getName().equals(modifier2.getName())){
            return false;
        }

        if(modifier.isPermanent() || modifier2.isPermanent()){
            return false;
        }

        modifiers.remove(index);
        modifiersBackup.remove(index);

        return true;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }


    /**
     * Create specified number of agents
     * @param quantity number of agents to create
     * @return array of created agents
     */
    public static Agent[] createAgents(int quantity){
        var agents = new Agent[quantity];
        var rnd = new Random();
        for (int index = 0; index < quantity; index++) {
            agents[index] = new Agent(Long.toString(index),
                    rnd.nextInt(),
                    AgentState.Susceptible,
                    createPermanentModifiers(rnd.nextInt(5)));
        }

        return agents;
    }

    /**
     * Creates specified number of modifiers for an agent
     * @param quantity number of modifiers to create
     * @return array of created modifiers
     */
    private static AgentModifier[] createPermanentModifiers(int quantity) {
        var modifiers = new AgentModifier[quantity];

        var rnd = new Random();
        var modifierTypes = AgentModifierType.values();

        for (int index = 0; index < quantity; index++) {

            modifiers[index] = new AgentModifier(
                    "Default modifier",
                    rnd.nextFloat(0.7f, 1.3f),
                    modifierTypes[rnd.nextInt(modifierTypes.length)]);
        }

        return  modifiers;
    }
}