package org.crowdnotifier.android.sdk;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import com.herumi.mcl.Mcl;

import org.crowdnotifier.android.sdk.model.*;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.Base64Util;
import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.crowdnotifier.android.sdk.utils.QrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * This class implements tests to validate the cryptographic operations in the app. To do so the test implements all cryptographic
 * computations that are normally done in the Backend.
 */
@RunWith(AndroidJUnit4.class)
public class MatchingTestV3 {

	private Context context;
	private SodiumAndroid sodium;
	private CryptoUtils cryptoUtils;
	KeyPair haKeyPair;
	private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L;
	private static final long ONE_DAY_IN_MILLIS = 24 * ONE_HOUR_IN_MILLIS;
	private static final int QR_CODE_VERSION = 3;


	@Before
	public void init() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		VenueVisitStorage.getInstance(context).clear();
		ExposureStorage.getInstance(context).clear();

		sodium = new LazySodiumAndroid(new SodiumAndroid()).getSodium();
		System.loadLibrary("mcljava");
		Mcl.SystemInit(Mcl.BLS12_381);
		cryptoUtils = CryptoUtils.getInstance();

		//Setup Health Authority
		haKeyPair = createHAKeyPair();
	}


	@Test
	public void testMatching() throws QrUtils.QRException, InvalidProtocolBufferException {

		long currentTime = System.currentTimeMillis();
		long arrivalTime = currentTime - ONE_HOUR_IN_MILLIS;
		long departureTime = currentTime + ONE_HOUR_IN_MILLIS;
		long exposureStart = currentTime - ONE_HOUR_IN_MILLIS;
		long exposureEnd = currentTime;
		String exposureMessage = "This is a message.";

		// Sets up location owner, adds User-Check-In, generates PreTraces and Traces and returns a list of ProblematicEventInfos
		List<ProblematicEventInfo> publishedSKs =
				generateVisitAndExposure(arrivalTime, departureTime, exposureStart, exposureEnd, exposureMessage);

		//User matches Traces with VenueVisits stored in App
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(publishedSKs, context);

		//Final Checks
		assertEquals(1, exposureEvents.size());
		assertEquals(arrivalTime, exposureEvents.get(0).getStartTime());
		assertEquals(departureTime, exposureEvents.get(0).getEndTime());
		assertEquals(exposureMessage, exposureEvents.get(0).getMessage());
	}

	@Test
	public void testNoMatching() throws QrUtils.QRException, InvalidProtocolBufferException {

		long currentTime = System.currentTimeMillis();
		long arrivalTime = currentTime - ONE_HOUR_IN_MILLIS;
		long departureTime = currentTime + ONE_HOUR_IN_MILLIS;
		long exposureStart = currentTime - 2 * ONE_HOUR_IN_MILLIS;
		long exposureEnd = currentTime - ONE_HOUR_IN_MILLIS - 1;
		String exposureMessage = "This is a message.";

		// Sets up location owner, adds User-Check-In, generates PreTraces and Traces and returns a list of ProblematicEventInfos
		List<ProblematicEventInfo> publishedSKs =
				generateVisitAndExposure(arrivalTime, departureTime, exposureStart, exposureEnd, exposureMessage);

		//User matches Traces with VenueVisits stored in App
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(publishedSKs, context);

		//Final Check
		assertEquals(0, exposureEvents.size());
	}


	private List<ProblematicEventInfo> generateVisitAndExposure(long arrivalTime, long departureTime, long exposureStart,
			long exposureEnd, String message) throws QrUtils.QRException, InvalidProtocolBufferException {
		long currentTime = System.currentTimeMillis();

		//Setup Location Owner
		byte[] cryptographicSeed = getRandomValue(32);

		long qrCodeValidFrom = currentTime - ONE_DAY_IN_MILLIS;
		long qrCodeValidTo = currentTime + ONE_DAY_IN_MILLIS;
		Location location = new Location(haKeyPair.publicKey, QrV3.VenueType.OTHER, "Name", "Location",
				"Room", cryptographicSeed, qrCodeValidFrom, qrCodeValidTo);

		BackendV3.QRCodeTrace qrTrace = location.qrCodeTrace;
		QrV3.QRCodePayload qrEntry = location.qrCodePayload;

		//User checks in with App
		String urlPrefix = "https://test.com";
		VenueInfo venueInfo = CrowdNotifier.getVenueInfo(urlPrefix + "?v=" + QR_CODE_VERSION + "#"
				+ Base64Util.toBase64(qrEntry.toByteArray()), urlPrefix);

		CrowdNotifier.addCheckIn(arrivalTime, departureTime, venueInfo, context);

		//Venue Owner Creates PreTraces
		List<BackendV3.PreTraceWithProof> preTraceWithProofList = createPreTrace(qrTrace, exposureStart, exposureEnd, message);

		//Health Authority generates Traces
		List<ProblematicEventInfo> publishedSKs = new ArrayList<>();
		for (BackendV3.PreTraceWithProof preTraceWithProof : preTraceWithProofList) {
			BackendV3.Trace trace = createTrace(preTraceWithProof, haKeyPair);

			publishedSKs.add(new ProblematicEventInfo(trace.getIdentity().toByteArray(),
					trace.getSecretKeyForIdentity().toByteArray(), trace.getStartTime(), trace.getEndTime(),
					trace.getEncryptedMessage().toByteArray(), trace.getNonce().toByteArray()));
		}
		return publishedSKs;
	}


	private BackendV3.Trace createTrace(BackendV3.PreTraceWithProof preTraceWithProof, KeyPair haKeyPair)
			throws InvalidProtocolBufferException {

		BackendV3.PreTrace preTrace = preTraceWithProof.getPreTrace();
		BackendV3.TraceProof proof = preTraceWithProof.getProof();

		byte[] ctxha = preTrace.getCipherTextHealthAuthority().toByteArray();
		byte[] mskh_raw = new byte[ctxha.length - Box.SEALBYTES];
		int result = sodium.crypto_box_seal_open(mskh_raw, ctxha, ctxha.length, haKeyPair.publicKey, haKeyPair.privateKey);
		if (result != 0) { throw new RuntimeException("crypto_box_seal_open returned a value != 0"); }

		Fr mskh = new Fr();
		mskh.deserialize(mskh_raw);

		G1 partialSecretKeyForIdentityOfHealthAuthority = keyDer(mskh, preTrace.getIdentity().toByteArray());
		G1 partialSecretKeyForIdentityOfLocation = new G1();
		partialSecretKeyForIdentityOfLocation.deserialize(preTrace.getPartialSecretKeyForIdentityOfLocation().toByteArray());

		G1 secretKeyForIdentity = new G1();
		Mcl.add(secretKeyForIdentity, partialSecretKeyForIdentityOfLocation, partialSecretKeyForIdentityOfHealthAuthority);

		QrV3.QRCodePayload qrCodePayload = QrV3.QRCodePayload.parseFrom(preTraceWithProof.getQrCodePayload());
		byte[] identity = cryptoUtils.generateIdentityV3(qrCodePayload, preTraceWithProof.getCounter());
		if (!Arrays.equals(preTrace.getIdentity().toByteArray(), identity)) {
			return null;
		}

		//verifyTrace
		int NONCE_LENGTH = 32;
		byte[] msg_orig = getRandomValue(NONCE_LENGTH);
		G2 masterPublicKey = new G2();
		masterPublicKey.deserialize(proof.getMasterPublicKey().toByteArray());
		IBECiphertext ibeCiphertext = cryptoUtils.encryptInternal(masterPublicKey, identity, msg_orig);
		byte[] msg_dec = cryptoUtils.decryptInternal(ibeCiphertext, secretKeyForIdentity, identity);
		if (msg_dec == null) throw new RuntimeException("Health Authority could not verify Trace");

		byte[] nonce = getRandomValue(Box.NONCEBYTES);
		byte[] encryptedMessage = encryptMessage(preTraceWithProof.getPreTrace().getNotificationKey().toByteArray(),
				preTraceWithProof.getPreTrace().getMessage(), nonce);

		return BackendV3.Trace.newBuilder()
				.setIdentity(preTrace.getIdentity())
				.setSecretKeyForIdentity(ByteString.copyFrom(secretKeyForIdentity.serialize()))
				.setStartTime(preTraceWithProof.getStartTime())
				.setEndTime(preTraceWithProof.getEndTime())
				.setNonce(ByteString.copyFrom(nonce))
				.setEncryptedMessage(ByteString.copyFrom(encryptedMessage))
				.build();
	}

	private List<BackendV3.PreTraceWithProof> createPreTrace(BackendV3.QRCodeTrace qrCodeTrace, long startTime, long endTime,
			String message) throws InvalidProtocolBufferException {

		QrV3.QRCodePayload qrCodePayload = QrV3.QRCodePayload.parseFrom(qrCodeTrace.getQrCodePayload());

		G2 masterPublicKey = new G2();
		masterPublicKey.deserialize(qrCodePayload.getCrowdNotifierData().getPublicKey().toByteArray());

		Fr masterSecretKeyLocation = new Fr();
		masterSecretKeyLocation.deserialize(qrCodeTrace.getMasterSecretKeyLocation().toByteArray());

		CryptoUtils.NoncesAndNotificationKey cryptoData = cryptoUtils.getNoncesAndNotificationKey(qrCodePayload);

		ArrayList<BackendV3.PreTraceWithProof> preTraceWithProofsList = new ArrayList<>();
		ArrayList<Integer> affectedHours = cryptoUtils.getAffectedHours(startTime, endTime);
		for (Integer hour : affectedHours) {

			byte[] identity = cryptoUtils.generateIdentityV3(qrCodePayload, hour);

			G1 partialSecretKeyForIdentityOfLocation = keyDer(masterSecretKeyLocation, identity);

			BackendV3.PreTrace preTrace = BackendV3.PreTrace.newBuilder()
					.setIdentity(ByteString.copyFrom(identity))
					.setCipherTextHealthAuthority(qrCodeTrace.getCipherTextHealthAuthority())
					.setPartialSecretKeyForIdentityOfLocation(
							ByteString.copyFrom(partialSecretKeyForIdentityOfLocation.serialize()))
					.setNotificationKey(ByteString.copyFrom(cryptoData.notificationKey))
					.setMessage(message)
					.build();

			BackendV3.TraceProof traceProof = BackendV3.TraceProof.newBuilder()
					.setMasterPublicKey(qrCodePayload.getCrowdNotifierData().getPublicKey())
					.setNonce1(ByteString.copyFrom(cryptoData.nonce1))
					.setNonce2(ByteString.copyFrom(cryptoData.nonce2))
					.build();

			BackendV3.PreTraceWithProof preTraceWithProof = BackendV3.PreTraceWithProof.newBuilder()
					.setPreTrace(preTrace)
					.setProof(traceProof)
					.setQrCodePayload(qrCodeTrace.getQrCodePayload())
					.setStartTime(startTime)
					.setEndTime(endTime)
					.setCounter(hour)
					.build();
			preTraceWithProofsList.add(preTraceWithProof);
		}
		return preTraceWithProofsList;
	}

	private G1 keyDer(Fr msk, byte[] identity) {
		G1 g1_temp = new G1();
		Mcl.hashAndMapToG1(g1_temp, identity);

		G1 result = new G1();
		Mcl.mul(result, g1_temp, msk);
		return result;
	}

	private KeyPair createHAKeyPair() {
		byte[] publicKey = new byte[Box.PUBLICKEYBYTES];
		byte[] privateKey = new byte[Box.SECRETKEYBYTES];
		int result = sodium.crypto_box_keypair(publicKey, privateKey);
		return new KeyPair(publicKey, privateKey);
	}

	private byte[] getRandomValue(int bytes) {
		byte[] nonce = new byte[bytes];
		sodium.randombytes_buf(nonce, nonce.length);
		return nonce;
	}

	private byte[] encryptMessage(byte[] secretKey, String message, byte[] nonce) {
		byte[] messageBytes = message.getBytes();
		byte[] encrytpedMessage = new byte[messageBytes.length + Box.MACBYTES];
		sodium.crypto_secretbox_easy(encrytpedMessage, messageBytes, messageBytes.length, nonce, secretKey);
		return encrytpedMessage;
	}

	private KeyPairMcl keyGen() {
		Fr privateKey = new Fr();
		privateKey.setByCSPRNG();

		G2 publicKey = new G2();
		Mcl.mul(publicKey, cryptoUtils.baseG2(), privateKey);
		return new KeyPairMcl(publicKey, privateKey);
	}

	private class KeyPairMcl {
		public G2 publicKey;
		public Fr privateKey;

		public KeyPairMcl(G2 publicKey, Fr privateKey) {
			this.publicKey = publicKey;
			this.privateKey = privateKey;
		}

	}


	private class KeyPair {
		public byte[] publicKey;
		public byte[] privateKey;

		public KeyPair(byte[] publicKey, byte[] privateKey) {
			this.publicKey = publicKey;
			this.privateKey = privateKey;
		}

	}


	private class Location {
		QrV3.QRCodePayload qrCodePayload;
		BackendV3.QRCodeTrace qrCodeTrace;

		public Location(byte[] healthAuthorityPublicKey, QrV3.VenueType venueType, String name, String location,
				String room, byte[] cryptographicSeed, long validFrom, long validTo) {

			QrV3.TraceLocation traceLocation = QrV3.TraceLocation.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setStartTimestamp(validFrom)
					.setEndTimestamp(validTo)
					.setDescription(name)
					.setAddress(location)
					.build();

			KeyPairMcl locationKeyPair = keyGen();
			KeyPairMcl haKeyPair = keyGen();
			G2 masterPublicKey = new G2();
			Mcl.add(masterPublicKey, locationKeyPair.publicKey, haKeyPair.publicKey);

			QrV3.CrowdNotifierData crowdNotifierData = QrV3.CrowdNotifierData.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setCryptographicSeed(ByteString.copyFrom(cryptographicSeed))
					.setPublicKey(ByteString.copyFrom(masterPublicKey.serialize()))
					.build();

			QrV3.NotifyMeLocationData countryData = QrV3.NotifyMeLocationData.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setRoom(room)
					.setType(venueType)
					.build();

			this.qrCodePayload = QrV3.QRCodePayload.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setCrowdNotifierData(crowdNotifierData)
					.setLocationData(traceLocation)
					.setCountryData(countryData.toByteString())
					.build();

			byte[] cipherTextHealthAuthority = new byte[haKeyPair.privateKey.serialize().length + Box.SEALBYTES];
			sodium.crypto_box_seal(cipherTextHealthAuthority, haKeyPair.privateKey.serialize(),
					haKeyPair.privateKey.serialize().length, healthAuthorityPublicKey);

			this.qrCodeTrace = BackendV3.QRCodeTrace.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setQrCodePayload(qrCodePayload.toByteString())
					.setMasterSecretKeyLocation(ByteString.copyFrom(locationKeyPair.privateKey.serialize()))
					.setCipherTextHealthAuthority(ByteString.copyFrom(cipherTextHealthAuthority))
					.build();
		}

	}

}