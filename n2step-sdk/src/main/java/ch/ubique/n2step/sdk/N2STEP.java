package ch.ubique.n2step.sdk;

import android.content.Context;

import java.util.List;

import ch.ubique.n2step.sdk.model.Exposure;
import ch.ubique.n2step.sdk.model.ProblematicEventInfo;
import ch.ubique.n2step.sdk.model.VenueInfo;
import ch.ubique.n2step.sdk.storage.VenueVisitStorage;

public class N2STEP {

	/**
	 * @param qrCodeData
	 * @return A {@link VenueInfo} if the input qrCodeData was a valid qrCode, null otherwise.
	 */
	public static VenueInfo getInfo(String qrCodeData) {
		//TODO parse qrCodeData and return VenueInfo if qrCodeData is of a valid format
		return new VenueInfo("Test-Venue", "Test-Location", 5000, "publicKey", "notificationKey");
	}

	/**
	 * @param venueInfo
	 * @return An ID
	 */
	public static long checkIn(VenueInfo venueInfo, Context context) {

		//TODO implement
		/*
		Compute a Diffie-Hellman key exchange:
		1. Get the venue public key from the QR Code -> pkV
		2. Pick a random private key r (mod p)
		3. Compute public key g^r
		4. Compute shared key pkV^r
		5. Store public key g^r, and shared key pkV^r together with Enc(pkV, arrival_time || notification_key)
		6. Return an ID which identifies this DB entry
		 */

		return VenueVisitStorage.getInstance(context).addCheckIn(1000, venueInfo.getPublicKey(), "TODO", "TODO", "TODO");
	}

	public static void changeDuration(String publicKey, long checkinId, long duration, Context context) {

		//TODO implement
		/*
		Compute a Diffie-Hellman key exchange:
		1. Get the venue public key from the QR Code -> pkV
		2. Pick a random private key r (mod p)
		3. Compute public key g^r
		4. Compute shared key pkV^r
		5. Replace entry of checkinId with Enc(pkV, departure_time)
		 */
		VenueVisitStorage.getInstance(context).changeCheckOut(checkinId, "TODO");
	}

	public static List<Exposure> checkForMatches(List<ProblematicEventInfo> problematicEventInfos) {
		//TODO implement
		/*
		1. Go through all check ins overlapping with the problematicEvent duration and check if our public key to the power of
		the published secret key is equals the shared key (pk^skV == sharedKey)
		2. If so, decrypt the check-in and check-out DB entry, use the notification_key to decrypt the message and create an
		Exposure
		object and add it to the return list.
		 */
		return null;
	}

	public static void cleanupOldData(int maxDaysToKeep) {
		//TODO implement
	}

}
