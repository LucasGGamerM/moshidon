package org.joinmastodon.android.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.PushSubscription;
import org.unifiedpush.android.connector.UnifiedPush;

public class UnifiedPushHelper {

	/**
	 * @param context
	 * @return `true` if UnifiedPush is used
	 */
	public static boolean isUnifiedPushEnabled(@NonNull Context context) {
		return UnifiedPush.getAckDistributor(context) != null;
	}

	/**
	 * If any distributor is installed on the device
	 * @param context
	 * @return `true` if at least one is installed
	 */
	public static boolean hasAnyDistributorInstalled(@NonNull Context context) {
		return !UnifiedPush.getDistributors(context).isEmpty();
	}

	public static void registerAllAccounts(@NonNull Context context) {
		for (AccountSession accountSession : AccountSessionManager.getInstance().getLoggedInAccounts()){
			String vapidKey = accountSession.app.vapidKey;
			// Sometimes this is null when the account's server has died (don't ask me how I know this)
			if (vapidKey == null) {
				// TODO: throw this on a translatable string and tell the user to log out and back in
				Toast.makeText(context, "Error on unified push subscription: no valid vapid key for account " + accountSession.getFullUsername(), Toast.LENGTH_LONG).show();
				break;
			}
			PushSubscription sub = accountSession.pushSubscription;
			if (sub == null || sub.standard) {
				vapidKey = vapidKey.replaceAll("=","");
			} else {
				// If we know the server doesn't support the _standard_ VAPID,
				// we register without vapid
				vapidKey = null;
			}
			UnifiedPush.register(
					context,
					accountSession.getID(),
					accountSession.self.fqn,
					vapidKey
			);
		}
	}

	public static void unregisterAllAccounts(@NonNull Context context) {
		for (AccountSession accountSession : AccountSessionManager.getInstance().getLoggedInAccounts()){
			UnifiedPush.unregister(
				context,
				accountSession.getID()
			);
			// use FCM again
			accountSession.getPushSubscriptionManager().registerAccountForPush(null);
		}
	}
}
