package org.joinmastodon.android.fragments.settings;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.PushNotification;
import org.joinmastodon.android.model.PushSubscription;

import java.util.ArrayList;

public class NotificationsFragment extends SettingsBaseFragment {
    @Override
    public void addItems(ArrayList<Item> items) {
        items.add(notificationPolicyItem = new NotificationPolicyItem());
        PushSubscription pushSubscription = getPushSubscription();
        boolean switchEnabled = pushSubscription.policy != PushSubscription.Policy.NONE;

        items.add(new SwitchItem(R.string.notify_favorites, R.drawable.ic_fluent_star_24_regular, pushSubscription.alerts.favourite, i -> onNotificationsChanged(PushNotification.Type.FAVORITE, i.checked), switchEnabled));
        items.add(new SwitchItem(R.string.notify_follow, R.drawable.ic_fluent_person_add_24_regular, pushSubscription.alerts.follow, i -> onNotificationsChanged(PushNotification.Type.FOLLOW, i.checked), switchEnabled));
        items.add(new SwitchItem(R.string.notify_reblog, R.drawable.ic_fluent_arrow_repeat_all_24_regular, pushSubscription.alerts.reblog, i -> onNotificationsChanged(PushNotification.Type.REBLOG, i.checked), switchEnabled));
        items.add(new SwitchItem(R.string.notify_mention, R.drawable.ic_fluent_mention_24_regular, pushSubscription.alerts.mention, i -> onNotificationsChanged(PushNotification.Type.MENTION, i.checked), switchEnabled));
        items.add(new SwitchItem(R.string.sk_notify_posts, R.drawable.ic_fluent_chat_24_regular, pushSubscription.alerts.status, i -> onNotificationsChanged(PushNotification.Type.STATUS, i.checked), switchEnabled));
        items.add(new SwitchItem(R.string.sk_notify_update, R.drawable.ic_fluent_history_24_regular, pushSubscription.alerts.update, i -> onNotificationsChanged(PushNotification.Type.UPDATE, i.checked), switchEnabled));
        items.add(new SwitchItem(R.string.sk_notify_poll_results, R.drawable.ic_fluent_poll_24_regular, pushSubscription.alerts.poll, i -> onNotificationsChanged(PushNotification.Type.POLL, i.checked), switchEnabled));

    }
}
