package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.mastodon.requests.tags.GetFollowedHashtags;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class FollowedHashtagsFragment extends RecyclerFragment<Hashtag> implements ScrollableToTop {
    private String nextMaxID;
    private String accountId;

    public FollowedHashtagsFragment() {
        super(20);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args=getArguments();
        accountId=args.getString("account");
        setTitle(R.string.sk_hashtags_you_follow);
    }

    @Override
    protected void onShown(){
        super.onShown();
        if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
            loadData();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 0.5f, 56, 16));
    }

    @Override
    protected void doLoadData(int offset, int count){
        currentRequest=new GetFollowedHashtags(offset==0 ? null : nextMaxID, null, count, null)
                .setCallback(new SimpleCallback<>(this){
                    @Override
                    public void onSuccess(HeaderPaginationList<Hashtag> result){
                        if (getActivity() == null) return;
                        if(result.nextPageUri!=null)
                            nextMaxID=result.nextPageUri.getQueryParameter("max_id");
                        else
                            nextMaxID=null;
                        onDataLoaded(result, nextMaxID!=null);
                    }
                })
                .exec(accountId);
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        return new HashtagsAdapter();
    }

    @Override
    public void scrollToTop() {
        smoothScrollRecyclerViewToTop(list);
    }

    @Override
    public boolean isScrolledToTop() {
        return list.getChildAt(0).getTop() == 0;
    }

    private class HashtagsAdapter extends RecyclerView.Adapter<HashtagViewHolder>{
        @NonNull
        @Override
        public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            return new HashtagViewHolder();
        }

        @Override
        public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class HashtagViewHolder extends BindableViewHolder<Hashtag> implements UsableRecyclerView.Clickable{
        private final TextView title;

        public HashtagViewHolder(){
            super(getActivity(), R.layout.item_text, list);
            title=findViewById(R.id.title);
        }

        @Override
        public void onBind(Hashtag item) {
            title.setText(item.name);
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(itemView.getContext().getDrawable(R.drawable.ic_fluent_number_symbol_24_regular), null, null, null);
        }

        @Override
        public void onClick() {
            UiUtils.openHashtagTimeline(getActivity(), accountId, item.name, item.following);
        }
    }
}
