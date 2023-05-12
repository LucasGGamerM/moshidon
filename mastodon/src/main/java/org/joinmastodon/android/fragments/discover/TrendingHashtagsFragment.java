package org.joinmastodon.android.fragments.discover;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.trends.GetTrendingHashtags;
import org.joinmastodon.android.fragments.DomainDisplay;
import org.joinmastodon.android.fragments.IsOnTop;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.HashtagChartView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class TrendingHashtagsFragment extends BaseRecyclerFragment<Hashtag> implements ScrollableToTop, IsOnTop, DomainDisplay {
	private String accountID;
	private DiscoverInfoBannerHelper bannerHelper=new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.TRENDING_HASHTAGS);

	public TrendingHashtagsFragment(){
		super(10);
	}

	@Override
	public String getDomain() {
		return DomainDisplay.super.getDomain() + "/explore/tags";
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetTrendingHashtags(10)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Hashtag> result){
						if (getActivity() == null) return;
						onDataLoaded(result, false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return new HashtagsAdapter();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, .5f, 16, 16));
		bannerHelper.maybeAddBanner(contentWrap);
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	@Override
	public boolean isScrolledToTop() {
		return list.getChildAt(0).getTop() == 0;
	}

	@Override
	public boolean isOnTop() {
		return isRecyclerViewOnTop(list);
	}

	private class HashtagsAdapter extends RecyclerView.Adapter<HashtagViewHolder>{
		@NonNull
		@Override
		public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new HashtagViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position){
			holder.bind(data.get(position));
		}

		@Override
		public int getItemCount(){
			return data.size();
		}
	}

	private class HashtagViewHolder extends BindableViewHolder<Hashtag> implements UsableRecyclerView.Clickable{
		private final TextView title, subtitle;
		private final HashtagChartView chart;

		public HashtagViewHolder(){
			super(getActivity(), R.layout.item_trending_hashtag, list);
			title=findViewById(R.id.title);
			subtitle=findViewById(R.id.subtitle);
			chart=findViewById(R.id.chart);
		}

		@Override
		public void onBind(Hashtag item){
			title.setText('#'+item.name);
			int numPeople = 0;
			if(item.history != null){
				numPeople=item.history.get(0).accounts;
				if(item.history.size()>1)
					numPeople+=item.history.get(1).accounts;
				chart.setData(item.history);
			}
			subtitle.setText(getResources().getQuantityString(R.plurals.x_people_talking, numPeople, numPeople));
		}

		@Override
		public void onClick(){
			UiUtils.openHashtagTimeline(getActivity(), accountID, item.name, item.following);
		}
	}
}
