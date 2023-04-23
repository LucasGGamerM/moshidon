package org.joinmastodon.android.fragments.settings;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;

import java.util.ArrayList;

import me.grishka.appkit.utils.V;

public class BehaviourFragment extends SettingsBaseFragment{
    @Override
    public void addItems(ArrayList<Item> items) {
        items.add(new HeaderItem(R.string.settings_behavior));
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

        items.add(new HeaderItem(R.string.mo_composer_behavior));
        items.add(new ButtonItem(R.string.sk_settings_publish_button_text, R.drawable.ic_fluent_send_24_regular, b-> {
            updatePublishText(b);
            b.setOnClickListener(l -> {
                if(!GlobalUserPreferences.relocatePublishButton) {
                    FrameLayout inputWrap = new FrameLayout(getContext());
                    EditText input = new EditText(getContext());
                    input.setHint(R.string.publish);
                    input.setText(GlobalUserPreferences.publishButtonText.trim());
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(V.dp(16), V.dp(4), V.dp(16), V.dp(16));
                    input.setLayoutParams(params);
                    inputWrap.addView(input);
                    new M3AlertDialogBuilder(getContext()).setTitle(R.string.sk_settings_publish_button_text_title).setView(inputWrap)
                            .setPositiveButton(R.string.save, (d, which) -> {
                                GlobalUserPreferences.publishButtonText = input.getText().toString().trim();
                                GlobalUserPreferences.save();
                                updatePublishText(b);
                            })
                            .setNeutralButton(R.string.clear, (d, which) -> {
                                GlobalUserPreferences.publishButtonText = "";
                                GlobalUserPreferences.save();
                                updatePublishText(b);
                            })
                            .setNegativeButton(R.string.cancel, (d, which) -> {
                            })
                            .show();

                } else {
                    Toast.makeText(getActivity(), R.string.mo_disable_relocate_publish_button_to_enable_customization,
                            Toast.LENGTH_LONG).show();
                }
            });
        }));
        items.add(new SwitchItem(R.string.mo_relocate_publish_button, R.drawable.ic_fluent_arrow_autofit_down_24_regular, GlobalUserPreferences.relocatePublishButton, i->{
            GlobalUserPreferences.relocatePublishButton=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.mo_change_default_reply_visibility_to_unlisted, R.drawable.ic_fluent_lock_open_24_regular, GlobalUserPreferences.defaultToUnlistedReplies, i->{
            GlobalUserPreferences.defaultToUnlistedReplies=i.checked;
            GlobalUserPreferences.save();
        }));
        // TODO find a good icon for this setting
        items.add(new SwitchItem(R.string.mo_mention_reblogger_automatically, R.drawable.ic_fluent_balloon_24_regular, GlobalUserPreferences.mentionRebloggerAutomatically, i -> {
            GlobalUserPreferences.mentionRebloggerAutomatically=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.mo_disable_reminder_to_add_alt_text, R.drawable.ic_fluent_image_alt_text_24_regular, GlobalUserPreferences.disableAltTextReminder, i->{
            GlobalUserPreferences.disableAltTextReminder=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.sk_settings_prefix_reply_cw_with_re, R.drawable.ic_fluent_arrow_reply_24_regular, GlobalUserPreferences.prefixRepliesWithRe, i->{
            GlobalUserPreferences.prefixRepliesWithRe=i.checked;
            GlobalUserPreferences.save();
        }));
    }

    private void updatePublishText(Button btn) {
        if (GlobalUserPreferences.publishButtonText.isBlank()) btn.setText(R.string.publish);
        else btn.setText(GlobalUserPreferences.publishButtonText);
    }
}
