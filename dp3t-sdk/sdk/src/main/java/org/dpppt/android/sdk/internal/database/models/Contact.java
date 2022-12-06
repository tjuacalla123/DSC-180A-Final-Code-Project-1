package org.dpppt.android.sdk.internal.database.models;

import org.dpppt.android.sdk.internal.crypto.EphId;

public class Contact {

	private int id;
	private long date;
	private EphId ephId;
	private int windowCount;
	private int associatedKnownCase;

	public Contact(int id, long date, EphId ephId, int windowCount, int associatedKnownCase) {
		this.id = id;
		this.date = date;
		this.ephId = ephId;
		this.windowCount = windowCount;
		this.associatedKnownCase = associatedKnownCase;
	}

	public EphId getEphId() {
		return ephId;
	}

	public int getId() {
		return id;
	}

}
