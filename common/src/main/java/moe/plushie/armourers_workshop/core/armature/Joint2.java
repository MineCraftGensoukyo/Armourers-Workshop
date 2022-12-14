package moe.plushie.armourers_workshop.core.armature;

import moe.plushie.armourers_workshop.api.client.IJoint;

public class Joint2 implements IJoint {

    private int id;
    private final String name;

    public Joint2(String name) {
        this.name = name;
        this.id = 0;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Joint2)) return false;
        Joint2 joint2 = (Joint2) o;
        return name.equals(joint2.name);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return name + "@" + id;
    }
}
