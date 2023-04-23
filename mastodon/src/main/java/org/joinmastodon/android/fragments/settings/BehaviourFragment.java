package org.joinmastodon.android.fragments.settings;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;

import java.util.ArrayList;

public class BehaviourFragment extends SettingsBaseFragment{
    @Override
    public void addItems(ArrayList<Item> items) {
        items.add(new SwitchItem(R.string.settings_gif, R.drawable.ic_fluent_gif_24_regular, GlobalUserPreferences.playGifs, i->{
            GlobalUserPreferences.playGifs=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.settings_custom_tabs, R.drawable.ic_fluent_link_24_regular, GlobalUserPreferences.useCustomTabs, i->{
            GlobalUserPreferences.useCustomTabs=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.mo_hide_compose_button_while_scrolling_setting, R.drawable.ic_fluent_edit_24_regular, GlobalUserPreferences.enableFabAutoHide, i->{
            GlobalUserPreferences.enableFabAutoHide =i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.mo_load_remote_followers, R.drawable.ic_fluent_people_24_regular, GlobalUserPreferences.loadRemoteAccountFollowers, i -> {
            GlobalUserPreferences.loadRemoteAccountFollowers=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.sk_settings_show_interaction_counts, R.drawable.ic_fluent_number_row_24_regular, GlobalUserPreferences.showInteractionCounts, i->{
            GlobalUserPreferences.showInteractionCounts=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.sk_settings_always_reveal_content_warnings, R.drawable.ic_fluent_chat_warning_24_regular, GlobalUserPreferences.alwaysExpandContentWarnings, i->{
            GlobalUserPreferences.alwaysExpandContentWarnings=i.checked;
            GlobalUserPreferences.save();
        }));

//		items.add(new SwitchItem(R.string.sk_settings_show_differentiated_notification_icons, R.drawable.ic_ntf_logo, GlobalUserPreferences.showUniformPushNoticationIcons, this::onNotificationStyleChanged));
        items.add(new SwitchItem(R.string.sk_tabs_disable_swipe, R.drawable.ic_fluent_swipe_right_24_regular, GlobalUserPreferences.disableSwipe, i->{
            GlobalUserPreferences.disableSwipe=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.mo_disable_double_tap_to_swipe_between_tabs, R.drawable.ic_fluent_double_tap_swipe_right_24_regular, GlobalUserPreferences.disableDoubleTapToSwipe, i->{
            GlobalUserPreferences.disableDoubleTapToSwipe=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.sk_settings_confirm_before_reblog, R.drawable.ic_fluent_checkmark_circle_24_regular, GlobalUserPreferences.confirmBeforeReblog, i->{
            GlobalUserPreferences.confirmBeforeReblog=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.mo_swap_bookmark_with_reblog, R.drawable.ic_boost, GlobalUserPreferences.swapBookmarkWithBoostAction, i -> {
            GlobalUserPreferences.swapBookmarkWithBoostAction=i.checked;
            GlobalUserPreferences.save();
        }));
    }
}
