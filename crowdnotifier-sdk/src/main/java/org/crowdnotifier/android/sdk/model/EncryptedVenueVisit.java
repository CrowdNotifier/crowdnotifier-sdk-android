package org.crowdnotifier.android.sdk.model;

import java.util.List;

public class EncryptedVenueVisit {
	private long id;
	List<IBECiphertext> ibeCiphertextEntries;

	public EncryptedVenueVisit(List<IBECiphertext> ibeCiphertextEntries) {
		this.ibeCiphertextEntries = ibeCiphertextEntries;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<IBECiphertext> getIbeCiphertextEntries() {
		return ibeCiphertextEntries;
	}

}





