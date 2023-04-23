package org.joinmastodon.android.fragments.settings;

import org.joinmastodon.android.R;

import java.util.ArrayList;

public class SettingsMainFragment extends SettingsBaseFragment{
    @Override
    public void addItems(ArrayList<SettingsBaseFragment.Item> items) {
        items.add(new SettingsBaseFragment.SettingsCategoryItem(R.string.settings_theme, () -> {
            System.out.println("YAY");
        }, R.drawable.ic_fluent_color_24_regular));
    }
}
