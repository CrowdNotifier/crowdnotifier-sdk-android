package org.crowdnotifier.android.sdk.model;

import org.crowdnotifier.android.sdk.utils.Base64Util;

import static org.crowdnotifier.android.sdk.utils.QrUtils.QR_CODE_VERSION;

public class VenueInfo {
	private String description;
	private String address;
	private byte[] notificationKey;
	private byte[] publicKey;
	private byte[] noncePreId;
	private byte[] nonceTimekey;
	private long validFrom;
	private long validTo;
	private byte[] qrCodePayload;
	private byte[] countryData;

	public VenueInfo(String description, String address, byte[] notificationKey, byte[] publicKey, byte[] noncePreId,
			byte[] nonceTimekey, long validFrom, long validTo, byte[] qrCodePayload, byte[] countryData) {
		this.description = description;
		this.address = address;
		this.notificationKey = notificationKey;
		this.publicKey = publicKey;
		this.noncePreId = noncePreId;
		this.nonceTimekey = nonceTimekey;
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.qrCodePayload = qrCodePayload;
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

	public byte[] getNoncePreId() {
		return noncePreId;
	}

	public byte[] getNonceTimekey() {
		return nonceTimekey;
	}

	public long getValidFrom() {
		return validFrom;
	}

	public long getValidTo() {
		return validTo;
	}

	public byte[] getQrCodePayload() {
		return qrCodePayload;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public byte[] getCountryData() {
		return countryData;
	}

	public String toQrCodeString(String prefix) {
		return prefix + "?v=" + QR_CODE_VERSION + "#" + Base64Util.toBase64(qrCodePayload);
	}

}
