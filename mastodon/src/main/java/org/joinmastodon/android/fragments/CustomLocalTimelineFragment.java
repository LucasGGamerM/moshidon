package org.joinmastodon.android.fragments;

import android.app.Activity;

import org.joinmastodon.android.api.mastodon.requests.timelines.GetPublicTimeline;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.utils.StatusFilterPredicate;

import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;

public class CustomLocalTimelineFragment extends StatusListFragment {
    //    private String name;
    private String domain;

    private String maxID;
    @Override
    protected boolean wantsComposeButton() {
        return false;
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        domain=getArguments().getString("domain");
        updateTitle(domain);

        setHasOptionsMenu(true);
    }

    private void updateTitle(String domain) {
        this.domain = domain;
        setTitle(this.domain);
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    protected void doLoadData(int offset, int count){
        currentRequest=new GetPublicTimeline(true, false, refreshing ? null : maxID, count)
                .setCallback(new SimpleCallback<>(this){
                    @Override
                    public void onSuccess(List<Status> result){
                        if(!result.isEmpty())
                            maxID=result.get(result.size()-1).id;
                        if (getActivity() == null) return;
                        result=result.stream().filter(new StatusFilterPredicate(accountID, Filter.FilterContext.PUBLIC)).collect(Collectors.toList());
                        result.stream().forEach(status -> {
                            status.account.acct += "@"+domain;
                            status.mentions.forEach(mention -> mention.id = null);
                            status.reloadWhenClicked = true;
                        });

                        onDataLoaded(result, !result.isEmpty());
                    }
                })
                .execNoAuth(domain);
    }

    @Override
    protected void onShown(){
        super.onShown();
        if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
            loadData();
    }
}
