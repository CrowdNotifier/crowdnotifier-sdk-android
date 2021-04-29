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
import org.crowdnotifier.android.sdk.model.v3.ProtoV3;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.Base64Util;
import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.crowdnotifier.android.sdk.utils.QrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * This class implements tests to validate the cryptographic operations in the app. To do so, the test implements all cryptographic
 * computations that are normally done in the Backend.
 */
@RunWith(AndroidJUnit4.class)
public class MatchingV3Tests {

	private Context context;
	private SodiumAndroid sodium;
	private CryptoUtils cryptoUtils;
	KeyPair haKeyPair;
	private static final long ONE_HOUR_IN_SECONDS = 60 * 60;
	private static final long ONE_DAY_IN_SECONDS = 24 * ONE_HOUR_IN_SECONDS;
	private static final int QR_CODE_VERSION = 3;


	/**
	 * Make some initializations before running the tests. (Load native mcl library, initialize singleton objects and create
	 * a Health Authority Keypair, which is used to simulate the cryptographic actions of the Health Authority in the CrowdNotifier
	 * protocol.
	 */
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


	/**
	 * This tests a full cycle of the CrowdNotifier protocol, from setting up the location owner, adding a User-Check-In to the SDK,
	 * simulating the generation of a List of PreTrace Protobuf objects followed by the creation of the corresponding Trace
	 * Protobuf objects as well as the ProblematicEventInfo objects, which are then successfully matched against the Check-In made
	 * by the user, by calling the checkForMatches function of the SDK. After the successful match, the checkForMatches function
	 * returns an ExposureEvent object.
	 */
	@Test
	public void testMatching() throws QrUtils.QRException, InvalidProtocolBufferException {

		long currentTime = System.currentTimeMillis() / 1000;
		long arrivalTime = currentTime - ONE_HOUR_IN_SECONDS;
		long departureTime = currentTime + ONE_HOUR_IN_SECONDS;
		long exposureStart = currentTime - ONE_HOUR_IN_SECONDS;
		long exposureEnd = currentTime;
		String exposureMessage = "This is a message.";
		byte[] countryData = getRandomValue(200);

		// Sets up location owner, adds User-Check-In, generates PreTraces and Traces and returns a list of ProblematicEventInfos
		List<ProblematicEventInfo> publishedSKs =
				generateVisitAndExposure(arrivalTime, departureTime, exposureStart, exposureEnd, exposureMessage, countryData);

		//User matches Traces with VenueVisits stored in App
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(publishedSKs, context);

		//Final Checks
		assertEquals(1, exposureEvents.size());
		assertEquals(arrivalTime, exposureEvents.get(0).getStartTime());
		assertEquals(departureTime, exposureEvents.get(0).getEndTime());
		assertEquals(exposureMessage, exposureEvents.get(0).getMessage());
		assertArrayEquals(countryData, exposureEvents.get(0).getCountryData());
	}

	/**
	 * This test does exactly the same as the testMatching test, except it that the generated ProblematicEventInfo does not match
	 * with the saved User-Check-In due to a time mismatch.
	 */
	@Test
	public void testNoMatching() throws QrUtils.QRException, InvalidProtocolBufferException {

		long currentTime = System.currentTimeMillis() / 1000;
		long arrivalTime = currentTime - ONE_HOUR_IN_SECONDS;
		long departureTime = currentTime + ONE_HOUR_IN_SECONDS;
		long exposureStart = currentTime - 2 * ONE_HOUR_IN_SECONDS;
		long exposureEnd = currentTime - ONE_HOUR_IN_SECONDS - 1;
		String exposureMessage = "This is a message.";
		byte[] countryData = getRandomValue(200);

		// Sets up location owner, adds User-Check-In, generates PreTraces and Traces and returns a list of ProblematicEventInfos
		List<ProblematicEventInfo> publishedSKs =
				generateVisitAndExposure(arrivalTime, departureTime, exposureStart, exposureEnd, exposureMessage, countryData);

		//User matches Traces with VenueVisits stored in App
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(publishedSKs, context);

		//Final Check
		assertEquals(0, exposureEvents.size());
	}


