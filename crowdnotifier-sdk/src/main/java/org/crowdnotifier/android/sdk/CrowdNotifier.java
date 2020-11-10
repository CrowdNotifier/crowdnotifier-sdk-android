package org.crowdnotifier.android.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.crowdnotifier.android.sdk.model.EncryptedVenueVisit;
import org.crowdnotifier.android.sdk.model.Exposure;
import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.crowdnotifier.android.sdk.utils.QrUtils;

public class CrowdNotifier {

	/**
	 * @param qrCodeData
	 * @param expectedQrCodePrefix the part of the url before the # you would accept as valid QR code
	 * @return A {@link VenueInfo} if the input qrCodeData was a valid qrCode, null otherwise.
	 */
	public static VenueInfo getInfo(String qrCodeData, String expectedQrCodePrefix) {
		return QrUtils.getQrInfo(qrCodeData, expectedQrCodePrefix);
	}


	public static long addVenueVisit(long arrivalTime, long departureTime, byte[] notificationKey, byte[] venuePublicKey,
			Context context) {

		CryptoUtils crypto = CryptoUtils.getInstance();
		EncryptedVenueVisit encryptedVenueVisit =
				crypto.getEncryptedVenueVisit(arrivalTime, departureTime, notificationKey, venuePublicKey);

		return VenueVisitStorage.getInstance(context).addEntry(encryptedVenueVisit);
	}

	public static List<Exposure> checkForMatches(List<ProblematicEventInfo> problematicEventInfos, Context context) {

		ArrayList<Exposure> newExposures = new ArrayList<>();
		ExposureStorage exposureStorage = ExposureStorage.getInstance(context);

		for (ProblematicEventInfo problematicEventInfo : problematicEventInfos) {

			List<Exposure> matches = CryptoUtils.getInstance().searchAndDecryptMatches(
					problematicEventInfo.getSecretKey(),
					VenueVisitStorage.getInstance(context).getEntries()
			);

			for (Exposure match : matches) {
				if (match.getStartTime() <= problematicEventInfo.getEndTimestamp() &&
						match.getEndTime() >= problematicEventInfo.getStartTimestamp()) {
					Exposure newExposure = new Exposure(match.getId(),
							Math.max(match.getStartTime(), problematicEventInfo.getStartTimestamp()),
							Math.min(match.getEndTime(), problematicEventInfo.getEndTimestamp()));
					boolean added = exposureStorage.addEntry(newExposure);
					if (added) newExposures.add(newExposure);
				}
			}
		}
		return newExposures;
	}

	public static List<Exposure> getExposures(Context context) {
		return ExposureStorage.getInstance(context).getEntries();
	}

	public static void cleanupOldData(Context context, int maxDaysToKeep) {
		VenueVisitStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
		ExposureStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
	}

}
