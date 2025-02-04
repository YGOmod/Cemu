package info.cemu.Cemu.gameview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import info.cemu.Cemu.R;

import info.cemu.Cemu.nativeinterface.NativeGameTitles.Game;

public class GameAdapter extends ListAdapter<Game, GameAdapter.ViewHolder> {
    private final GameTitleClickAction gameTitleClickAction;
    private List<Game> orignalGameList;
    private String filterText;
    private Game selectedGame;

    public Game getSelectedGame() {
        return selectedGame;
    }

    public static final DiffUtil.ItemCallback<Game> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
            return oldItem.titleId() == newItem.titleId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
            return oldItem.path().equals(newItem.path()) &&
                    oldItem.titleId() == newItem.titleId() &&
                    oldItem.isFavorite() == newItem.isFavorite();
        }
    };

    public interface GameTitleClickAction {
        void action(Game game);
    }

    public GameAdapter(GameTitleClickAction gameTitleClickAction) {
        super(DIFF_CALLBACK);
        this.gameTitleClickAction = gameTitleClickAction;
    }

    @Override
    public void submitList(@Nullable List<Game> list) {
        orignalGameList = list;
        if (orignalGameList == null) {
            orignalGameList = new ArrayList<>();
        }
        if (filterText == null || filterText.isBlank()) {
            super.submitList(orignalGameList);
            return;
        }
        super.submitList(orignalGameList.stream().filter(g -> g.name().toLowerCase(Locale.US).contains(this.filterText)).collect(Collectors.toList()));
    }

    @NonNull
    @Override
    public GameAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_game, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameAdapter.ViewHolder holder, int position) {
        Game game = getItem(position);
        if (game == null) {
            return;
        }
        holder.icon.setImageBitmap(game.icon());
        holder.favoriteIcon.setVisibility(game.isFavorite() ? View.VISIBLE : View.GONE);
        holder.text.setText(game.name());
        holder.itemView.setOnClickListener(v -> gameTitleClickAction.action(game));
        holder.itemView.setOnLongClickListener(v -> {
            selectedGame = game;
            return false;
        });
    }

    public void setFilterText(String filterText) {
        if (filterText != null) {
            filterText = filterText.toLowerCase(Locale.US);
        }
        this.filterText = filterText;
        if (filterText == null || filterText.isBlank() || orignalGameList == null) {
            super.submitList(orignalGameList);
            return;
        }
        super.submitList(orignalGameList.stream().filter(g -> g.name().toLowerCase(Locale.US).contains(this.filterText)).collect(Collectors.toList()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView text;
        MaterialCardView favoriteIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.game_icon);
            text = itemView.findViewById(R.id.game_title);
            favoriteIcon = itemView.findViewById(R.id.game_favorite_icon);
        }
    }
}
