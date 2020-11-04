package ch.ubique.n2step.sdk.utils;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

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

	public byte[] getRandomEphemeralSecretKey() {
		byte[] secretKey = new byte[32];
		Sodium.randombytes_buf(secretKey, 32);
		return secretKey;
	}

	public byte[] computeEphemeralPublicKey(byte[] secretKey) {
		byte[] publicKey = new byte[32];
		Sodium.crypto_scalarmult_curve25519_base(publicKey, secretKey);
		return publicKey;
	}

	public byte[] computeSharedKey(byte[] publicKey, byte[] ephemeralSecretKey) {
		byte[] sharedKey = new byte[32];
		Sodium.crypto_scalarmult_curve25519(sharedKey, ephemeralSecretKey, publicKey);
		return sharedKey;
	}

	public boolean isSignatureValid(byte[] signature, byte[] message, byte[] publicKey) {
		int verified = Sodium.crypto_sign_ed25519_verify_detached(signature, message, message.length, publicKey);
		return verified != -1;
	}


	public byte[] encryptMessage(String payload, byte[] publicKey) {
		byte[] payloadBytes = payload.getBytes();
		byte[] curvePublicKey = new byte[32];
		Sodium.crypto_sign_ed25519_pk_to_curve25519(curvePublicKey, publicKey);
		byte[] encryptedPayload = new byte[payloadBytes.length];
		Sodium.crypto_box_seal(encryptedPayload, payloadBytes, payloadBytes.length, curvePublicKey);
		return encryptedPayload;
	}

}
