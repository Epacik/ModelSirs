package xyz.epat.modelsirs.agents;

/**
 * Modifier of a global simulation setting
 */
public class AgentModifier {

    public AgentModifier(String name, float value, AgentModifierType type){

        this.name = name;
        this.value = value;
        this.type = type;
        permanent = true;
    }
    public AgentModifier(String name, float value, AgentModifierType type, long tickOfDeath){

        this.name = name;
        this.value = value;
        this.type = type;
        this.tickOfDeath = tickOfDeath;
        permanent = false;
    }

    private final String name;
    private final float value;
    private final AgentModifierType type;
    private final boolean permanent;
    private long tickOfDeath;

    public String getName() {
        return name;
    }

    public float getValue() {
        return value;
    }

    public AgentModifierType getType() {
        return type;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public long getTickOfDeath() {
        return tickOfDeath;
    }
}


