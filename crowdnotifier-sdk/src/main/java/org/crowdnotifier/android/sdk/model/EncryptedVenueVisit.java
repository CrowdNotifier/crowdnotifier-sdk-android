package org.crowdnotifier.android.sdk.model;

public class EncryptedVenueVisit {
	private long id;
	private DayDate dayDate;
	private byte[] c1;
	private byte[] c2;
	private byte[] c3;
	private byte[] nonce;

	public EncryptedVenueVisit(long id, DayDate dayDate, byte[] c1, byte[] c2, byte[] c3, byte[] nonce) {
		this.id = id;
		this.dayDate = dayDate;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		this.nonce = nonce;
	}

	public long getId() {
		return id;
	}

	public DayDate getDayDate() {
		return dayDate;
	}

	public void setId(long id) {
		this.id = id;
	}

	public byte[] getC1() {
		return c1;
	}

	public byte[] getC2() {
		return c2;
	}

	public byte[] getC3() {
		return c3;
	}

	public byte[] getNonce() {
		return nonce;
	}

}





