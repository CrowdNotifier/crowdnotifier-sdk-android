package org.crowdnotifier.android.sdk.utils;

import android.util.Base64;

import com.google.protobuf.InvalidProtocolBufferException;

import org.crowdnotifier.android.sdk.model.Qr;
import org.crowdnotifier.android.sdk.model.VenueInfo;

import static android.util.Base64.NO_PADDING;

public class QrUtils {

	private static final String QR_CODE_VERSION = "2";

	public static VenueInfo getQrInfo(String qrCodeString, String expectedQrCodePrefix) throws QRException {

		String[] fragmentSplit = qrCodeString.split("#");
		if (fragmentSplit.length != 2) throw new InvalidQRCodeFormatException();
		String[] urlSplits = fragmentSplit[0].split("\\?v=");
		if (urlSplits.length != 2) throw new InvalidQRCodeFormatException();
		String urlPrefix = urlSplits[0];
		String version = urlSplits[1];
		if (!QR_CODE_VERSION.equals(version)) throw new InvalidQRCodeVersionException();
		if (!urlPrefix.equals(expectedQrCodePrefix)) throw new InvalidQRCodeFormatException();

		try {
			int decodeFlags = Base64.NO_WRAP | Base64.URL_SAFE | NO_PADDING;
			byte[] decoded = Base64.decode(fragmentSplit[1], decodeFlags);
			Qr.QRCodeEntry qrCodeEntry = Qr.QRCodeEntry.parseFrom(decoded);
			Qr.QRCodeContent qrCode = qrCodeEntry.getData();

			if (System.currentTimeMillis() < qrCode.getValidFrom()) {
				throw new NotYetValidException();
			}
			if (System.currentTimeMillis() > qrCode.getValidTo()) {
				throw new NotValidAnymoreException();
			}

			return new VenueInfo(qrCode.getName(), qrCode.getLocation(), qrCode.getRoom(),
					qrCode.getNotificationKey().toByteArray(), qrCode.getVenueType(),
					qrCodeEntry.getMasterPublicKey().toByteArray(), qrCodeEntry.getEntryProof());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			throw new InvalidQRCodeFormatException();
		}
	}

	public static class QRException extends Exception { }


	public static class NotYetValidException extends QRException { }


	public static class NotValidAnymoreException extends QRException { }


	public static class InvalidQRCodeFormatException extends QRException { }


	public static class InvalidQRCodeVersionException extends QRException { }

}
