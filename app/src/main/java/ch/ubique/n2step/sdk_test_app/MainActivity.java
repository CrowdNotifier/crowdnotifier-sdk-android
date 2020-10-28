package ch.ubique.n2step.sdk_test_app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import ch.ubique.n2step.sdk_test_app.ui.main.MainFragment;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, MainFragment.newInstance())
					.commitNow();
		}
	}

}