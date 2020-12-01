package org.crowdnotifier.android.sdk.utils;

import android.util.Base64;

import com.google.protobuf.InvalidProtocolBufferException;

import org.crowdnotifier.android.sdk.model.Qr;
import org.crowdnotifier.android.sdk.model.VenueInfo;

import static android.util.Base64.NO_PADDING;

public class QrUtils {

	private static final String QR_CODE_VERSION = "1";

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
			Qr.QRCodeWrapper qrCodeWrapper = Qr.QRCodeWrapper.parseFrom(decoded);
			Qr.QRCodeContent qrCode = qrCodeWrapper.getContent();

			if (qrCode.hasValidFrom() && System.currentTimeMillis() < qrCode.getValidFrom()) {
				throw new NotYetValidException();
			}
			if (qrCode.hasValidTo() && System.currentTimeMillis() > qrCode.getValidTo()) {
				throw new NotValidAnymoreException();
			}

			return new VenueInfo(qrCode.getName(), qrCode.getLocation(), qrCode.getRoom(), qrCode.getVenueType(),
					qrCodeWrapper.getPublicKey().toByteArray(), qrCode.getNotificationKey().toByteArray(),
					qrCodeWrapper.getR1().toByteArray(), qrCode.getValidFrom(), qrCode.getValidTo());
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
