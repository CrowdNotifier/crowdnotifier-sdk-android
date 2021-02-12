package org.crowdnotifier.android.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.crowdnotifier.android.sdk.model.EncryptedVenueVisit;
import org.crowdnotifier.android.sdk.model.ExposureEvent;
import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.crowdnotifier.android.sdk.utils.QrUtils;

public class CrowdNotifier {

	/**
	 * @param qrCode
	 * @param expectedQrCodePrefix the part of the url before the # you would accept as valid QR code
	 * @return A {@link VenueInfo} if the input qrCode was a valid qrCode, null otherwise.
	 */
	public static VenueInfo getVenueInfo(String qrCode, String expectedQrCodePrefix) throws QrUtils.QRException {
		return QrUtils.getQrInfo(qrCode, expectedQrCodePrefix);
	}


	public static long addCheckIn(long arrivalTime, long departureTime, VenueInfo venueInfo, Context context) {

		CryptoUtils crypto = CryptoUtils.getInstance();
		EncryptedVenueVisit encryptedVenueVisit = crypto.getEncryptedVenueVisit(arrivalTime, departureTime, venueInfo);

		return VenueVisitStorage.getInstance(context).addEntry(encryptedVenueVisit);
	}

	public static boolean updateCheckIn(long id, long arrivalTime, long departureTime, VenueInfo venueInfo, Context context) {

		CryptoUtils crypto = CryptoUtils.getInstance();
		EncryptedVenueVisit encryptedVenueVisit = crypto.getEncryptedVenueVisit(arrivalTime, departureTime, venueInfo);
		encryptedVenueVisit.setId(id);
		return VenueVisitStorage.getInstance(context).updateEntry(encryptedVenueVisit);
	}

	public static List<ExposureEvent> checkForMatches(List<ProblematicEventInfo> publishedSKs, Context context) {

		ArrayList<ExposureEvent> newExposureEvents = new ArrayList<>();
		ExposureStorage exposureStorage = ExposureStorage.getInstance(context);

		for (ProblematicEventInfo problematicEventInfo : publishedSKs) {

			List<ExposureEvent> matches = CryptoUtils.getInstance()
					.searchAndDecryptMatches(problematicEventInfo, VenueVisitStorage.getInstance(context).getEntries());

			for (ExposureEvent match : matches) {
				if (match.getStartTime() <= problematicEventInfo.getEndTimestamp() &&
						match.getEndTime() >= problematicEventInfo.getStartTimestamp()) {
					boolean added = exposureStorage.addEntry(match);
					if (added) newExposureEvents.add(match);
				}
			}
		}
		return newExposureEvents;
	}

	public static List<ExposureEvent> getExposureEvents(Context context) {
		return ExposureStorage.getInstance(context).getEntries();
	}

	public static void cleanUpOldData(Context context, int maxDaysToKeep) {
		VenueVisitStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
		ExposureStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
	}

	public static void removeExposure(Context context, long exposureId) {
		ExposureStorage.getInstance(context).removeExposure(exposureId);
	}

}
