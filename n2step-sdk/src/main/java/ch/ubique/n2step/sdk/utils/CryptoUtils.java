package ch.ubique.n2step.sdk.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import ch.ubique.n2step.sdk.model.EncryptedVenueVisit;
import ch.ubique.n2step.sdk.model.Payload;

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
		return verified != -1;
	}

	public EncryptedVenueVisit getEncryptedVenueVisit(long arrivalTime, long departureTime, byte[] notificationKey,
			byte[] venuePublicKey) {

		byte[] pk_venue_kx = new byte[Sodium.crypto_box_publickeybytes()];
		Sodium.crypto_sign_ed25519_pk_to_curve25519(pk_venue_kx, venuePublicKey);

		byte[] ephemeralSecretKey = new byte[Sodium.crypto_scalarmult_scalarbytes()];
		byte[] ephemeralPublicKey = new byte[Sodium.crypto_scalarmult_bytes()];
		Sodium.randombytes_buf(ephemeralSecretKey, ephemeralSecretKey.length);
		Sodium.crypto_scalarmult_base(ephemeralPublicKey, ephemeralSecretKey);

		byte[] tag = new byte[Sodium.crypto_scalarmult_bytes()];
		Sodium.crypto_scalarmult(tag, ephemeralSecretKey, pk_venue_kx);

		String payload = new Gson().toJson(new Payload(arrivalTime, departureTime, notificationKey));
		byte[] payloadBytes = payload.getBytes();
		byte[] encryptedPayload = new byte[payloadBytes.length + Sodium.crypto_box_sealbytes()];
		Sodium.crypto_box_seal(encryptedPayload, payloadBytes, payloadBytes.length, pk_venue_kx);

		return new EncryptedVenueVisit(0, arrivalTime, ephemeralPublicKey, tag, encryptedPayload);
	}

	public List<Payload> searchAndDecryptMatches(byte[] sk_venue_sgn, List<EncryptedVenueVisit> venueVisits) {

		List<Payload> result = new ArrayList<>();

		byte[] sk_venue_kx = new byte[Sodium.crypto_box_secretkeybytes()];
		Sodium.crypto_sign_ed25519_sk_to_curve25519(sk_venue_kx, sk_venue_sgn);

		for (EncryptedVenueVisit venueVisit : venueVisits) {

			byte[] tagprime = new byte[Sodium.crypto_scalarmult_bytes()];
			Sodium.crypto_scalarmult(tagprime, sk_venue_kx, venueVisit.getEphemeralPublicKey());

			if (Arrays.equals(tagprime, venueVisit.getTag())) {

				byte[] pk_venue_kx = new byte[Sodium.crypto_box_publickeybytes()];
				Sodium.crypto_scalarmult_curve25519_base(pk_venue_kx, sk_venue_kx);

				byte[] encryptedPayload = venueVisit.getEncryptedPayload();
				byte[] decriptedPayloadBytes = new byte[encryptedPayload.length - Sodium.crypto_box_sealbytes()];
				int r = Sodium.crypto_box_seal_open(decriptedPayloadBytes, encryptedPayload,
						encryptedPayload.length, pk_venue_kx, sk_venue_kx);

				Payload payload = new Gson().fromJson(new String(decriptedPayloadBytes), Payload.class);
				result.add(payload);
			}
		}

		return result;
	}

}