	private List<ProblematicEventInfo> generateVisitAndExposure(long arrivalTime, long departureTime, long exposureStart,
			long exposureEnd, String message, byte[] countryData) throws QrUtils.QRException, InvalidProtocolBufferException {
		long currentTime = System.currentTimeMillis() / 1000;

		//Setup Location Owner
		byte[] cryptographicSeed = getRandomValue(32);

		long qrCodeValidFrom = currentTime - ONE_DAY_IN_SECONDS;
		long qrCodeValidTo = currentTime + ONE_DAY_IN_SECONDS;
		Location location = new Location(haKeyPair.publicKey, ProtoV3.VenueType.OTHER, "Name", "Location",
				"Room", cryptographicSeed, qrCodeValidFrom, qrCodeValidTo);

		ProtoV3.QRCodeTrace qrTrace = location.qrCodeTrace;
		ProtoV3.QRCodePayload qrEntry = location.qrCodePayload;

		//User checks in with App
		String urlPrefix = "https://test.com";
		VenueInfo venueInfo = CrowdNotifier.getVenueInfo(urlPrefix + "?v=" + QR_CODE_VERSION + "#"
				+ Base64Util.toBase64(qrEntry.toByteArray()), urlPrefix);

		CrowdNotifier.addCheckIn(arrivalTime, departureTime, venueInfo, context);

		//Venue Owner Creates PreTraces
		List<ProtoV3.PreTraceWithProof> preTraceWithProofList =
				createPreTrace(qrTrace, exposureStart, exposureEnd);

		//Health Authority generates Traces
		List<ProblematicEventInfo> publishedSKs = new ArrayList<>();
		for (ProtoV3.PreTraceWithProof preTraceWithProof : preTraceWithProofList) {
			ProtoV3.Trace trace = createTrace(preTraceWithProof, haKeyPair, message, countryData);

			publishedSKs.add(new ProblematicEventInfo(trace.getIdentity().toByteArray(),
					trace.getSecretKeyForIdentity().toByteArray(), trace.getStartTime(), trace.getEndTime(),
					trace.getEncryptedAssociatedData().toByteArray(), trace.getNonce().toByteArray()));
		}
		return publishedSKs;
	}


	private ProtoV3.Trace createTrace(ProtoV3.PreTraceWithProof preTraceWithProof, KeyPair haKeyPair, String message,
			byte[] countryData) throws InvalidProtocolBufferException {

		ProtoV3.PreTrace preTrace = preTraceWithProof.getPreTrace();
		ProtoV3.TraceProof proof = preTraceWithProof.getProof();

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

		ProtoV3.QRCodePayload qrCodePayload = ProtoV3.QRCodePayload.parseFrom(preTraceWithProof.getQrCodePayload());
		byte[] identity = cryptoUtils.generateIdentityV3(qrCodePayload, preTraceWithProof.getStartOfInterval());
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
		byte[] encryptedAssociatedData = encryptAssociatedData(preTraceWithProof.getPreTrace().getNotificationKey().toByteArray(),
				message, countryData, nonce);

		return ProtoV3.Trace.newBuilder()
				.setIdentity(preTrace.getIdentity())
				.setSecretKeyForIdentity(ByteString.copyFrom(secretKeyForIdentity.serialize()))
				.setStartTime(preTraceWithProof.getStartTime())
				.setEndTime(preTraceWithProof.getEndTime())
				.setNonce(ByteString.copyFrom(nonce))
				.setEncryptedAssociatedData(ByteString.copyFrom(encryptedAssociatedData))
				.build();
	}

