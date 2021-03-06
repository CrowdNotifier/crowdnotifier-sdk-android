package org.crowdnotifier.android.sdk.utils;

import android.net.Uri;

import com.google.protobuf.InvalidProtocolBufferException;

import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.crowdnotifier.android.sdk.model.CrowdNotifierData;
import org.crowdnotifier.android.sdk.model.QRCodePayload;
import org.crowdnotifier.android.sdk.model.TraceLocation;

/**
 * This class extracts the VenueInfo object from a provided QR Code URL in its only public function getQrInfo(...). It performs
 * several checks wheter the provided QR Code Url is valid. (It checks if the format is correct, if the version is correct and if
 * the current time is within the QR Code's validity time interval.
 */
public class QrUtils {

	public static final int QR_CODE_VERSION = 4;

	public static VenueInfo getQrInfo(String qrCodeString, String host) throws QRException {

		if(!qrCodeString.startsWith("http")){
			qrCodeString = "https://" + qrCodeString;
		}
		Uri uri = Uri.parse(qrCodeString);
		if (!host.equals(uri.getHost())) throw new InvalidQRCodeFormatException();
		String version = uri.getQueryParameter("v");
		if (!String.valueOf(QR_CODE_VERSION).equals(version)) throw new InvalidQRCodeVersionException();

		String fragment = uri.getFragment();
		if (fragment == null) throw new InvalidQRCodeFormatException();
		checkQrCodeValidity(fragment);
		return getVenueInfoFromQrCode(fragment);
	}

	private static void checkQrCodeValidity(String qrCodeString) throws QRException {
		try {
			byte[] decoded = Base64Util.fromBase64(qrCodeString);
			QRCodePayload qrCodeEntry = QRCodePayload.parseFrom(decoded);
			TraceLocation locationData = qrCodeEntry.getLocationData();

			if (System.currentTimeMillis() / 1000 < locationData.getStartTimestamp()) {
				throw new NotYetValidException();
			}
			if (System.currentTimeMillis() / 1000 > locationData.getEndTimestamp()) {
				throw new NotValidAnymoreException();
			}
		} catch (InvalidProtocolBufferException e) {
			throw new InvalidQRCodeFormatException();
		}
	}

	public static VenueInfo getVenueInfoFromQrCode(String qrCodeString) throws InvalidQRCodeFormatException {
		try {
			byte[] decoded = Base64Util.fromBase64(qrCodeString);
			QRCodePayload qrCodeEntry = QRCodePayload.parseFrom(decoded);
			TraceLocation locationData = qrCodeEntry.getLocationData();
			CrowdNotifierData crowdNotifierData = qrCodeEntry.getCrowdNotifierData();

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
