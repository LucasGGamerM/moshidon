package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.accounts.GetFollowRequests;
import org.joinmastodon.android.events.FollowRequestHandledEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.squareup.otto.Subscribe;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.V;

public class NotificationsFragment extends MastodonToolbarFragment implements ScrollableToTop, DomainDisplay{

	private TabLayout tabLayout;
	private ViewPager2 pager;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;

	private NotificationsListFragment allNotificationsFragment, mentionsFragment, postsFragment;

	private String accountID;

	@Override
	public String getDomain() {
		return DomainDisplay.super.getDomain() + "/notifications";
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
		E.register(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		E.unregister(this);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		setTitle(R.string.notifications);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.notifications, menu);
		menu.findItem(R.id.clear_notifications).setVisible(GlobalUserPreferences.enableDeleteNotifications);
		UiUtils.enableOptionsMenuIcons(getActivity(), menu, R.id.follow_requests);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.follow_requests) {
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), FollowRequestsListFragment.class, args);
			return true;
		} else if (item.getItemId() == R.id.clear_notifications) {
			UiUtils.confirmDeleteNotification(getActivity(), accountID, null, ()->{
				for (int i = 0; i < tabViews.length; i++) {
					getFragmentForPage(i).reload();
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		LinearLayout view=(LinearLayout) inflater.inflate(R.layout.fragment_notifications, container, false);

		tabLayout=view.findViewById(R.id.tabbar);
		pager=view.findViewById(R.id.pager);

		tabViews=new FrameLayout[3];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.notifications_all;
				case 1 -> R.id.notifications_mentions;
				case 2 -> R.id.notifications_posts;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			view.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		tabLayout.setTabTextSize(V.dp(16));
		tabLayout.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorTabInactive), UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));

		pager.setOffscreenPageLimit(4);
		pager.setUserInputEnabled(!GlobalUserPreferences.disableSwipe);
		pager.setAdapter(new DiscoverPagerAdapter());
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			@Override
			public void onPageSelected(int position){
				if(position==0)
					return;
				Fragment _page=getFragmentForPage(position);
				if(_page instanceof BaseRecyclerFragment<?> page){
					if(!page.loaded && !page.isDataLoading())
						page.loadData();
				}
			}
		});

		if(allNotificationsFragment==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("__is_tab", true);
			args.putBoolean("noAutoLoad", true);

			allNotificationsFragment=new NotificationsListFragment();
			allNotificationsFragment.setArguments(args);

			args=new Bundle(args);
			args.putBoolean("onlyMentions", true);
			args.putBoolean("noAutoLoad", true);
			mentionsFragment=new NotificationsListFragment();
			mentionsFragment.setArguments(args);

			args=new Bundle(args);
			args.putBoolean("onlyPosts", true);
			args.putBoolean("noAutoLoad", true);
			postsFragment=new NotificationsListFragment();
			postsFragment.setArguments(args);

			getChildFragmentManager().beginTransaction()
					.add(R.id.notifications_all, allNotificationsFragment)
					.add(R.id.notifications_mentions, mentionsFragment)
					.add(R.id.notifications_posts, postsFragment)
					.commit();
		}

		tabLayoutMediator=new TabLayoutMediator(tabLayout, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				tab.setText(switch(position){
					case 0 -> R.string.all_notifications;
					case 1 -> R.string.mentions;
					case 2 -> R.string.posts;
					default -> throw new IllegalStateException("Unexpected value: "+position);
				});
				tab.view.textView.setAllCaps(true);
			}
		});
		tabLayoutMediator.attach();

		return view;
	}

	public void refreshFollowRequestsBadge() {
		new GetFollowRequests(null, 1).setCallback(new Callback<>() {
			@Override
			public void onSuccess(HeaderPaginationList<Account> accounts) {
				getToolbar().getMenu().findItem(R.id.follow_requests).setVisible(!accounts.isEmpty());
			}

			@Override
			public void onError(ErrorResponse errorResponse) {}
		}).exec(accountID);
	}

	@Subscribe
	public void onFollowRequestHandled(FollowRequestHandledEvent ev) {
		refreshFollowRequestsBadge();
	}

	@Override
	public void scrollToTop(){
		if (getFragmentForPage(pager.getCurrentItem()).isScrolledToTop() && !GlobalUserPreferences.disableDoubleTapToSwipe) {
			int nextPage = (pager.getCurrentItem() + 1) % tabViews.length;
			pager.setCurrentItem(nextPage, true);
			return;
		}
		getFragmentForPage(pager.getCurrentItem()).scrollToTop();
	}

	@Override
	public boolean isScrolledToTop() {
		return getFragmentForPage(pager.getCurrentItem()).isScrolledToTop();
	}

	public void loadData(){
		refreshFollowRequestsBadge();
		if(allNotificationsFragment!=null && !allNotificationsFragment.loaded && !allNotificationsFragment.dataLoading)
			allNotificationsFragment.loadData();
	}

	@Override
	protected void updateToolbar(){
		super.updateToolbar();
		getToolbar().setOutlineProvider(null);
	}

	private NotificationsListFragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> allNotificationsFragment;
			case 1 -> mentionsFragment;
			case 2 -> postsFragment;
			default -> throw new IllegalStateException("Unexpected value: "+page);
		};
	}

	private class DiscoverPagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=tabViews[viewType];
			if (view.getParent() != null) ((ViewGroup)view.getParent()).removeView(view);
			view.setVisibility(View.VISIBLE);
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){}

		@Override
		public int getItemCount(){
			return 3;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}