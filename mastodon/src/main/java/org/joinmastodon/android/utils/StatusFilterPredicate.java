package org.joinmastodon.android.utils;

import org.joinmastodon.android.api.mastodon.session.AccountSessionManager;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Status;

import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StatusFilterPredicate implements Predicate<Status>{
	private final List<Filter> filters;

	public StatusFilterPredicate(List<Filter> filters){
		this.filters=filters;
	}

	public StatusFilterPredicate(String accountID, Filter.FilterContext context){
		filters=AccountSessionManager.getInstance().getAccount(accountID).wordFilters.stream().filter(f->f.context.contains(context)).collect(Collectors.toList());
	}

	@Override
	public boolean test(Status status){
		if(status.filtered!=null){
			if (status.filtered.isEmpty()){
				return true;
			}
			boolean matches=status.filtered.stream()
					.map(filterResult->filterResult.filter)
					.filter(filter->filter.expiresAt==null||filter.expiresAt.isAfter(Instant.now()))
					.anyMatch(filter->filter.filterAction==Filter.FilterAction.HIDE);
			return !matches;
		}
		for(Filter filter:filters){
			if(filter.matches(status))
				return false;
		}
		return true;
	}

	public boolean testWithWarning(Status status) {
		if(status.filtered!=null){
			if (status.filtered.isEmpty()){
				return true;
			}
			boolean matches=status.filtered.stream()
					.map(filterResult->filterResult.filter)
					.filter(filter->filter.expiresAt==null||filter.expiresAt.isAfter(Instant.now()))
					.anyMatch(filter->filter.filterAction==Filter.FilterAction.WARN);
			return !matches;
		}
		for(Filter filter:filters){
			if(filter.matches(status))
				return false;
		}
		return true;
	}
}
