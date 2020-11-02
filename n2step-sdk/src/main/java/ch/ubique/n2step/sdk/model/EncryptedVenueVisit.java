package ch.ubique.n2step.sdk.model;

public class EncryptedVenueVisit {
	private long id;
	private long dayTimestamp;
	private String publicKey;
	private String sharedKey;
	private String encryptedArrivalAndNotificationKey;
	private String encryptedCheckout;

	public EncryptedVenueVisit(long id, long dayTimestamp, String publicKey, String sharedKey,
			String encryptedArrivalAndNotificationKey, String encryptedCheckout) {
		this.id = id;
		this.dayTimestamp = dayTimestamp;
		this.publicKey = publicKey;
		this.sharedKey = sharedKey;
		this.encryptedArrivalAndNotificationKey = encryptedArrivalAndNotificationKey;
		this.encryptedCheckout = encryptedCheckout;
	}

	public long getId() {
		return id;
	}

	public long getDayTimestamp() {
		return dayTimestamp;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getSharedKey() {
		return sharedKey;
	}

	public String getEncryptedArrivalAndNotificationKey() {
		return encryptedArrivalAndNotificationKey;
	}

	public String getEncryptedCheckout() {
		return encryptedCheckout;
	}

	public void setEncryptedCheckout(String encryptedCheckout) {
		this.encryptedCheckout = encryptedCheckout;
	}

}





