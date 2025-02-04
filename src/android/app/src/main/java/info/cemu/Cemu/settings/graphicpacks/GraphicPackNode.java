package info.cemu.Cemu.settings.graphicpacks;

public abstract class GraphicPackNode {
    protected boolean titleIdInstalled;
    protected String name;

    public boolean hasTitleIdInstalled() {
        return titleIdInstalled;
    }

    public String getName() {
        return name;
    }
}