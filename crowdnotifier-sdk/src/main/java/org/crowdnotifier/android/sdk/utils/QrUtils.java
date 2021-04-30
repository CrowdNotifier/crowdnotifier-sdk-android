package org.crowdnotifier.android.sdk.utils;

import com.google.protobuf.InvalidProtocolBufferException;

import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.crowdnotifier.android.sdk.model.v3.CrowdNotifierData;
import org.crowdnotifier.android.sdk.model.v3.QRCodePayload;
import org.crowdnotifier.android.sdk.model.v3.TraceLocation;

/**
 * This class extracts the VenueInfo object from a provided QR Code URL in its only public function getQrInfo(...). It performs
 * several checks wheter the provided QR Code Url is valid. (It checks if the format is correct, if the version is correct and if
 * the current time is within the QR Code's validity time interval.
 */
public class QrUtils {

	public static final int QR_CODE_VERSION_3 = 3;

	public static VenueInfo getQrInfo(String qrCodeString, String expectedQrCodePrefix) throws QRException {

		String[] fragmentSplit = qrCodeString.split("#");
		if (fragmentSplit.length != 2) throw new InvalidQRCodeFormatException();
		String[] urlSplits = fragmentSplit[0].split("\\?v=");
		if (urlSplits.length != 2) throw new InvalidQRCodeFormatException();
		String urlPrefix = urlSplits[0];
		String version = urlSplits[1];
		if (!urlPrefix.equals(expectedQrCodePrefix)) throw new InvalidQRCodeFormatException();

		if (String.valueOf(QR_CODE_VERSION_3).equals(version)) {
			return getVenueInfoFromQrCode(fragmentSplit[1]);
		} else {
			throw new InvalidQRCodeVersionException();
		}
	}

	private static VenueInfo getVenueInfoFromQrCode(String qrCodeString) throws QRException {
		try {
			byte[] decoded = Base64Util.fromBase64(qrCodeString);
			QRCodePayload qrCodeEntry = QRCodePayload.parseFrom(decoded);
			TraceLocation locationData = qrCodeEntry.getLocationData();
			CrowdNotifierData crowdNotifierData = qrCodeEntry.getCrowdNotifierData();

			if (System.currentTimeMillis() / 1000 < locationData.getStartTimestamp()) {
				throw new NotYetValidException();
			}
			if (System.currentTimeMillis() / 1000 > locationData.getEndTimestamp()) {
				throw new NotValidAnymoreException();
			}

			CryptoUtils.NoncesAndNotificationKey cryptoData = CryptoUtils.getInstance().getNoncesAndNotificationKey(qrCodeEntry);

			return new VenueInfo(locationData.getDescription(), locationData.getAddress(), cryptoData.notificationKey,
					crowdNotifierData.getPublicKey().toByteArray(), cryptoData.noncePreId, cryptoData.nonceTimekey,
					locationData.getStartTimestamp(), locationData.getEndTimestamp(), qrCodeEntry.toByteArray(),
					qrCodeEntry.getCountryData().toByteArray());
		} catch (InvalidProtocolBufferException e) {
			throw new InvalidQRCodeFormatException();
		}
	}

	public static class QRException extends Exception { }


	public static class NotYetValidException extends QRException { }


	public static class NotValidAnymoreException extends QRException { }


	public static class InvalidQRCodeFormatException extends QRException { }


	public static class InvalidQRCodeVersionException extends QRException { }

}
