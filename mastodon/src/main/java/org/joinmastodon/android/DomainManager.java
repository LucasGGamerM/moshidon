package org.joinmastodon.android;

public class DomainManager {
	private static final String TAG="DomainManager";

	private static final DomainManager instance=new DomainManager();

	private String currentDomain = "";


	public static DomainManager getInstance(){
		return instance;
	}

	private DomainManager(){

	}

	public String getCurrentDomain() {
		return currentDomain;
	}

	public void setCurrentDomain(String domain) {
		this.currentDomain = domain;
	}
}
