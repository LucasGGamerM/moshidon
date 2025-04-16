package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.parceler.Parcel;

import java.net.IDN;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public abstract class Instance extends BaseModel{
	/**
	 * The title of the website.
	 */
	@RequiredField
	public String title;
	/**
	 * Admin-defined description of the Mastodon site.
	 */
	@RequiredField
	public String description;
	/**
	 * The version of Mastodon installed on the instance.
	 */
	@RequiredField
	public String version;
	/**
	 * Primary languages of the website and its staff.
	 */
//	@RequiredField
	public List<String> languages;


	public List<Rule> rules;
	public Configuration configuration;

	// non-standard field in some Mastodon forks
	public int maxTootChars;

	// MOSHIDON: this is for translation support detection.
	public V2 v2;

	// MOSHIDON: we got pleroma babyyyyyy
	public Pleroma pleroma;
	public PleromaPollLimits pollLimits;

	// MOSHIDON:
	/** like uri, but always without scheme and trailing slash */
	public transient String normalizedUri;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(rules==null)
			rules=Collections.emptyList();
	}

	public CatalogInstance toCatalogInstance(){
		CatalogInstance ci=new CatalogInstance();
		ci.domain=getDomain();
		ci.normalizedDomain=IDN.toUnicode(getDomain());
		ci.description=description.trim();
		if(languages!=null && !languages.isEmpty()){
			ci.language=languages.get(0);
			ci.languages=languages;
		}else{
			ci.languages=List.of();
			ci.language="unknown";
		}
		ci.proxiedThumbnail=getThumbnailURL();
//		if(stats!=null)
//			ci.totalUsers=stats.userCount;
		return ci;
	}

	public abstract String getDomain();
	public abstract Account getContactAccount();
	public abstract String getContactEmail();
	public abstract boolean areRegistrationsOpen();
	public abstract boolean isSignupReasonRequired();
	public abstract boolean areInvitesEnabled();
	public abstract String getThumbnailURL();
	public abstract int getVersion();
	public abstract long getApiVersion(String name);

	public long getApiVersion(){
		return getApiVersion("mastodon");
	}

	// MOSHIDON:
	public boolean isAkkoma() {
		return pleroma != null;
	}

	public boolean isPixelfed() {
		return version.contains("compatible; Pixelfed");
	}

	public enum Feature {
		BUBBLE_TIMELINE,
		MACHINE_TRANSLATION
	}

	public boolean hasFeature(Feature feature) {
		Optional<List<String>> pleromaFeatures = Optional.ofNullable(pleroma)
				.map(p -> p.metadata)
				.map(m -> m.features);

		return switch (feature) {
			case BUBBLE_TIMELINE -> pleromaFeatures
					.map(f -> f.contains("bubble_timeline"))
					.orElse(false);
			case MACHINE_TRANSLATION -> pleromaFeatures
					.map(f -> f.contains("akkoma:machine_translation"))
					.orElse(false);
		};
	}

	@Parcel
	public static class Rule{
		public String id;
		public String text;
		public String hint;
		public Map<String, Translation> translations;

		public transient CharSequence parsedText;
		public transient CharSequence parsedHint;
		public transient boolean hintExpanded;

		private Translation findTranslationForCurrentLocale(){
			if(translations==null || translations.isEmpty())
				return null;
			Locale locale=Locale.getDefault();
			Translation t=translations.get(locale.toLanguageTag());
			if(t!=null)
				return t;
			return translations.get(locale.getLanguage());
		}

		public String getTranslatedText(){
			Translation translation=findTranslationForCurrentLocale();
			return translation==null || translation.text==null ? text : translation.text;
		}

		public String getTranslatedHint(){
			Translation translation=findTranslationForCurrentLocale();
			return translation==null || translation.hint==null ? hint : translation.hint;
		}

		@Parcel
		public static class Translation{
			public String text;
			public String hint;
		}
	}

	@Parcel
	public static class Configuration{
		public StatusesConfiguration statuses;
		public MediaAttachmentsConfiguration mediaAttachments;
		public PollsConfiguration polls;
		public URLsConfiguration urls;
	}

	@Parcel
	public static class StatusesConfiguration{
		public int maxCharacters;
		public int maxMediaAttachments;
		public int charactersReservedPerUrl;
	}

	@Parcel
	public static class MediaAttachmentsConfiguration{
		public List<String> supportedMimeTypes;
		public int imageSizeLimit;
		public int imageMatrixLimit;
		public int videoSizeLimit;
		public int videoFrameRateLimit;
		public int videoMatrixLimit;
	}

	@Parcel
	public static class PollsConfiguration{
		public int maxOptions;
		public int maxCharactersPerOption;
		public int minExpiration;
		public int maxExpiration;
	}

	@Parcel
	public static class URLsConfiguration{
		public String streaming;
		public String status;
		public String about;
		public String privacyPolicy;
		public String termsOfService;
	}

	// MOSHIDON: we check for translation support, so this needs to be here
	@Parcel
	public static class V2 extends BaseModel {
		public V2.Configuration configuration;

		@Parcel
		public static class Configuration {
			public TranslationConfiguration translation;
		}

		@Parcel
		public static class TranslationConfiguration{
			public boolean enabled;
		}
	}

	// MOSHIDON: more pleroma :D
	@Parcel
	public static class Pleroma extends BaseModel {
		public Pleroma.Metadata metadata;

		@Parcel
		public static class Metadata {
			public List<String> features;
			public Pleroma.Metadata.FieldsLimits fieldsLimits;

			@Parcel
			public static class FieldsLimits {
				public long maxFields;
				public long maxRemoteFields;
				public long nameLength;
				public long valueLength;
			}
		}
	}

	@Parcel
	public static class PleromaPollLimits {
		public long maxExpiration;
		public long maxOptionChars;
		public long maxOptions;
		public long minExpiration;
	}
}
