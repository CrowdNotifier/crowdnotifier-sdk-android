package ch.ubique.n2step.sdk_test_app.test;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ch.ubique.n2step.sdk.N2STEP;
import ch.ubique.n2step.sdk_test_app.R;

public class MainFragment extends Fragment {

	private MainViewModel mViewModel;

	public static MainFragment newInstance() {
		return new MainFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		N2STEP.cleanupOldData(14);
		return inflater.inflate(R.layout.main_fragment, container, false);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
		// TODO: Use the ViewModel
	}

}