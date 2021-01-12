package org.crowdnotifier.android.sdk.model;

public class IBECiphertext {
	private byte[] c1;
	private byte[] c2;
	private byte[] c3;
	private byte[] nonce;

	public IBECiphertext(byte[] c1, byte[] c2, byte[] c3, byte[] nonce) {
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		this.nonce = nonce;
	}

	public byte[] getC1() {
		return c1;
	}

	public byte[] getC2() {
		return c2;
	}

	public byte[] getC3() {
		return c3;
	}

	public byte[] getNonce() {
		return nonce;
	}

}
