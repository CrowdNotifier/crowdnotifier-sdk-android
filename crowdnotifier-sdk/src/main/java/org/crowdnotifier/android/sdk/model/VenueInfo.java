package org.crowdnotifier.android.sdk.model;

public class VenueInfo {
	private String description;
	private String address;
	private byte[] notificationKey;
	private byte[] publicKey;
	private byte[] nonce1;
	private byte[] nonce2;
	private long validFrom;
	private long validTo;
	private byte[] infoBytes; // if null -> old CrowdNotifier QR Code Version (2)
	private byte[] countryData;

	public VenueInfo(String description, String address, byte[] notificationKey, byte[] publicKey, byte[] nonce1,
			byte[] nonce2, long validFrom, long validTo, byte[] infoBytes, byte[] countryData) {
		this.description = description;
		this.address = address;
		this.notificationKey = notificationKey;
		this.publicKey = publicKey;
		this.nonce1 = nonce1;
		this.nonce2 = nonce2;
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.infoBytes = infoBytes;
		this.countryData = countryData;
	}


	public String getDescription() {
		return description;
	}

	public String getAddress() {
		return address;
	}

	public byte[] getNotificationKey() {
		return notificationKey;
	}

	public String getTitle() {
		return description;
	}

	public byte[] getNonce1() {
		return nonce1;
	}

	public byte[] getNonce2() {
		return nonce2;
	}

	public long getValidFrom() {
		return validFrom;
	}

	public long getValidTo() {
		return validTo;
	}

	public byte[] getInfoBytes() {
		return infoBytes;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public byte[] getCountryData() {
		return countryData;
	}

}