	private List<ProtoV3.PreTraceWithProof> createPreTrace(ProtoV3.QRCodeTrace qrCodeTrace, long startTime, long endTime)
			throws InvalidProtocolBufferException {

		ProtoV3.QRCodePayload qrCodePayload = ProtoV3.QRCodePayload.parseFrom(qrCodeTrace.getQrCodePayload());

		G2 masterPublicKey = new G2();
		masterPublicKey.deserialize(qrCodePayload.getCrowdNotifierData().getPublicKey().toByteArray());

		Fr masterSecretKeyLocation = new Fr();
		masterSecretKeyLocation.deserialize(qrCodeTrace.getMasterSecretKeyLocation().toByteArray());

		CryptoUtils.NoncesAndNotificationKey cryptoData = cryptoUtils.getNoncesAndNotificationKey(qrCodePayload);

		ArrayList<ProtoV3.PreTraceWithProof> preTraceWithProofsList = new ArrayList<>();
		ArrayList<Integer> affectedHours = cryptoUtils.getAffectedHours(startTime, endTime);
		for (Integer hour : affectedHours) {

			long startOfInterval = hour * 3600;

			byte[] identity = cryptoUtils.generateIdentityV3(qrCodePayload, startOfInterval);

			G1 partialSecretKeyForIdentityOfLocation = keyDer(masterSecretKeyLocation, identity);

			ProtoV3.PreTrace preTrace = ProtoV3.PreTrace.newBuilder()
					.setIdentity(ByteString.copyFrom(identity))
					.setCipherTextHealthAuthority(qrCodeTrace.getCipherTextHealthAuthority())
					.setPartialSecretKeyForIdentityOfLocation(
							ByteString.copyFrom(partialSecretKeyForIdentityOfLocation.serialize()))
					.setNotificationKey(ByteString.copyFrom(cryptoData.notificationKey))
					.build();

			ProtoV3.TraceProof traceProof = ProtoV3.TraceProof.newBuilder()
					.setMasterPublicKey(qrCodePayload.getCrowdNotifierData().getPublicKey())
					.setNoncePreId(ByteString.copyFrom(cryptoData.noncePreId))
					.setNonceTimekey(ByteString.copyFrom(cryptoData.nonceTimekey))
					.build();

			ProtoV3.PreTraceWithProof preTraceWithProof = ProtoV3.PreTraceWithProof.newBuilder()
					.setPreTrace(preTrace)
					.setProof(traceProof)
					.setQrCodePayload(qrCodeTrace.getQrCodePayload())
					.setStartTime(startTime)
					.setEndTime(endTime)
					.setStartOfInterval(startOfInterval)
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

	private byte[] encryptAssociatedData(byte[] secretKey, String message, byte[] countryData, byte[] nonce) {
		ProtoV3.AssociatedData associatedData = ProtoV3.AssociatedData.newBuilder()
				.setMessage(message)
				.setCountryData(ByteString.copyFrom(countryData))
				.setVersion(QR_CODE_VERSION)
				.build();

		byte[] messageBytes = associatedData.toByteArray();
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


	/**
	 * This class simulates the setup of a Location owner. It generates the two QR Code Protobufs for the public and the private
	 * QR Codes (qrCodePayload and qrCodeTrace).
	 */
	private class Location {
		ProtoV3.QRCodePayload qrCodePayload;
		ProtoV3.QRCodeTrace qrCodeTrace;

		public Location(byte[] healthAuthorityPublicKey, ProtoV3.VenueType venueType, String name, String location,
				String room, byte[] cryptographicSeed, long validFrom, long validTo) {

			ProtoV3.TraceLocation traceLocation = ProtoV3.TraceLocation.newBuilder()
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

			ProtoV3.CrowdNotifierData crowdNotifierData = ProtoV3.CrowdNotifierData.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setCryptographicSeed(ByteString.copyFrom(cryptographicSeed))
					.setPublicKey(ByteString.copyFrom(masterPublicKey.serialize()))
					.build();

			ProtoV3.NotifyMeLocationData countryData = ProtoV3.NotifyMeLocationData.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setRoom(room)
					.setType(venueType)
					.build();

			this.qrCodePayload = ProtoV3.QRCodePayload.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setCrowdNotifierData(crowdNotifierData)
					.setLocationData(traceLocation)
					.setCountryData(countryData.toByteString())
					.build();

			byte[] cipherTextHealthAuthority = new byte[haKeyPair.privateKey.serialize().length + Box.SEALBYTES];
			sodium.crypto_box_seal(cipherTextHealthAuthority, haKeyPair.privateKey.serialize(),
					haKeyPair.privateKey.serialize().length, healthAuthorityPublicKey);

			this.qrCodeTrace = ProtoV3.QRCodeTrace.newBuilder()
					.setVersion(QR_CODE_VERSION)
					.setQrCodePayload(qrCodePayload.toByteString())
					.setMasterSecretKeyLocation(ByteString.copyFrom(locationKeyPair.privateKey.serialize()))
					.setCipherTextHealthAuthority(ByteString.copyFrom(cipherTextHealthAuthority))
					.build();
		}

	}

}