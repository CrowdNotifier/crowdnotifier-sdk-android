package ch.ubique.n2step.sdk.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ch.ubique.n2step.sdk.model.DayDate;
import ch.ubique.n2step.sdk.model.Exposure;

public class ExposureStorage {

	private static final String KEY_N2STEP_STORE = "KEY_N2STEP_EXPOSURE_STORE";
	private static final String KEY_EXPOSURES = "KEY_EXPOSURES";
	private static final Type EXPOSURE_LIST_TYPE = new TypeToken<ArrayList<Exposure>>() { }.getType();

	private static ExposureStorage instance;

	private SharedPreferences sharedPreferences;
	private Gson gson = new Gson();

	private ExposureStorage(Context context) {
		try {
			String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
			sharedPreferences = EncryptedSharedPreferences.create(KEY_N2STEP_STORE,
					KEY_ALIAS,
					context,
					EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
					EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
		} catch (GeneralSecurityException | IOException ex) {
			ex.printStackTrace();
		}
	}

	public static synchronized ExposureStorage getInstance(Context context) {
		if (instance == null) {
			instance = new ExposureStorage(context);
		}
		return instance;
	}

	public long addEntry(Exposure exposure) {
		List<Exposure> exposureList = getEntries();
		long newId = getMaxId(exposureList) + 1;
		exposure.setId(newId);
		exposureList.add(exposure);
		saveToPrefs(exposureList);
		return newId;
	}

	private long getMaxId(List<Exposure> exposureList) {
		long maxId = 0;
		for (Exposure exposure : exposureList) {
			if (exposure.getId() > maxId) {
				maxId = exposure.getId();
			}
		}
		return maxId;
	}

	public Exposure getExposureWithId(long id) {
		for (Exposure exposure : getEntries()) {
			if (exposure.getId() == id) {
				return exposure;
			}
		}
		return null;
	}

	public List<Exposure> getEntries() {
		return gson.fromJson(sharedPreferences.getString(KEY_EXPOSURES, "[]"), EXPOSURE_LIST_TYPE);
	}

	public void removeEntriesBefore(int maxDaysToKeep) {
		List<Exposure> exposureList = getEntries();
		DayDate lastDateToKeep = new DayDate().subtractDays(maxDaysToKeep);
		Iterator<Exposure> iterator = exposureList.iterator();
		while (iterator.hasNext()) {
			if (new DayDate(iterator.next().getEndTime()).isBefore(lastDateToKeep)) {
				iterator.remove();
			}
		}
		saveToPrefs(exposureList);
	}

	private void saveToPrefs(List<Exposure> exposureList) {
		sharedPreferences.edit().putString(KEY_EXPOSURES, gson.toJson(exposureList)).apply();
	}

	public void clear() {
		saveToPrefs(new ArrayList<>());
	}

}