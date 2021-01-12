package org.crowdnotifier.android.sdk.model;

public class VenueInfo {
	private String name;
	private String location;
	private String room;
	private byte[] notificationKey;
	private Qr.QRCodeContent.VenueType venueType;
	private byte[] masterPublicKey;
	private byte[] nonce1;
	private byte[] nonce2;
	long validFrom;
	long validTo;

	public VenueInfo(String name, String location, String room, byte[] notificationKey, Qr.QRCodeContent.VenueType venueType,
			byte[] masterPublicKey, byte[] nonce1, byte[] nonce2, long validFrom, long validTo) {
		this.name = name;
		this.location = location;
		this.room = room;
		this.notificationKey = notificationKey;
		this.venueType = venueType;
		this.masterPublicKey = masterPublicKey;
		this.nonce1 = nonce1;
		this.nonce2 = nonce2;
		this.validFrom = validFrom;
		this.validTo = validTo;
	}


	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public byte[] getNotificationKey() {
		return notificationKey;
	}

	public String getRoom() {
		return room;
	}

	public Qr.QRCodeContent.VenueType getVenueType() {
		return venueType;
	}

	public String getTitle() {
		return name;
	}

	public String getSubtitle() {
		if (room == null || room.equals("")) {
			return location;
		} else {
			return location + ", " + room;
		}
	}

	public byte[] getMasterPublicKey() {
		return masterPublicKey;
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

}
