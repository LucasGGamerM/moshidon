package org.joinmastodon.android;

import android.Manifest;
import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.mastodon.session.AccountSession;
import org.joinmastodon.android.api.mastodon.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.onboarding.AccountActivationFragment;
import org.joinmastodon.android.fragments.onboarding.CustomWelcomeFragment;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;
import org.parceler.Parcels;

import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;

public class MainActivity extends FragmentStackActivity{

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		UiUtils.setUserPreferredTheme(this);
		super.onCreate(savedInstanceState);

		if(savedInstanceState==null){
			if(AccountSessionManager.getInstance().getLoggedInAccounts().isEmpty()){
				showFragmentClearingBackStack(new CustomWelcomeFragment());
			}else{
				AccountSessionManager.getInstance().maybeUpdateLocalInfo();
				AccountSession session;
				Bundle args=new Bundle();
				Intent intent=getIntent();
				boolean fromNotification = intent.getBooleanExtra("fromNotification", false);
				boolean hasNotification = intent.hasExtra("notification");
				if(fromNotification){
					String accountID=intent.getStringExtra("accountID");
					try{
						session=AccountSessionManager.getInstance().getAccount(accountID);
						if(!hasNotification) args.putString("tab", "notifications");
					}catch(IllegalStateException x){
						session=AccountSessionManager.getInstance().getLastActiveAccount();
					}
				}else{
					session=AccountSessionManager.getInstance().getLastActiveAccount();
				}
				args.putString("account", session.getID());
				Fragment fragment=session.activated ? new HomeFragment() : new AccountActivationFragment();
				fragment.setArguments(args);
				if(fromNotification && hasNotification){
					Notification notification=Parcels.unwrap(intent.getParcelableExtra("notification"));
					showFragmentForNotification(notification, session.getID());
				} else if (intent.getBooleanExtra("compose", false)){
					showCompose();
				} else {
					showFragmentClearingBackStack(fragment);
					maybeRequestNotificationsPermission();
				}
			}
		}

		if(GithubSelfUpdater.needSelfUpdating()){
			GithubSelfUpdater.getInstance().maybeCheckForUpdates();
		}
	}

	@Override
	protected void onNewIntent(Intent intent){
		super.onNewIntent(intent);
		if(intent.getBooleanExtra("fromNotification", false)){
			String accountID=intent.getStringExtra("accountID");
			AccountSession accountSession;
			try{
				accountSession=AccountSessionManager.getInstance().getAccount(accountID);
				DomainManager.getInstance().setCurrentDomain(accountSession.domain);
			}catch(IllegalStateException x){
				return;
			}
			if(intent.hasExtra("notification")){
				Notification notification=Parcels.unwrap(intent.getParcelableExtra("notification"));
				showFragmentForNotification(notification, accountID);
			}else{
				AccountSessionManager.getInstance().setLastActiveAccountID(accountID);
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putString("tab", "notifications");
				Fragment fragment=new HomeFragment();
				fragment.setArguments(args);
				showFragmentClearingBackStack(fragment);
			}
		}else if(intent.getBooleanExtra("compose", false)){
			showCompose();
		}/*else if(intent.hasExtra(PackageInstaller.EXTRA_STATUS) && GithubSelfUpdater.needSelfUpdating()){
			GithubSelfUpdater.getInstance().handleIntentFromInstaller(intent, this);
		}*/
	}

	private void showFragmentForNotification(Notification notification, String accountID){
		Fragment fragment;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putBoolean("_can_go_back", true);
		try{
			notification.postprocess();
		}catch(ObjectValidationException x){
			Log.w("MainActivity", x);
			return;
		}
		if(notification.status!=null){
			fragment=new ThreadFragment();
			args.putParcelable("status", Parcels.wrap(notification.status));
		}else{
			fragment=new ProfileFragment();
			args.putParcelable("profileAccount", Parcels.wrap(notification.account));
		}
		fragment.setArguments(args);
		showFragment(fragment);
	}

	private void showCompose(){
		AccountSession session=AccountSessionManager.getInstance().getLastActiveAccount();
		if(session==null || !session.activated)
			return;
		ComposeFragment compose=new ComposeFragment();
		Bundle composeArgs=new Bundle();
		composeArgs.putString("account", session.getID());
		compose.setArguments(composeArgs);
		showFragment(compose);
	}

	private void maybeRequestNotificationsPermission(){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
		}
	}

	/**
	 * when opening app through a notification: if (thread) fragment "can go back", clear back stack
	 * and show home fragment. upstream's implementation doesn't require this as it opens home first
	 * and then immediately switches to the notification's ThreadFragment. this causes a black
	 * screen in megalodon, for some reason, so i'm working around this that way.
 	 */
	@Override
	public void onBackPressed() {
		Fragment currentFragment = getFragmentManager().findFragmentById(
				(fragmentContainers.get(fragmentContainers.size() - 1)).getId()
		);
		Bundle currentArgs = currentFragment.getArguments();
		if (this.fragmentContainers.size() == 1
				&& currentArgs != null
				&& currentArgs.getBoolean("_can_go_back", false)
				&& currentArgs.containsKey("account")) {
			Bundle args = new Bundle();
			args.putString("account", currentArgs.getString("account"));
			args.putString("tab", "notifications");
			Fragment fragment=new HomeFragment();
			fragment.setArguments(args);
			showFragmentClearingBackStack(fragment);
		} else {
			super.onBackPressed();
		}
	}
	@Override
	public void onProvideAssistContent(AssistContent outContent) {
		super.onProvideAssistContent(outContent);

		outContent.setWebUri(Uri.parse(DomainManager.getInstance().getCurrentDomain()));
	}

}
