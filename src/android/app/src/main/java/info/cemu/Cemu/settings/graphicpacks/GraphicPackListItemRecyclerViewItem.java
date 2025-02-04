package info.cemu.Cemu.settings.graphicpacks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.Objects;

import info.cemu.Cemu.R;
import info.cemu.Cemu.guibasecomponents.RecyclerViewItem;

public class GraphicPackListItemRecyclerViewItem implements RecyclerViewItem {

    private static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView title;
        ImageView icon;
        MaterialTextView enabledGraphicPacksCount;
        ImageView enabledIcon;
        MaterialCardView graphicPackExtraInfo;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.graphic_pack_text);
            icon = itemView.findViewById(R.id.graphic_pack_icon);
            enabledGraphicPacksCount = itemView.findViewById(R.id.graphic_pack_enabled_count);
            enabledIcon = itemView.findViewById(R.id.graphic_pack_enabled_icon);
            graphicPackExtraInfo = itemView.findViewById(R.id.graphic_pack_extra_info);
        }
    }

    public interface OnClickCallback {
        void onClick();
    }


    private final String text;
    public final boolean hasInstalledTitleId;
    private final OnClickCallback onClickCallback;
    private final GraphicPackNode graphicPackNode;

    public GraphicPackListItemRecyclerViewItem(GraphicPackNode node, OnClickCallback onClickCallback) {
        this.text = node.getName();
        this.hasInstalledTitleId = node.hasTitleIdInstalled();
        graphicPackNode = Objects.requireNonNull(node);
        this.onClickCallback = onClickCallback;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_graphic_pack_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        ViewHolder graphicPackViewHolder = (ViewHolder) viewHolder;
        graphicPackViewHolder.itemView.setOnClickListener(v -> onClickCallback.onClick());
        graphicPackViewHolder.title.setText(text);
        if (graphicPackNode instanceof GraphicPackSectionNode sectionNode) {
            configureGraphicPackSection(graphicPackViewHolder, sectionNode);
        } else if (graphicPackNode instanceof GraphicPackDataNode dataNode) {
            configureGraphicPack(graphicPackViewHolder, dataNode);
        }
    }

    private void configureGraphicPackSection(ViewHolder viewHolder, GraphicPackSectionNode sectionNode) {
        viewHolder.icon.setImageResource(R.drawable.ic_lists);
        int enabledGraphicPacksCount = sectionNode.getEnabledGraphicPacksCount();
        if (enabledGraphicPacksCount == 0) {
            return;
        }
        String graphicPacksCountText = enabledGraphicPacksCount >= 100 ? "99+" : String.valueOf(enabledGraphicPacksCount);
        viewHolder.enabledGraphicPacksCount.setText(graphicPacksCountText);
        viewHolder.enabledGraphicPacksCount.setVisibility(View.VISIBLE);
    }

    private void configureGraphicPack(ViewHolder viewHolder, GraphicPackDataNode dataNode) {
        viewHolder.icon.setImageResource(R.drawable.ic_package_2);
        if (dataNode.isEnabled()) {
            viewHolder.enabledIcon.setVisibility(View.VISIBLE);
        }
    }
}
