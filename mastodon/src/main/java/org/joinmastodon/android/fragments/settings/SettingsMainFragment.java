package org.joinmastodon.android.fragments.settings;

import org.joinmastodon.android.R;

import java.util.ArrayList;

public class SettingsMainFragment extends SettingsBaseFragment{
    @Override
    public void addItems(ArrayList<SettingsBaseFragment.Item> items) {
        items.add(new SettingsCategoryItem(R.string.settings_theme, SettingsAppearanceFragment.class, R.drawable.ic_fluent_color_24_regular));
        items.add(new SettingsCategoryItem(R.string.settings_behavior, BehaviourFragment.class, R.drawable.ic_fluent_chat_settings_24_regular));
    }
}