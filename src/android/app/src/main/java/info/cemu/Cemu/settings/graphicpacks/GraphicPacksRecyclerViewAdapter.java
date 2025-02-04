package info.cemu.Cemu.settings.graphicpacks;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import info.cemu.Cemu.R;
import info.cemu.Cemu.guibasecomponents.RecyclerViewItem;

public class GraphicPacksRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static class GraphicPackSearchItemRecyclerViewItem implements RecyclerViewItem {
        private static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialTextView name;
            MaterialTextView path;
            MaterialCardView enabledIcon;

            public ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.graphic_pack_search_name);
                path = itemView.findViewById(R.id.graphic_pack_search_path);
                enabledIcon = itemView.findViewById(R.id.graphic_pack_enabled_icon);
            }
        }

        public interface OnClickCallback {
            void onClick();
        }

        private final GraphicPackDataNode dataNode;
        private final boolean hasInstalledTitleId;
        private final OnClickCallback onClickCallback;

        private GraphicPackSearchItemRecyclerViewItem(GraphicPackDataNode dataNode, OnClickCallback onClickCallback) {
            this.dataNode = dataNode;
            this.hasInstalledTitleId = dataNode.hasTitleIdInstalled();
            this.onClickCallback = onClickCallback;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_graphic_pack_search_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
            ViewHolder graphicPackViewHolder = (ViewHolder) viewHolder;
            graphicPackViewHolder.itemView.setOnClickListener(v -> onClickCallback.onClick());
            graphicPackViewHolder.name.setText(dataNode.getName());
            graphicPackViewHolder.path.setText(dataNode.getPath());
            if (dataNode.isEnabled()) {
                graphicPackViewHolder.enabledIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    public interface OnItemClickCallback {
        void onClick(GraphicPackDataNode graphicPackDataNode);
    }

    private final OnItemClickCallback onItemClickCallback;
    private boolean showInstalledGamesOnly;
    private String filterText = "";

    public GraphicPacksRecyclerViewAdapter(OnItemClickCallback onItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback;
    }

    private List<GraphicPackSearchItemRecyclerViewItem> recyclerViewItems = new ArrayList<>();
    private List<GraphicPackSearchItemRecyclerViewItem> filteredViewItems = new ArrayList<>();

    public void setShowInstalledOnly(boolean showInstalledGamesOnly) {
        this.showInstalledGamesOnly = showInstalledGamesOnly;
        filter();
    }

    public void setItems(List<GraphicPackDataNode> graphicPackDataNodes) {
        clearItems();
        recyclerViewItems = graphicPackDataNodes.stream()
                .sorted(Comparator.comparing(GraphicPackDataNode::getPath))
                .map(graphicPackDataNode ->
                        new GraphicPackSearchItemRecyclerViewItem(
                                graphicPackDataNode,
                                () -> GraphicPacksRecyclerViewAdapter.this.onItemClickCallback.onClick(graphicPackDataNode)
                        )).collect(Collectors.toList());
        filteredViewItems = recyclerViewItems;
        if (showInstalledGamesOnly) {
            filteredViewItems = filteredViewItems.stream().filter(g -> g.hasInstalledTitleId).collect(Collectors.toList());
        }
        notifyItemRangeInserted(0, filteredViewItems.size());
    }

    private void resetFilteredItems() {
        filteredViewItems = recyclerViewItems;
        if (showInstalledGamesOnly) {
            filteredViewItems = filteredViewItems.stream().filter(g -> g.hasInstalledTitleId).collect(Collectors.toList());
        }
        notifyDataSetChanged();
    }

    public void setFilterText(String filterText) {
        this.filterText = filterText;
        filter();
    }

    public void filter() {
        if (filterText.isBlank()) {
            resetFilteredItems();
            return;
        }
        var stringBuilder = new StringBuilder();
        String pattern = Arrays.stream(filterText.split(" "))
                .map(s -> "(?=.*" + Pattern.quote(s) + ")")
                .collect(() -> stringBuilder, StringBuilder::append, StringBuilder::append)
                .append(".*")
                .toString();
        var regex = Pattern.compile(pattern, CASE_INSENSITIVE);
        var filteredData = recyclerViewItems.stream();
        if (showInstalledGamesOnly) {
            filteredData = filteredData.filter(g -> g.hasInstalledTitleId);
        }
        filteredData = filteredData.filter(g -> regex.matcher(g.dataNode.getPath()).matches());
        filteredViewItems = filteredData.collect(Collectors.toList());
        notifyDataSetChanged();
    }

    public void clearItems() {
        if (filteredViewItems.isEmpty()) {
            return;
        }
        int itemsCount = filteredViewItems.size();
        filteredViewItems.clear();
        recyclerViewItems.clear();
        notifyItemRangeRemoved(0, itemsCount);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        return filteredViewItems.get(position).onCreateViewHolder(parent);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        filteredViewItems.get(position).onBindViewHolder(holder, this);
    }

    @Override
    public int getItemCount() {
        return filteredViewItems.size();
    }
}
