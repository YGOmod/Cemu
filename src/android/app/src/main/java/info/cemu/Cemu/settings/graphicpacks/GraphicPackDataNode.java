package info.cemu.Cemu.settings.graphicpacks;

public class GraphicPackDataNode extends GraphicPackNode {
    private final long id;
    private final String path;
    private final GraphicPackSectionNode parentNode;
    private boolean enabled;

    public GraphicPackDataNode(long id, String name, String path, boolean enabled, boolean hasTitleIdInstalled, GraphicPackSectionNode parentNode) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.titleIdInstalled = hasTitleIdInstalled;
        this.parentNode = parentNode;
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (parentNode != null) {
            parentNode.updateEnabledCount(enabled);
        }
    }
}