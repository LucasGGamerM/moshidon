package org.joinmastodon.android.fragments.settings;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;

public class SettingsAppearanceFragment extends SettingsBaseFragment {
    @Override
    public void addItems(ArrayList<Item> items) {
        items.add(themeItem SettingsBaseFragment.ThemeItem());
        items.add(new SettingsFragment.ButtonItem(R.string.sk_settings_color_palette, R.drawable.ic_fluent_color_24_regular, b -> {
            PopupMenu popupMenu = new PopupMenu(getActivity(), b, Gravity.CENTER_HORIZONTAL);
            popupMenu.inflate(R.menu.color_palettes);
            popupMenu.getMenu().findItem(R.id.m3_color).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
            popupMenu.setOnMenuItemClickListener(SettingsBaseFragment.this::onColorPreferenceClick);
            b.setOnTouchListener(popupMenu.getDragToOpenListener());
            b.setOnClickListener(v -> popupMenu.show());
            b.setText(switch (GlobalUserPreferences.color) {
                case MATERIAL3 -> R.string.sk_color_palette_material3;
                case PINK -> R.string.sk_color_palette_pink;
                case PURPLE -> R.string.sk_color_palette_purple;
                case GREEN -> R.string.sk_color_palette_green;
                case BLUE -> R.string.sk_color_palette_blue;
                case BROWN -> R.string.sk_color_palette_brown;
                case RED -> R.string.sk_color_palette_red;
                case YELLOW -> R.string.sk_color_palette_yellow;
                case NORD -> R.string.mo_color_palette_nord;
            });
        }));
        items.add(new SettingsBaseFragment.SwitchItem(R.string.theme_true_black, R.drawable.ic_fluent_dark_theme_24_regular, GlobalUserPreferences.trueBlackTheme, this::onTrueBlackThemeChanged));
        items.add(new SettingsBaseFragment.SwitchItem(R.string.sk_disable_marquee, R.drawable.ic_fluent_text_more_24_regular, GlobalUserPreferences.disableMarquee, i -> {
            GlobalUserPreferences.disableMarquee = i.checked;
            GlobalUserPreferences.save();
            needAppRestart = true;
        }));
        items.add(new SettingsBaseFragment.SwitchItem(R.string.sk_settings_uniform_icon_for_notifications, R.drawable.ic_ntf_logo, GlobalUserPreferences.uniformNotificationIcon, i -> {
            GlobalUserPreferences.uniformNotificationIcon = i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SettingsBaseFragment.SwitchItem(R.string.sk_settings_reduce_motion, R.drawable.ic_fluent_star_emphasis_24_regular, GlobalUserPreferences.reduceMotion, i -> {
            GlobalUserPreferences.reduceMotion = i.checked;
            GlobalUserPreferences.save();
            needAppRestart = true;
        }));
    }
}

