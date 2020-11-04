package ch.ubique.n2step.sdk.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ch.ubique.n2step.sdk.model.EncryptedVenueVisit;

public class VenueVisitStorage {

	private static final String KEY_N2STEP_STORE = "KEY_N2STEP_STORE";
	private static final String KEY_VENUE_VISITS = "KEY_VENUE_VISITS";
	private static final Type VENUE_LIST_TYPE = new TypeToken<ArrayList<EncryptedVenueVisit>>() { }.getType();

	private static VenueVisitStorage instance;

	private SharedPreferences sharedPreferences;
	private ArrayList<EncryptedVenueVisit> venueVisitList;
	private Gson gson = new Gson();

	private VenueVisitStorage(Context context) {
		sharedPreferences = context.getSharedPreferences(KEY_N2STEP_STORE, Context.MODE_PRIVATE);
		venueVisitList = gson.fromJson(sharedPreferences.getString(KEY_VENUE_VISITS, "[]"), VENUE_LIST_TYPE);
	}

	public static synchronized VenueVisitStorage getInstance(Context context) {
		if (instance == null) {
			instance = new VenueVisitStorage(context);
		}
		return instance;
	}

	public long addCheckIn(long dayTimestamp, byte[] publicKey, byte[] sharedKey,
			byte[] encryptedArrivalAndNotificationKey, byte[] encryptedCheckout) {
		long newId = getMaxId() + 1;
		EncryptedVenueVisit newVenueVisit = new EncryptedVenueVisit(newId, dayTimestamp, publicKey, sharedKey,
				encryptedArrivalAndNotificationKey, encryptedCheckout);
		venueVisitList.add(newVenueVisit);
		saveToPrefs();
		return newId;
	}

	public void changeCheckOut(long id, byte[] encryptedCheckout) {
		EncryptedVenueVisit venueVisit = getVenueVisitWithId(id);
		if (venueVisit == null) {
			throw new IllegalArgumentException("There is no visit with id: " + id);
		}
		venueVisit.setEncryptedCheckout(encryptedCheckout);
		saveToPrefs();
	}

	public ArrayList<EncryptedVenueVisit> getVenueVisitList() {
		return venueVisitList;
	}

	private long getMaxId() {
		long maxId = 0;
		for (EncryptedVenueVisit venueVisit : venueVisitList) {
			if (venueVisit.getId() > maxId) {
				maxId = venueVisit.getId();
			}
		}
		return maxId;
	}

	private EncryptedVenueVisit getVenueVisitWithId(long id) {
		for (EncryptedVenueVisit venueVisit : venueVisitList) {
			if (venueVisit.getId() == id) {
				return venueVisit;
			}
		}
		return null;
	}

	private void saveToPrefs() {
		sharedPreferences.edit().putString(KEY_VENUE_VISITS, gson.toJson(venueVisitList)).apply();
	}

}
