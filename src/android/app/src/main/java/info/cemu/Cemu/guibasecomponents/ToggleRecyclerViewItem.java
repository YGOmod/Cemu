package info.cemu.Cemu.guibasecomponents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import info.cemu.Cemu.R;

public class ToggleRecyclerViewItem implements RecyclerViewItem {
    public interface OnCheckedChangeListener {
        void onCheckChanged(boolean checked);
    }

    private static class ToggleViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        TextView description;
        MaterialSwitch toggle;

        public ToggleViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.toggle_label);
            description = itemView.findViewById(R.id.toggle_description);
            toggle = itemView.findViewById(R.id.toggle);
        }
    }

    private final String label;
    private final String description;
    private boolean checked;
    private final OnCheckedChangeListener onCheckedChangeListener;

    public ToggleRecyclerViewItem(String label, String description, boolean checked, OnCheckedChangeListener onCheckedChangeListener) {
        this.label = label;
        this.description = description;
        this.checked = checked;
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ToggleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_toggle, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        ToggleViewHolder toggleViewHolder = (ToggleViewHolder) viewHolder;
        toggleViewHolder.label.setText(label);
        toggleViewHolder.description.setText(description);
        toggleViewHolder.toggle.setChecked(checked);
        toggleViewHolder.itemView.setOnClickListener(view -> {
            checked = !toggleViewHolder.toggle.isChecked();
            toggleViewHolder.toggle.setChecked(checked);
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckChanged(checked);
            }
        });
    }
}
