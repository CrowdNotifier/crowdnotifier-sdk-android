package org.crowdnotifier.android.sdk;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.ByteString;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import com.herumi.mcl.Mcl;

import org.crowdnotifier.android.sdk.model.Qr;
import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.crowdnotifier.android.sdk.utils.QrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.crowdnotifier.android.sdk.model.ExposureEvent;
import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.Base64Util;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MatchingTest {

	private Context context;
	private SodiumAndroid sodium;
	private CryptoUtils cryptoUtils;
	private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;
	private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L;


	@Before
	public void init() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		VenueVisitStorage.getInstance(context).clear();
		ExposureStorage.getInstance(context).clear();

		sodium = new LazySodiumAndroid(new SodiumAndroid()).getSodium();
		System.loadLibrary("mcljava");
		Mcl.SystemInit(Mcl.BLS12_381);
		cryptoUtils = CryptoUtils.getInstance();
	}


	@Test
	public void testMatching() throws QrUtils.QRException {

		//Setup Health Authority
		KeyPair haKeyPair = createHAKeyPair();

		//Setup Location Owner
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		long qrCodeValidFrom = System.currentTimeMillis() - ONE_DAY_IN_MILLIS;
		long qrCodeValidTo = System.currentTimeMillis() + ONE_DAY_IN_MILLIS;
		Location location = new Location(haKeyPair.publicKey, Qr.QRCodeContent.VenueType.OTHER, "Name", "Location",
				"Room", notificationKey, qrCodeValidFrom, qrCodeValidTo);
		Qr.QRCodeTrace qrTrace = location.getQrCodeTrace();
		Qr.QRCodeEntry qrEntry = location.getQrCodeEntry();

		//User checks in with App
		String urlPrefix = "https://test.com";
		VenueInfo venueInfo =
				CrowdNotifier.getVenueInfo(urlPrefix + "?v=2#" + Base64Util.toBase64(qrEntry.toByteArray()), urlPrefix);

		CrowdNotifier.addCheckIn(System.currentTimeMillis() - ONE_HOUR_IN_MILLIS, System.currentTimeMillis() + ONE_HOUR_IN_MILLIS,
				venueInfo, context);

		//Venue Owner Creates PreTraces
		long exposureStart = System.currentTimeMillis() - ONE_HOUR_IN_MILLIS;
		long exposureEnd = System.currentTimeMillis() + ONE_HOUR_IN_MILLIS;
		List<Qr.PreTraceWithProof> preTraceWithProofList = createPreTrace(qrTrace, qrEntry, exposureStart, exposureEnd);

		//Health Authority generates Traces
		List<ProblematicEventInfo> publishedSKs = new ArrayList<>();
		for (Qr.PreTraceWithProof preTraceWithProof : preTraceWithProofList) {
			Qr.Trace trace = createTrace(preTraceWithProof, location.qrCodeContent, haKeyPair);

			String message = "This is a message";
			byte[] nonce = getRandomValue(Box.NONCEBYTES);
			byte[] encryptedMessage = encryptMessage(notificationKey, message, nonce);

			publishedSKs.add(new ProblematicEventInfo(trace.getIdentity().toByteArray(),
					trace.getSecretKeyForIdentity().toByteArray(), exposureStart, exposureEnd, encryptedMessage, nonce));
		}

		//User matches Traces with VenueVisits stored in App
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(publishedSKs, context);

		//Final Checks
		assertEquals(1, exposureEvents.size());
		assertEquals("This is a message", exposureEvents.get(0).getMessage());
	}

	private Qr.Trace createTrace(Qr.PreTraceWithProof preTraceWithProof, Qr.QRCodeContent qrCodeContent, KeyPair haKeyPair) {

		Qr.PreTrace preTrace = preTraceWithProof.getPreTrace();
		Qr.TraceProof proof = preTraceWithProof.getProof();

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

		byte[] identity = cryptoUtils.generateIdentity(qrCodeContent, proof.getNonce1().toByteArray(),
				proof.getNonce2().toByteArray(), preTraceWithProof.getCounter());
		if (!Arrays.equals(preTrace.getIdentity().toByteArray(), identity)) {
			return null;
		}

		//verifyTrace
		//TODO verify Trace

		//cryptoUtils.generateIdentity(preTraceWithProof.getPreTrace().)

		return Qr.Trace.newBuilder()
				.setIdentity(preTrace.getIdentity())
				.setSecretKeyForIdentity(ByteString.copyFrom(secretKeyForIdentity.serialize()))
				.build();
	}

	private List<Qr.PreTraceWithProof> createPreTrace(Qr.QRCodeTrace qrCodeTrace, Qr.QRCodeEntry qrCodeEntry, long startTime,
			long endTime) {

		Qr.MasterTrace masterTraceRecord = qrCodeTrace.getMasterTraceRecord();
		G2 masterPublicKey = new G2();
		masterPublicKey.deserialize(masterTraceRecord.getMasterPublicKey().toByteArray());

		Fr masterSecretKeyLocation = new Fr();
		masterSecretKeyLocation.deserialize(masterTraceRecord.getMasterSecretKeyLocation().toByteArray());

		ArrayList<Qr.PreTraceWithProof> preTraceWithProofsList = new ArrayList<>();
		ArrayList<Integer> affectedHours = cryptoUtils.getAffectedHours(startTime, endTime);
		for (Integer hour : affectedHours) {

			byte[] identity = cryptoUtils.generateIdentity(qrCodeEntry.getData(),
					qrCodeEntry.getEntryProof().getNonce1().toByteArray(),
					qrCodeEntry.getEntryProof().getNonce2().toByteArray(), hour);

			G1 partialSecretKeyForIdentityOfLocation = keyDer(masterSecretKeyLocation, identity);

			Qr.PreTrace preTrace = Qr.PreTrace.newBuilder()
					.setIdentity(ByteString.copyFrom(identity))
					.setCipherTextHealthAuthority(masterTraceRecord.getCipherTextHealthAuthority())
					.setPartialSecretKeyForIdentityOfLocation(
							ByteString.copyFrom(partialSecretKeyForIdentityOfLocation.serialize()))
					.build();

			Qr.TraceProof traceProof = Qr.TraceProof.newBuilder()
					.setMasterPublicKey(masterTraceRecord.getMasterPublicKey())
					.setNonce1(masterTraceRecord.getNonce1())
					.setNonce2(masterTraceRecord.getNonce2())
					.build();

			Qr.PreTraceWithProof preTraceWithProof = Qr.PreTraceWithProof.newBuilder()
					.setPreTrace(preTrace)
					.setProof(traceProof)
					.setInfo(masterTraceRecord.getInfo())
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


	/*
	@Test
	public void testMatchingOld() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		String message = "This is a message";
		byte[] nonce = getRandomValue(Box.NONCEBYTES);
		byte[] encryptedMessage = encryptMessage(notificationKey, message, nonce);
		byte[] r1 = getRandomValue(Box.SECRETKEYBYTES);
		byte[] r2 = getRandomValue(Box.SECRETKEYBYTES);
		String s = "a";
		byte[] infoBytes = getInfoBytes(s, s, s, notificationKey, Qr.QRCodeContent.VenueType.OTHER);
		byte[] venuePublicKey = new byte[Box.PUBLICKEYBYTES];
		byte[] venuePrivateKey = new byte[Box.SECRETKEYBYTES];
		int result = sodium.crypto_box_seed_keypair(venuePublicKey, venuePrivateKey, hashInfo(infoBytes, r1, r2));
		if (result != 0) { throw new RuntimeException("crypto_box_seed_keypair returned a value != 0"); }
		VenueInfo venueInfo =
				new VenueInfo(s, s, s, Qr.QRCodeContent.VenueType.OTHER, venuePublicKey, notificationKey, r1);

		CrowdNotifier.addCheckIn(time - 2 * 60 * 60 * 1000l, time, venueInfo, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents
				.add(new ProblematicEventInfo(venuePrivateKey, time - 1 * 60 * 60 * 1000l, time - 10, encryptedMessage,
						nonce, r2));

		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposureEvents.size());
		assertEquals(time - 10, exposureEvents.get(0).getEndTime());
		assertEquals(time - 1 * 60 * 60 * 1000l, exposureEvents.get(0).getStartTime());
		assertEquals(message, exposureEvents.get(0).getMessage());
	}

	 */

	private G2 baseG2() {
		G2 baseG2 = new G2();
		baseG2.setStr("1 3527010695874666181871391160110601448900299527927752" +
				"40219908644239793785735715026873347600343865175952761926303160 " +
				"305914434424421370997125981475378163698647032547664755865937320" +
				"6291635324768958432433509563104347017837885763365758 " +
				"198515060228729193556805452117717163830086897821565573085937866" +
				"5066344726373823718423869104263333984641494340347905 " +
				"927553665492332455747201965776037880757740193453592970025027978" +
				"793976877002675564980949289727957565575433344219582");
		return baseG2;
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
		Mcl.mul(publicKey, baseG2(), privateKey);
		return new KeyPairMcl(publicKey, privateKey);
	}

	private ILocationData genCode(byte[] healthAuthorityPublicKey, Qr.QRCodeContent qrCodeContent) {
		KeyPairMcl locationKeyPair = keyGen();
		KeyPairMcl haKeyPair = keyGen();

		G2 masterPublicKey = new G2();
		Mcl.add(masterPublicKey, locationKeyPair.publicKey, haKeyPair.publicKey);
		byte[] nonce1 = getRandomValue(Box.NONCEBYTES);
		byte[] nonce2 = getRandomValue(Box.NONCEBYTES);

		byte[] cipherTextHealthAuthority = new byte[haKeyPair.privateKey.serialize().length + Box.SEALBYTES];
		sodium.crypto_box_seal(cipherTextHealthAuthority, haKeyPair.privateKey.serialize(),
				haKeyPair.privateKey.serialize().length,
				healthAuthorityPublicKey);

		Qr.EntryProof entryProof = Qr.EntryProof.newBuilder()
				.setNonce1(ByteString.copyFrom(nonce1))
				.setNonce2(ByteString.copyFrom(nonce2))
				.build();

		IMasterTrace masterTrace = new IMasterTrace(masterPublicKey, locationKeyPair.privateKey, qrCodeContent, nonce1, nonce2,
				cipherTextHealthAuthority);
		return new ILocationData(masterPublicKey, entryProof, masterTrace);
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
		byte[] healthAuthorityPublicKey;
		Qr.QRCodeContent qrCodeContent;

		ILocationData iLocationData;

		public Location(byte[] healthAuthorityPublicKey, Qr.QRCodeContent.VenueType venueType, String name, String location,
				String room, byte[] notificationKey, long validFrom, long validTo) {
			this.healthAuthorityPublicKey = healthAuthorityPublicKey;
			this.qrCodeContent = Qr.QRCodeContent.newBuilder()
					.setVenueType(venueType)
					.setName(name)
					.setLocation(location)
					.setRoom(room)
					.setNotificationKey(ByteString.copyFrom(notificationKey))
					.setValidFrom(validFrom)
					.setValidTo(validTo)
					.build();
			this.iLocationData = genCode(healthAuthorityPublicKey, qrCodeContent);
		}

		public Qr.QRCodeEntry getQrCodeEntry() {
			return Qr.QRCodeEntry.newBuilder()
					.setData(qrCodeContent)
					.setEntryProof(iLocationData.entryProof)
					.setMasterPublicKey(ByteString.copyFrom(iLocationData.masterPublicKey.serialize()))
					.setVersion(2)
					.build();
		}


		public Qr.QRCodeTrace getQrCodeTrace() {

			return Qr.QRCodeTrace.newBuilder()
					.setVersion(2)
					.setMasterTraceRecord(iLocationData.masterTrace.toProtoMasterTrace())
					.build();
		}

	}


	private class ILocationData {
		G2 masterPublicKey;
		Qr.EntryProof entryProof;
		IMasterTrace masterTrace;

		public ILocationData(G2 masterPublicKey, Qr.EntryProof entryProof, IMasterTrace masterTrace) {
			this.masterPublicKey = masterPublicKey;
			this.entryProof = entryProof;
			this.masterTrace = masterTrace;
		}

	}


	private class IMasterTrace {
		G2 masterPublicKey;
		Fr masterSecretKeyLocation;
		Qr.QRCodeContent info;
		byte[] nonce1;
		byte[] nonce2;
		byte[] cipherTextHealthAuthority;

		public IMasterTrace(G2 masterPublicKey, Fr masterSecretKeyLocation, Qr.QRCodeContent info, byte[] nonce1, byte[] nonce2,
				byte[] cipherTextHealthAuthority) {
			this.masterPublicKey = masterPublicKey;
			this.masterSecretKeyLocation = masterSecretKeyLocation;
			this.info = info;
			this.nonce1 = nonce1;
			this.nonce2 = nonce2;
			this.cipherTextHealthAuthority = cipherTextHealthAuthority;
		}

		public Qr.MasterTrace toProtoMasterTrace() {
			return Qr.MasterTrace.newBuilder()
					.setMasterPublicKey(ByteString.copyFrom(masterPublicKey.serialize()))
					.setMasterSecretKeyLocation(ByteString.copyFrom(masterSecretKeyLocation.serialize()))
					.setInfo(info.toByteString())
					.setNonce1(ByteString.copyFrom(nonce1))
					.setNonce2(ByteString.copyFrom(nonce2))
					.setCipherTextHealthAuthority(ByteString.copyFrom(cipherTextHealthAuthority))
					.build();
		}

	}

}