package org.crowdnotifier.android.sdk.model;

import java.util.List;

public class EncryptedVenueVisit {
	private long id;
	private DayDate dayDate;
	List<IBECiphertext> ibeCiphertextEntries;

	public EncryptedVenueVisit(DayDate dayDate, List<IBECiphertext> ibeCiphertextEntries) {
		this.dayDate = dayDate;
		this.ibeCiphertextEntries = ibeCiphertextEntries;
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

	public List<IBECiphertext> getIbeCiphertextEntries() {
		return ibeCiphertextEntries;
	}

}





