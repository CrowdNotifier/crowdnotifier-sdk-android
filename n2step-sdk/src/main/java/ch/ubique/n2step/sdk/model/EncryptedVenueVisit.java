package ch.ubique.n2step.sdk.model;

public class EncryptedVenueVisit {
	private long id;
	private long dayTimestamp;
	private byte[] publicKey;
	private byte[] sharedKey;
	private byte[] encryptedPayload;

	public EncryptedVenueVisit(long id, long dayTimestamp, byte[] publicKey, byte[] sharedKey,
			byte[] encryptedPayload) {
		this.id = id;
		this.dayTimestamp = dayTimestamp;
		this.publicKey = publicKey;
		this.sharedKey = sharedKey;
		this.encryptedPayload = encryptedPayload;
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

	public byte[] getEncryptedPayload() {
		return encryptedPayload;
	}

}





