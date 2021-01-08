package org.crowdnotifier.android.sdk.model;

public class VenueInfo {
	private String name;
	private String location;
	private String room;
	private byte[] notificationKey;
	private Qr.QRCodeContent.VenueType venueType;
	private byte[] masterPublicKey;
	private Qr.EntryProof entryProof;
	long validFrom;
	long validTo;

	public VenueInfo(String name, String location, String room, byte[] notificationKey, Qr.QRCodeContent.VenueType venueType,
			byte[] masterPublicKey, Qr.EntryProof entryProof, long validFrom, long validTo) {
		this.name = name;
		this.location = location;
		this.room = room;
		this.notificationKey = notificationKey;
		this.venueType = venueType;
		this.masterPublicKey = masterPublicKey;
		this.entryProof = entryProof;
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

	public Qr.EntryProof getEntryProof() {
		return entryProof;
	}

	public long getValidFrom() {
		return validFrom;
	}

	public long getValidTo() {
		return validTo;
	}

}
