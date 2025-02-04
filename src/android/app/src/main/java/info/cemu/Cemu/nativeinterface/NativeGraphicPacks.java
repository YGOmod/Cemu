package info.cemu.Cemu.nativeinterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NativeGraphicPacks {
    public record GraphicPackBasicInfo(long id,
                                       String virtualPath,
                                       boolean enabled,
                                       ArrayList<Long> titleIds) {
    }

    public static native ArrayList<GraphicPackBasicInfo> getGraphicPackBasicInfos();

    public static class GraphicPackPreset {
        private final long graphicPackId;
        private final String category;
        private final ArrayList<String> presets;
        private String activePreset;

        @Override
        public int hashCode() {
            return Objects.hash(graphicPackId, category, presets, activePreset);
        }

        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object == this) {
                return true;
            }
            if (object instanceof GraphicPackPreset preset) {
                return this.hashCode() == preset.hashCode();
            }
            return false;
        }

        public GraphicPackPreset(long graphicPackId, String category, ArrayList<String> presets, String activePreset) {
            this.graphicPackId = graphicPackId;
            this.category = category;
            this.presets = presets;
            this.activePreset = activePreset;
        }

        public String getActivePreset() {
            return activePreset;
        }

        public void setActivePreset(String activePreset) {
            if (presets.stream().noneMatch(s -> s.equals(activePreset))) {
                throw new IllegalArgumentException("Trying to set an invalid preset: " + activePreset);
            }
            setGraphicPackActivePreset(graphicPackId, category, activePreset);
            this.activePreset = activePreset;
        }

        public String getCategory() {
            return category;
        }

        public ArrayList<String> getPresets() {
            return presets;
        }
    }

    public static class GraphicPack {
        public final long id;
        protected boolean active;
        public final String name;
        public final String description;
        public List<GraphicPackPreset> presets;

        public GraphicPack(long id, boolean active, String name, String description, ArrayList<GraphicPackPreset> presets) {
            this.id = id;
            this.active = active;
            this.name = name;
            this.description = description;
            this.presets = presets;
        }

        public boolean isActive() {
            return active;
        }

        public void reloadPresets() {
            presets = NativeGraphicPacks.getGraphicPackPresets(id);
        }

        public void setActive(boolean active) {
            this.active = active;
            setGraphicPackActive(id, active);
        }
    }

    public static native void refreshGraphicPacks();

    public static native GraphicPack getGraphicPack(long id);

    public static native void setGraphicPackActive(long id, boolean active);

    public static native void setGraphicPackActivePreset(long id, String category, String preset);

    public static native ArrayList<GraphicPackPreset> getGraphicPackPresets(long id);
}
