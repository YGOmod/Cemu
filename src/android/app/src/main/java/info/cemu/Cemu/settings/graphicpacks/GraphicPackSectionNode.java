package info.cemu.Cemu.settings.graphicpacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import info.cemu.Cemu.nativeinterface.NativeGraphicPacks;

public class GraphicPackSectionNode extends GraphicPackNode {
    private int enabledGraphicPacksCount = 0;
    private final GraphicPackSectionNode parent;
    private final ArrayList<GraphicPackNode> children = new ArrayList<>();

    List<GraphicPackNode> getChildren() {
        return children;
    }

    private GraphicPackSectionNode(String name, boolean hasTitleIdInstalled, GraphicPackSectionNode parent) {
        this.name = name;
        this.titleIdInstalled = hasTitleIdInstalled;
        this.parent = parent;
    }

    public GraphicPackSectionNode() {
        this(null, false, null);
    }

    void clear() {
        children.clear();
    }

    public void addGraphicPackDataByTokens(NativeGraphicPacks.GraphicPackBasicInfo graphicPackBasicInfo, boolean hasTitleIdInstalled) {
        var node = this;
        var tokens = Arrays.asList(graphicPackBasicInfo.virtualPath().split("/"));
        if (tokens.isEmpty()) {
            return;
        }
        for (var token : tokens.subList(0, tokens.size() - 1)) {
            node = getOrAddSectionByToken(node, token, node.children, hasTitleIdInstalled);
        }
        var dataNode = new GraphicPackDataNode(
                graphicPackBasicInfo.id(),
                tokens.get(tokens.size() - 1),
                graphicPackBasicInfo.virtualPath(),
                graphicPackBasicInfo.enabled(),
                hasTitleIdInstalled,
                node
        );
        if (graphicPackBasicInfo.enabled()) {
            node.updateEnabledCount(true);
        }
        node.children.add(dataNode);
    }

    private GraphicPackSectionNode getOrAddSectionByToken(GraphicPackSectionNode parent, String token, ArrayList<GraphicPackNode> graphicPackNodes, boolean hasTitleIdInstalled) {
        var sectionNodeOptional = graphicPackNodes.stream()
                .filter(g -> (g instanceof GraphicPackSectionNode) && g.name.equals(token))
                .findFirst();
        if (sectionNodeOptional.isPresent()) {
            return (GraphicPackSectionNode) sectionNodeOptional.get();
        }
        var sectionNode = new GraphicPackSectionNode(token, hasTitleIdInstalled, parent);
        graphicPackNodes.add(sectionNode);
        return sectionNode;
    }

    public void sort() {
        children.forEach(node -> {
            if (node instanceof GraphicPackSectionNode sectionNode) {
                sectionNode.sort();
            }
        });
        children.sort(Comparator.comparing(o -> o.name));
    }

    public void updateEnabledCount(boolean enabled) {
        enabledGraphicPacksCount = Math.max(0, enabledGraphicPacksCount + (enabled ? 1 : -1));
        if (parent != null) {
            parent.updateEnabledCount(enabled);
        }
    }

    public int getEnabledGraphicPacksCount() {
        return enabledGraphicPacksCount;
    }
}