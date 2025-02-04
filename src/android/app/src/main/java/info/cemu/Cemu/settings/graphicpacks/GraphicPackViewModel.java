package info.cemu.Cemu.settings.graphicpacks;

import androidx.lifecycle.ViewModel;

public class GraphicPackViewModel extends ViewModel {
    private GraphicPackNode graphicPackNode;

    public GraphicPackViewModel() {
    }

    public GraphicPackNode getGraphicPackNode() {
        return graphicPackNode;
    }

    public void setGraphicPackNode(GraphicPackNode graphicPackNode) {
        this.graphicPackNode = graphicPackNode;
    }
}