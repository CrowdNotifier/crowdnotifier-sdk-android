package org.crowdnotifier.android.sdk;

import java.util.ArrayList;

public class TestVectors {
	public ArrayList<IdentityTest> identityTestVector;
	public ArrayList<HKDFTest> hkdfTestVector;


	public static class HKDFTest {
		public byte[] qrCodePayload;
		public byte[] nonce1;
		public byte[] nonce2;
		public byte[] notificationKey;

	}


	public static class IdentityTest {
		public byte[] qrCodePayload;
		public int startOfInterval;
		public byte[] identity;

	}

}
