package org.crowdnotifier.android.sdk.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import org.crowdnotifier.android.sdk.model.DayDate;
import org.crowdnotifier.android.sdk.model.EncryptedVenueVisit;
import org.crowdnotifier.android.sdk.model.ExposureEvent;
import org.crowdnotifier.android.sdk.model.Payload;

public class CryptoUtils {

	private static CryptoUtils instance;

	private CryptoUtils() {
		NaCl.sodium();
	}

	public static synchronized CryptoUtils getInstance() {
		if (instance == null) {
			instance = new CryptoUtils();
		}
		return instance;
	}

	public boolean isSignatureValid(byte[] signature, byte[] message, byte[] publicKey) {
		int verified = Sodium.crypto_sign_ed25519_verify_detached(signature, message, message.length, publicKey);
		return verified == 0;
	}

	public EncryptedVenueVisit getEncryptedVenueVisit(long arrivalTime, long departureTime, byte[] notificationKey,
			byte[] venuePublicKey) {

		byte[] pk_venue_kx = new byte[Sodium.crypto_box_publickeybytes()];
		int r = Sodium.crypto_sign_ed25519_pk_to_curve25519(pk_venue_kx, venuePublicKey);
		if (r != 0) {
			throw new RuntimeException("crypto_sign_ed25519_pk_to_curve25519 returned a value != 0");
		}

		byte[] ephemeralSecretKey = new byte[Sodium.crypto_scalarmult_scalarbytes()];
		byte[] ephemeralPublicKey = new byte[Sodium.crypto_scalarmult_bytes()];
		Sodium.randombytes_buf(ephemeralSecretKey, ephemeralSecretKey.length);
		r = Sodium.crypto_scalarmult_base(ephemeralPublicKey, ephemeralSecretKey);
		if (r != 0) {
			throw new RuntimeException("crypto_scalarmult_base returned a value != 0");
		}

		byte[] tag = new byte[Sodium.crypto_scalarmult_bytes()];
		r = Sodium.crypto_scalarmult(tag, ephemeralSecretKey, pk_venue_kx);
		if (r != 0) {
			throw new RuntimeException("crypto_scalarmult returned a value != 0");
		}

		String payload = new Gson().toJson(new Payload(arrivalTime, departureTime, notificationKey));
		byte[] payloadBytes = payload.getBytes();
		byte[] encryptedPayload = new byte[payloadBytes.length + Sodium.crypto_box_sealbytes()];
		r = Sodium.crypto_box_seal(encryptedPayload, payloadBytes, payloadBytes.length, pk_venue_kx);
		if (r != 0) {
			throw new RuntimeException("crypto_box_seal returned a value != 0");
		}

		return new EncryptedVenueVisit(0, new DayDate(departureTime), ephemeralPublicKey, tag, encryptedPayload);
	}

	public List<ExposureEvent> searchAndDecryptMatches(ProblematicEventInfo eventInfo, List<EncryptedVenueVisit> venueVisits) {

		byte[] sk_venue_sgn = eventInfo.getSecretKey();
		List<ExposureEvent> result = new ArrayList<>();

		byte[] sk_venue_kx = new byte[Sodium.crypto_box_secretkeybytes()];
		int r = Sodium.crypto_sign_ed25519_sk_to_curve25519(sk_venue_kx, sk_venue_sgn);
		if (r != 0) {
			throw new RuntimeException("crypto_sign_ed25519_sk_to_curve25519 returned a value != 0");
		}

		for (EncryptedVenueVisit venueVisit : venueVisits) {

			byte[] tagprime = new byte[Sodium.crypto_scalarmult_bytes()];
			r = Sodium.crypto_scalarmult(tagprime, sk_venue_kx, venueVisit.getEphemeralPublicKey());
			if (r != 0) {
				throw new RuntimeException("crypto_scalarmult returned a value != 0");
			}

			if (Arrays.equals(tagprime, venueVisit.getTag())) {

				byte[] pk_venue_kx = new byte[Sodium.crypto_box_publickeybytes()];
				r = Sodium.crypto_scalarmult_curve25519_base(pk_venue_kx, sk_venue_kx);
				if (r != 0) {
					throw new RuntimeException("crypto_scalarmult_curve25519_base returned a value != 0");
				}

				byte[] encryptedPayload = venueVisit.getEncryptedPayload();
				byte[] decryptedPayloadBytes = new byte[encryptedPayload.length - Sodium.crypto_box_sealbytes()];
				r = Sodium.crypto_box_seal_open(decryptedPayloadBytes, encryptedPayload,
						encryptedPayload.length, pk_venue_kx, sk_venue_kx);
				if (r != 0) {
					throw new RuntimeException("crypto_box_seal_open returned a value != 0");
				}

				Payload payload = new Gson().fromJson(new String(decryptedPayloadBytes), Payload.class);

				byte[] encryptedMessage = eventInfo.getEncryptedMessage();
				byte[] decryptedMessage = new byte[encryptedMessage.length - Sodium.crypto_box_macbytes()];
				r = Sodium.crypto_secretbox_open_easy(decryptedMessage, encryptedMessage, encryptedMessage.length,
						eventInfo.getNonce(), payload.getNotificationKey());
				if (r != 0) {
					throw new RuntimeException("crypto_secretbox_open_easy returned a value != 0");
				}

				String decryptedMessageString = new String(decryptedMessage);

				ExposureEvent exposureEvent = new ExposureEvent(venueVisit.getId(), payload.getArrivalTime(),
						payload.getDepartureTime(), decryptedMessageString);
				result.add(exposureEvent);
			}
		}

		return result;
	}

}
