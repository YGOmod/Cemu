package info.cemu.Cemu.settings.graphicpacks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import info.cemu.Cemu.R;
import info.cemu.Cemu.guibasecomponents.RecyclerViewItem;
import info.cemu.Cemu.nativeinterface.NativeGraphicPacks;

public class GraphicPackRecyclerViewItem implements RecyclerViewItem {
    private static class GraphicPackViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        MaterialSwitch enableToggle;
        TextView description;

        public GraphicPackViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.graphic_pack_name);
            enableToggle = itemView.findViewById(R.id.graphic_pack_enable_toggle);
            description = itemView.findViewById(R.id.graphic_pack_description);
        }
    }

    public interface OnCheckedChangeListener {
        void onCheckedChange(boolean checked);
    }

    private final NativeGraphicPacks.GraphicPack graphicPack;
    private final OnCheckedChangeListener onCheckedChangeListener;

    public GraphicPackRecyclerViewItem(NativeGraphicPacks.GraphicPack graphicPack, OnCheckedChangeListener onCheckedChangeListener) {
        this.graphicPack = graphicPack;
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new GraphicPackViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_graphic_pack, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, RecyclerView.Adapter<RecyclerView.ViewHolder> recyclerViewAdapter) {
        var graphicPackViewHolder = (GraphicPackViewHolder) viewHolder;
        graphicPackViewHolder.name.setText(graphicPack.name);
        if (graphicPack.description != null) {
            graphicPackViewHolder.description.setText(graphicPack.description);
        } else {
            graphicPackViewHolder.description.setText(R.string.graphic_pack_no_description);
        }
        graphicPackViewHolder.enableToggle.setChecked(graphicPack.isActive());
        graphicPackViewHolder.enableToggle.setOnCheckedChangeListener((materialCheckBox, isChecked) -> {
            graphicPack.setActive(isChecked);
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChange(isChecked);
            }
        });
    }
}
