package info.cemu.Cemu.settings.graphicpacks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.IntStream;

import info.cemu.Cemu.R;
import info.cemu.Cemu.databinding.LayoutGenericRecyclerViewBinding;
import info.cemu.Cemu.guibasecomponents.GenericRecyclerViewAdapter;
import info.cemu.Cemu.guibasecomponents.SingleSelectionRecyclerViewItem;
import info.cemu.Cemu.guibasecomponents.StringSelectionAdapter;
import info.cemu.Cemu.nativeinterface.NativeGraphicPacks;

public class GraphicPacksFragment extends Fragment {
    private final GenericRecyclerViewAdapter genericRecyclerViewAdapter = new GenericRecyclerViewAdapter();
    private GraphicPackViewModel graphicPackPreviousViewModel;
    private GraphicPackViewModel graphicPackCurrentViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavController navController = NavHostFragment.findNavController(this);
        graphicPackPreviousViewModel = new ViewModelProvider(Objects.requireNonNull(navController.getPreviousBackStackEntry()))
                .get(GraphicPackViewModel.class);
        graphicPackCurrentViewModel = new ViewModelProvider(Objects.requireNonNull(navController.getCurrentBackStackEntry()))
                .get(GraphicPackViewModel.class);
        fillData();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var binding = LayoutGenericRecyclerViewBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(genericRecyclerViewAdapter);

        return binding.getRoot();
    }

    private void fillData() {
        var graphicNode = graphicPackPreviousViewModel.getGraphicPackNode();
        if (graphicNode instanceof GraphicPackSectionNode sectionNode) {
            fillData(sectionNode);
        } else if (graphicNode instanceof GraphicPackDataNode dataNode) {
            fillData(dataNode);
        }
    }

    private void fillData(GraphicPackSectionNode graphicPackSectionNode) {
        graphicPackSectionNode.getChildren().forEach(node -> {
            GraphicPackListItemRecyclerViewItem graphicPackItemRecyclerViewItem = new GraphicPackListItemRecyclerViewItem(node,
                    () -> {
                        graphicPackCurrentViewModel.setGraphicPackNode(node);
                        Bundle bundle = new Bundle();
                        bundle.putString("title", node.name);
                        NavHostFragment.findNavController(GraphicPacksFragment.this)
                                .navigate(R.id.action_graphicPacksFragment_self, bundle);
                    });
            genericRecyclerViewAdapter.addRecyclerViewItem(graphicPackItemRecyclerViewItem);
        });
    }

    private void fillData(GraphicPackDataNode graphicPackDataNode) {
        genericRecyclerViewAdapter.clearRecyclerViewItems();
        var graphicPack = NativeGraphicPacks.getGraphicPack(graphicPackDataNode.getId());
        genericRecyclerViewAdapter.addRecyclerViewItem(new GraphicPackRecyclerViewItem(graphicPack, graphicPackDataNode::setEnabled));
        fillPresets(graphicPack);
    }

    private void fillPresets(NativeGraphicPacks.GraphicPack graphicPack) {
        var recyclerViewItems = new ArrayList<SingleSelectionRecyclerViewItem<String>>();
        IntStream.range(0, graphicPack.presets.size()).forEach(index -> {
            var graphicPackPreset = graphicPack.presets.get(index);
            String category = graphicPackPreset.getCategory();
            if (category == null) {
                category = getString(R.string.active_preset_category);
            }
            var recyclerViewItem = new SingleSelectionRecyclerViewItem<>(
                    category,
                    graphicPackPreset.getActivePreset(),
                    new StringSelectionAdapter(
                            graphicPackPreset.getPresets(),
                            graphicPackPreset.getActivePreset()
                    ),
                    (selectedValue, selectionRecyclerViewItem) -> {
                        var oldPresets = graphicPack.presets;
                        graphicPack.presets.get(index).setActivePreset(selectedValue);
                        selectionRecyclerViewItem.setDescription(selectedValue);
                        graphicPack.reloadPresets();
                        if (!oldPresets.equals(graphicPack.presets)) {
                            recyclerViewItems.forEach(genericRecyclerViewAdapter::removeRecyclerViewItem);
                            fillPresets(graphicPack);
                        }
                    }
            );
            recyclerViewItems.add(recyclerViewItem);
            genericRecyclerViewAdapter.addRecyclerViewItem(recyclerViewItem);
        });
    }
}