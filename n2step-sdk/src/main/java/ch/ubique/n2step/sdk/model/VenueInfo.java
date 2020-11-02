package ch.ubique.n2step.sdk.model;

public class VenueInfo {
	private String name;
	private String location;
	private long defaultDuration;
	private String publicKey;
	private String notificationKey;

	public VenueInfo(String name, String location, long defaultDuration, String publicKey, String notificationKey) {
		this.name = name;
		this.location = location;
		this.defaultDuration = defaultDuration;
		this.publicKey = publicKey;
		this.notificationKey = notificationKey;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public long getDefaultDuration() {
		return defaultDuration;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getNotificationKey() {
		return notificationKey;
	}

}
