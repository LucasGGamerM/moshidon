package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import org.joinmastodon.android.R;

import java.util.ArrayList;

import me.grishka.appkit.Nav;

public class SettingsMainFragment extends SettingsBaseFragment{
    @Override
    public void addItems(ArrayList<SettingsBaseFragment.Item> items) {
        items.add(new SettingsBaseFragment.SettingsCategoryItem(R.string.settings_theme, () -> {
            Bundle args = new Bundle();
            args.putString("account", accountID);
            Nav.go(getActivity(), SettingsAppearanceFragment.class, args);
        }, R.drawable.ic_fluent_color_24_regular));
    }
}
