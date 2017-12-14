package model;

public class Gene extends NucleotidesHolder {
    private String name;

    public Gene(String name, String type, String path, int totalDinucleotide, int totalTrinucleotide) {
        super(type, path, totalDinucleotide, totalTrinucleotide);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return (super.getType() == null) ? "": super.getType();
    }
}
