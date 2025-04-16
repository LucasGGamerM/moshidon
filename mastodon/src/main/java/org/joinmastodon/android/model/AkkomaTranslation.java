package org.joinmastodon.android.model;

public class AkkomaTranslation extends BaseModel{
	public String text;
	public String detectedLanguage;

	public Translation toTranslation() {
		Translation translation=new Translation();
		translation.content=text;
		translation.detectedSourceLanguage=detectedLanguage;
		translation.provider="Akkoma";
		return translation;
	}
}
