package org.crowdnotifier.android.sdk.model;

public class EncryptedVenueVisit {
	private long id;
	private DayDate dayDate;
	IBECiphertext ibeCiphertext;

	public EncryptedVenueVisit(DayDate dayDate, IBECiphertext ibeCiphertext) {
		this.dayDate = dayDate;
		this.ibeCiphertext = ibeCiphertext;
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

	public IBECiphertext getIbeCiphertext() {
		return ibeCiphertext;
	}

}





