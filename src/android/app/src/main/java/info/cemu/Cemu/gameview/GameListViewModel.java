package info.cemu.Cemu.gameview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import info.cemu.Cemu.nativeinterface.NativeGameTitles;
import info.cemu.Cemu.nativeinterface.NativeGameTitles.Game;

public class GameListViewModel extends ViewModel {
    private final MutableLiveData<List<Game>> gamesData;

    private final TreeSet<Game> games = new TreeSet<>();

    public LiveData<List<Game>> getGames() {
        return gamesData;
    }

    public GameListViewModel() {
        this.gamesData = new MutableLiveData<>();
        NativeGameTitles.setGameTitleLoadedCallback(game -> {
            if (!isGameValid(game)) {
                return;
            }
            synchronized (GameListViewModel.this) {
                games.add(game);
                gamesData.postValue(new ArrayList<>(games));
            }
        });
    }

    private boolean isGameValid(Game game) {
        return game.path() != null && game.name() != null;
    }

    public void setGameTitleFavorite(Game game, boolean isFavorite) {
        synchronized (this) {
            if (!games.contains(game)) {
                return;
            }
            NativeGameTitles.setGameTitleFavorite(game.titleId(), isFavorite);
            games.remove(game);
            Game newGame = new Game(
                    game.titleId(),
                    game.path(),
                    game.name(),
                    game.version(),
                    game.dlc(),
                    game.region(),
                    game.lastPlayedYear(),
                    game.lastPlayedMonth(),
                    game.lastPlayedDay(),
                    game.minutesPlayed(),
                    isFavorite,
                    game.icon()
            );
            games.add(newGame);
            gamesData.postValue(new ArrayList<>(games));
        }
    }

    @Override
    protected void onCleared() {
        NativeGameTitles.setGameTitleLoadedCallback(null);
    }

    public void refreshGames() {
        games.clear();
        gamesData.setValue(null);
        NativeGameTitles.reloadGameTitles();
    }
}
