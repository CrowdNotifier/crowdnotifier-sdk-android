package ch.ubique.n2step.sdk.model;

public class EncryptedVenueVisit {
	private long id;
	private DayDate dayDate;
	private byte[] ephemeralPublicKey;
	private byte[] tag;
	private byte[] encryptedPayload;

	public EncryptedVenueVisit(long id, DayDate dayDate, byte[] ephemeralPublicKey, byte[] tag,
			byte[] encryptedPayload) {
		this.id = id;
		this.dayDate = dayDate;
		this.ephemeralPublicKey = ephemeralPublicKey;
		this.tag = tag;
		this.encryptedPayload = encryptedPayload;
	}

	public long getId() {
		return id;
	}

	public DayDate getDayDate() {
		return dayDate;
	}

	public byte[] getEphemeralPublicKey() {
		return ephemeralPublicKey;
	}

	public byte[] getTag() {
		return tag;
	}

	public byte[] getEncryptedPayload() {
		return encryptedPayload;
	}

	public void setId(long id) {
		this.id = id;
	}

}





