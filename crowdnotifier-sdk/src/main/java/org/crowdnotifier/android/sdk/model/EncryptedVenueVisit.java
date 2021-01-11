package org.crowdnotifier.android.sdk.model;

public class EncryptedVenueVisit {
	private long id;
	private DayDate dayDate;
	EncryptedData encryptedData;

	public EncryptedVenueVisit(long id, DayDate dayDate, EncryptedData encryptedData) {
		this.id = id;
		this.dayDate = dayDate;
		this.encryptedData = encryptedData;
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

	public EncryptedData getEncryptedData() {
		return encryptedData;
	}

}





