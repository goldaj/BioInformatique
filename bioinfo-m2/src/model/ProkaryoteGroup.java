package model;

public enum ProkaryoteGroup {
    DPANNGroup("DPANN group", "Archaea"),
    Euryarchaeota("Euryarchaeota", "Archaea"),
    TACKGroup("TACK group", "Archaea"),
    AnclassifiedArchaea("unclassified Archaea", "Archaea"),

    Acidobacteria("Acidobacteria", "Bacteria"),
    Aquificae("Aquificae", "Bacteria"),
    Caldiserica("Caldiserica", "Bacteria"),
    Chrysiogenetes("Chrysiogenetes", "Bacteria"),
    Deferribacteres("Deferribacteres", "Bacteria"),
    Elusimicrobia("Elusimicrobia", "Bacteria"),
    FCBGroup("FCB group", "Bacteria"),
    Fusobacteria("Fusobacteria", "Bacteria"),
    NitrospinaeTectomicrobiaGroup("Nitrospinae/Tectomicrobia group", "Bacteria"),
    Nitrospirae("Nitrospirae", "Bacteria"),
    PVCGroup("PVC group", "Bacteria"),
    Proteobacteria("Proteobacteria", "Bacteria"),
    Rhodothermaeota("Rhodothermaeota", "Bacteria"),
    Spirochaetes("Spirochaetes", "Bacteria"),
    Synergistetes("Synergistetes", "Bacteria"),
    TerrabacteriaGroup("Terrabacteria group", "Bacteria"),
    Thermodesulfobacteria("Thermodesulfobacteria", "Bacteria"),
    Thermotogae("Thermotogae", "Bacteria"),
    EnvironmentalSamples("environmental samples", "Bacteria"),
    UnclassifiedBacteria("unclassified Bacteria", "Bacteria");

    private final String name;
    private final String type;

    public static ProkaryoteGroup buildByName(String name) {
        for (ProkaryoteGroup prokaryoteGroup: ProkaryoteGroup.values()) {
            if (prokaryoteGroup.getName().trim().toLowerCase().equals(name.trim().toLowerCase())) {
                return prokaryoteGroup;
            }
        }
        return null;
    }

    ProkaryoteGroup(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
