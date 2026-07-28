package model;

// A parking floor (level) of the building. Holds many slots.
public class Floor {
    private int id;
    private String name;
    private int level;

    public Floor(int id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getLevel() { return level; }

    // Shown directly in the floor JComboBox.
    @Override
    public String toString() {
        return name + " (Level " + level + ")";
    }
}
