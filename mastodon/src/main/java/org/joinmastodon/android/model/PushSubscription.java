package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.AllFieldsAreRequired;

import androidx.annotation.NonNull;

@AllFieldsAreRequired
public class PushSubscription extends BaseModel implements Cloneable{
	public int id;
	public String endpoint;
	public Boolean standard=false;
	public Alerts alerts;
	public String serverKey;
	public Policy policy=Policy.ALL;

	public PushSubscription(){}

	@Override
	public String toString(){
		return "PushSubscription{"+
				"id="+id+
				", endpoint='"+endpoint+'\''+
				", standard="+standard+
				", alerts="+alerts+
				", serverKey='"+serverKey+'\''+
				", policy="+policy+
				'}';
	}

	@NonNull
	@Override
	public PushSubscription clone(){
		PushSubscription copy=(PushSubscription) super.clone();
		copy.alerts=alerts.clone();
		return copy;
	}

	public static class Alerts implements Cloneable{
		public boolean follow;
		public boolean favourite;
		public boolean reblog;
		public boolean mention;
		public boolean poll;
		public boolean status;
		public boolean update;

		// set to true here because i didn't add any items for those to the settings
		// (so i don't have to determine whether the user is an admin to show the items or not, and
		// admins can still disable those through the android notifications settings)
		@SerializedName("admin.sign_up")
		public boolean adminSignUp = true;
		@SerializedName("admin.report")
		public boolean adminReport = true;

		public static Alerts ofAll(){
			Alerts alerts=new Alerts();
			alerts.follow=alerts.favourite=alerts.reblog=alerts.mention=alerts.poll=alerts.status=alerts.update=true;
			return alerts;
		}

		@Override
		public String toString(){
			return "Alerts{"+
					"follow="+follow+
					", favourite="+favourite+
					", reblog="+reblog+
					", mention="+mention+
					", poll="+poll+
					", status="+status+
					", update="+update+
					", adminSignUp="+adminSignUp+
					", adminReport="+adminReport+
					'}';
		}

		@NonNull
		@Override
		public Alerts clone(){
			try{
				return (Alerts) super.clone();
			}catch(CloneNotSupportedException e){
				return null;
			}
		}
	}

	public enum Policy{
		@SerializedName("all")
		ALL,
		@SerializedName("followed")
		FOLLOWED,
		@SerializedName("follower")
		FOLLOWER,
		@SerializedName("none")
		NONE
	}
}
