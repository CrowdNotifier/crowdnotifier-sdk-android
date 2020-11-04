package ch.ubique.n2step.sdk.model;

public class VenueInfo {
	private String name;
	private String location;
	private String room;
	private byte[] publicKey;
	private byte[] notificationKey;
	private Qr.QRCodeContent.VenueType venueType;

	public VenueInfo(String name, String location, String room, Qr.QRCodeContent.VenueType venueType, byte[] publicKey,
			byte[] notificationKey) {
		this.name = name;
		this.location = location;
		this.room = room;
		this.venueType = venueType;
		this.publicKey = publicKey;
		this.notificationKey = notificationKey;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}


	public byte[] getPublicKey() {
		return publicKey;
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

}
