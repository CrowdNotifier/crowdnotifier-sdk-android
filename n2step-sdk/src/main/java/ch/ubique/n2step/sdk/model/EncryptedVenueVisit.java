package ch.ubique.n2step.sdk.model;

public class EncryptedVenueVisit {
	private long id;
	private long dayTimestamp;
	private byte[] publicKey;
	private byte[] sharedKey;
	private byte[] encryptedArrivalAndNotificationKey;
	private byte[] encryptedCheckout;

	public EncryptedVenueVisit(long id, long dayTimestamp, byte[] publicKey, byte[] sharedKey,
			byte[] encryptedArrivalAndNotificationKey, byte[] encryptedCheckout) {
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

	public byte[] getPublicKey() {
		return publicKey;
	}

	public byte[] getSharedKey() {
		return sharedKey;
	}

	public byte[] getEncryptedArrivalAndNotificationKey() {
		return encryptedArrivalAndNotificationKey;
	}

	public byte[] getEncryptedCheckout() {
		return encryptedCheckout;
	}

	public void setEncryptedCheckout(byte[] encryptedCheckout) {
		this.encryptedCheckout = encryptedCheckout;
	}

}





