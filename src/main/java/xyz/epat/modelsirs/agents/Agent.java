package xyz.epat.modelsirs.agents;


import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * Agent used in simulation
 */
public class Agent {

    /**
     * Agent used in simulation
     * @param name name of an Agent
     * @param age initial age of Agent
     * @param state initial state of Agent
     */
    public Agent(String name, float age, AgentState state) {
        this.name = name;
        this.age = age;
        this.state = state;
    }

    /**
     * Agent used in simulation
     * @param name name of an Agent
     * @param age initial age of Agent
     * @param state initial state of Agent
     * @param modifiers initial modifiers
     */
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

    /**
     * Create a copy of an agent
     * @return new Agent with the same contents as current agent
     */
    public Agent copy() {
        return new Agent(this.name, this.age, this.state, this.modifiers.toArray(new AgentModifier[0]));
    }

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

    public void addToAge(long n) {
        age += n;
    }

    public void setState(AgentState newState) {
        state = newState;
    }
}