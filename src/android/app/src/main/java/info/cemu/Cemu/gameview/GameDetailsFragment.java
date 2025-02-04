package info.cemu.Cemu.gameview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import info.cemu.Cemu.R;
import info.cemu.Cemu.databinding.FragmentGameDetailsBinding;
import info.cemu.Cemu.nativeinterface.NativeGameTitles;
import info.cemu.Cemu.nativeinterface.NativeGameTitles.Game;

public class GameDetailsFragment extends Fragment {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentGameDetailsBinding binding = FragmentGameDetailsBinding.inflate(inflater, container, false);
        var game = new ViewModelProvider(requireActivity()).get(GameViewModel.class).getGame();
        binding.gameTitleName.setText(game.name());
        binding.titleVersion.setText(String.valueOf(game.version()));
        if (game.icon() != null) {
            binding.titleIcon.setImageBitmap(game.icon());
        }
        if (game.dlc() != 0) {
            binding.titleDlc.setText(String.valueOf(game.dlc()));
        }
        binding.titleTimePlayed.setText(getTimePlayed(game));
        binding.titleLastPlayed.setText(getLastPlayedDate(game));
        binding.titleId.setText(String.format("%016x", game.titleId()));
        binding.titleRegion.setText(getRegionName(game));
        NavigationUI.setupWithNavController(binding.gameDetailsToolbar, NavHostFragment.findNavController(this), new AppBarConfiguration.Builder().build());
        return binding.getRoot();
    }

    private String getLastPlayedDate(Game game) {
        if (game.lastPlayedYear() == 0) {
            return getString(R.string.never_played);
        }
        LocalDate lastPlayedDate = LocalDate.of(game.lastPlayedYear(), game.lastPlayedMonth(), game.lastPlayedDay());
        return DATE_FORMATTER.format(lastPlayedDate);
    }

    private String getTimePlayed(Game game) {
        if (game.minutesPlayed() == 0) {
            return getString(R.string.never_played);
        }
        if (game.minutesPlayed() < 60) {
            return getString(R.string.minutes_played, game.minutesPlayed());
        }
        return getString(R.string.hours_minutes_played, game.minutesPlayed() / 60, game.minutesPlayed() % 60);
    }

    private @StringRes() int getRegionName(Game game) {
        return switch (game.region()) {
            case NativeGameTitles.CONSOLE_REGION_JPN -> R.string.console_region_japan;
            case NativeGameTitles.CONSOLE_REGION_USA -> R.string.console_region_usa;
            case NativeGameTitles.CONSOLE_REGION_EUR -> R.string.console_region_europe;
            case NativeGameTitles.CONSOLE_REGION_AUS_DEPR -> R.string.console_region_australia;
            case NativeGameTitles.CONSOLE_REGION_CHN -> R.string.console_region_china;
            case NativeGameTitles.CONSOLE_REGION_KOR -> R.string.console_region_korea;
            case NativeGameTitles.CONSOLE_REGION_TWN -> R.string.console_region_taiwan;
            case NativeGameTitles.CONSOLE_REGION_AUTO -> R.string.console_region_auto;
            default -> R.string.console_region_many;
        };
    }
}
