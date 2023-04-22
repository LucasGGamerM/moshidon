package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import org.joinmastodon.android.DomainManager;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.E;
import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.GetNotifications;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.AllNotificationsSeenEvent;
import org.joinmastodon.android.events.NotificationReceivedEvent;
import org.joinmastodon.android.fragments.discover.DiscoverAccountsFragment;
import org.joinmastodon.android.fragments.discover.DiscoverFragment;
import org.joinmastodon.android.fragments.onboarding.OnboardingFollowSuggestionsFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.ui.AccountSwitcherSheet;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.TabBar;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class HomeFragment extends AppKitFragment implements OnBackPressedListener{
	private FragmentRootLinearLayout content;

	private HomeTabFragment homeTabFragment;

	private NotificationsFragment notificationsFragment;
	private DiscoverFragment searchFragment;
	private ProfileFragment profileFragment;
	private TabBar tabBar;
	private View tabBarWrap;
	private ImageView tabBarAvatar;
	private ImageView notificationTabIcon;
	@IdRes
	private int currentTab=R.id.tab_home;

	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(this);
		accountID=getArguments().getString("account");
		setTitle(R.string.mo_app_name);

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		if(savedInstanceState==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);

			homeTabFragment=new HomeTabFragment();
			homeTabFragment.setArguments(args);

			args=new Bundle(args);
			args.putBoolean("noAutoLoad", true);
			searchFragment=new DiscoverFragment();
			searchFragment.setArguments(args);
			notificationsFragment=new NotificationsFragment();
			notificationsFragment.setArguments(args);
			args=new Bundle(args);
			args.putParcelable("profileAccount", Parcels.wrap(AccountSessionManager.getInstance().getAccount(accountID).self));
			args.putBoolean("noAutoLoad", true);
			profileFragment=new ProfileFragment();
			profileFragment.setArguments(args);
		}

	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		content=new FragmentRootLinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		FrameLayout fragmentContainer=new FrameLayout(getActivity());
		fragmentContainer.setId(me.grishka.appkit.R.id.fragment_wrap);
		content.addView(fragmentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		inflater.inflate(R.layout.tab_bar, content);
		tabBar=content.findViewById(R.id.tabbar);
		tabBar.setListeners(this::onTabSelected, this::onTabLongClick);
		tabBarWrap=content.findViewById(R.id.tabbar_wrap);

		tabBarAvatar=tabBar.findViewById(R.id.tab_profile_ava);
		tabBarAvatar.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setOval(0, 0, view.getWidth(), view.getHeight());
			}
		});
		tabBarAvatar.setClipToOutline(true);
		Account self=AccountSessionManager.getInstance().getAccount(accountID).self;
		ViewImageLoader.load(tabBarAvatar, null, new UrlImageLoaderRequest(self.avatar, V.dp(28), V.dp(28)));

		notificationTabIcon=content.findViewById(R.id.tab_notifications);
		updateNotificationBadge();

		if(savedInstanceState==null){
			getChildFragmentManager().beginTransaction()
					.add(me.grishka.appkit.R.id.fragment_wrap, homeTabFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, searchFragment).hide(searchFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, notificationsFragment).hide(notificationsFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, profileFragment).hide(profileFragment)
					.commit();


			String defaultTab=getArguments().getString("tab");
			if("notifications".equals(defaultTab)){
				tabBar.selectTab(R.id.tab_notifications);
				fragmentContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						fragmentContainer.getViewTreeObserver().removeOnPreDrawListener(this);
						onTabSelected(R.id.tab_notifications);
						return true;
					}
				});
			}
		}

		return content;
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);

		if(savedInstanceState==null) return;


		homeTabFragment=(HomeTabFragment) getChildFragmentManager().getFragment(savedInstanceState, "homeTabFragment");

		searchFragment=(DiscoverFragment) getChildFragmentManager().getFragment(savedInstanceState, "searchFragment");
		notificationsFragment=(NotificationsFragment) getChildFragmentManager().getFragment(savedInstanceState, "notificationsFragment");
		profileFragment=(ProfileFragment) getChildFragmentManager().getFragment(savedInstanceState, "profileFragment");
		currentTab=savedInstanceState.getInt("selectedTab");
		Fragment current=fragmentForTab(currentTab);

		getChildFragmentManager().beginTransaction()
				.hide(homeTabFragment)
				.hide(searchFragment)
				.hide(notificationsFragment)
				.hide(profileFragment)
				.show(current)
				.commit();

		maybeTriggerLoading(current);
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		if (!hidden && fragmentForTab(currentTab) instanceof  DomainDisplay display)
			DomainManager.getInstance().setCurrentDomain(display.getDomain());
		fragmentForTab(currentTab).onHiddenChanged(hidden);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return currentTab!=R.id.tab_profile && !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			tabBarWrap.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(36)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
		WindowInsets topOnlyInsets=insets.replaceSystemWindowInsets(0, insets.getSystemWindowInsetTop(), 0, 0);

		homeTabFragment.onApplyWindowInsets(topOnlyInsets);

		searchFragment.onApplyWindowInsets(topOnlyInsets);
		notificationsFragment.onApplyWindowInsets(topOnlyInsets);
		profileFragment.onApplyWindowInsets(topOnlyInsets);
	}

	private Fragment fragmentForTab(@IdRes int tab){
		if(tab==R.id.tab_home){
			return homeTabFragment;
		}else if(tab==R.id.tab_search){
			return searchFragment;
		}else if(tab==R.id.tab_notifications){
			return notificationsFragment;
		}else if(tab==R.id.tab_profile){
			return profileFragment;
		}
		throw new IllegalArgumentException();
	}

	private void onTabSelected(@IdRes int tab){
		Fragment newFragment=fragmentForTab(tab);
		if(tab==currentTab){
			if(tab == R.id.tab_search){
				if(newFragment instanceof ScrollableToTop scrollable)
					scrollable.scrollToTop();
				searchFragment.selectSearch();
				return;
			}
			if(newFragment instanceof ScrollableToTop scrollable)
				scrollable.scrollToTop();
			return;
		}
		if(tab==currentTab && tab == R.id.tab_search){
			if(newFragment instanceof ScrollableToTop scrollable)
				scrollable.scrollToTop();
			return;
		}

		if (newFragment instanceof DomainDisplay display) {
			DomainManager.getInstance().setCurrentDomain(display.getDomain());
		}

		getChildFragmentManager().beginTransaction().hide(fragmentForTab(currentTab)).show(newFragment).commit();
		maybeTriggerLoading(newFragment);
		currentTab=tab;
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}

	private void maybeTriggerLoading(Fragment newFragment){
		if(newFragment instanceof LoaderFragment lf){
			if(!lf.loaded && !lf.dataLoading)
				lf.loadData();
		}else if(newFragment instanceof DiscoverFragment){
			((DiscoverFragment) newFragment).loadData();
		}else if(newFragment instanceof NotificationsFragment){
			((NotificationsFragment) newFragment).loadData();
			// TODO make an interface?
			NotificationManager nm=getActivity().getSystemService(NotificationManager.class);
			for (StatusBarNotification notification : nm.getActiveNotifications()) {
				if (accountID.equals(notification.getTag())) {
					nm.cancel(accountID, notification.getId());
				}
			}
		}
	}

	private boolean onTabLongClick(@IdRes int tab){
		if(tab==R.id.tab_profile){
			ArrayList<String> options=new ArrayList<>();
			for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
				options.add(session.self.displayName+"\n("+session.self.username+"@"+session.domain+")");
			}
			new AccountSwitcherSheet(getActivity(), true, true, false, accountSession -> {
				getActivity().finish();
				getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
			}).show();
			return true;
		}
		if(tab==R.id.tab_search){
			onTabSelected(R.id.tab_search);
			tabBar.selectTab(R.id.tab_search);
			searchFragment.selectSearch();
			return true;
		}
		if(tab==R.id.tab_home){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), OnboardingFollowSuggestionsFragment.class, args);
		}
		return false;
	}

	@Override
	public boolean onBackPressed(){
		if(currentTab==R.id.tab_profile)
			if (profileFragment.onBackPressed()) return true;
		if(currentTab==R.id.tab_search)
			if (searchFragment.onBackPressed()) return true;
		if (currentTab!=R.id.tab_home) {
			tabBar.selectTab(R.id.tab_home);
			onTabSelected(R.id.tab_home);
			return true;
		} else {
			return homeTabFragment.onBackPressed();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt("selectedTab", currentTab);

		if (homeTabFragment.isAdded()) getChildFragmentManager().putFragment(outState, "homeTabFragment", homeTabFragment);
		if (searchFragment.isAdded()) getChildFragmentManager().putFragment(outState, "searchFragment", searchFragment);
		if (notificationsFragment.isAdded()) getChildFragmentManager().putFragment(outState, "notificationsFragment", notificationsFragment);
		if (profileFragment.isAdded()) getChildFragmentManager().putFragment(outState, "profileFragment", profileFragment);
	}

	public void updateNotificationBadge() {
		AccountSession session = AccountSessionManager.getInstance().getAccount(accountID);
		Instance instance = AccountSessionManager.getInstance().getInstanceInfo(session.domain);
		if (instance == null) return;

		new GetNotifications(null, 1, EnumSet.allOf(Notification.Type.class), instance != null && instance.pleroma != null)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(List<Notification> notifications) {
						if (notifications.size() > 0) {
							try {
								long newestId = Long.parseLong(notifications.get(0).id);
								long lastSeenId = Long.parseLong(session.markers.notifications.lastReadId);
								System.out.println("NEWEST: " + newestId);
								System.out.println("LAST SEEN: " + lastSeenId);

								setNotificationBadge(newestId > lastSeenId);
							} catch (Exception ignored) {
								setNotificationBadge(false);
							}
						}
					}

					@Override
					public void onError(ErrorResponse error) {
						setNotificationBadge(false);
					}
				}).exec(accountID);
	}
	public void setNotificationBadge(boolean badge) {
		notificationTabIcon.setImageResource(badge
				? R.drawable.ic_fluent_alert_28_selector_badged
				: R.drawable.ic_fluent_alert_28_selector);
	}

	@Subscribe
	public void onNotificationReceived(NotificationReceivedEvent notificationReceivedEvent) {
		if (notificationReceivedEvent.account.equals(accountID)) setNotificationBadge(true);
	}

	@Subscribe
	public void onAllNotificationsSeen(AllNotificationsSeenEvent allNotificationsSeenEvent) {
		setNotificationBadge(false);
	}
}
