package st.alr.homA;

import java.util.List;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;

public class SettingsActivity extends PreferenceActivity {
	private static Preference serverPreference;
	private static short mqttConnectivityState = App.MQTT_CONNECTIVITY_DISCONNECTED;
	private static SharedPreferences sharedPreferences;
	private BroadcastReceiver mqttConnectivityChangedReceiver;
	private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
	private static Context context;

	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mqttConnectivityChangedReceiver);
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesChangedListener);

		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mqttConnectivityState = ((App) getApplicationContext()).getState();

		getFragmentManager().beginTransaction().replace(android.R.id.content, new UserPreferencesFragment()).commit();

		preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
				String stringValue = sharedPreference.getString(key, "");

				if (key.equals("serverAddress")) {
					setServerPreferenceSummary(stringValue);
				} else {
					Log.v(toString(), "OnPreferenceChangeListener not implemented for key " + key);
				}

			}
		};

		sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);

		mqttConnectivityChangedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.e(toString(), "action is: " + intent.getAction());
				setServerPreferenceSummaryManually();
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(App.MQTT_CONNECTIVITY_CHANGED);
		registerReceiver(mqttConnectivityChangedReceiver, filter);

	}

	public static class UserPreferencesFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {

			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			serverPreference = findPreference("serverPreference");
			setServerPreferenceSummaryManually();

		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onIsMultiPane() {
		return false;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preferences_headers, target);
	}

	private static void setServerPreferenceSummaryManually() {
		setServerPreferenceSummary(sharedPreferences.getString("serverAddress", ""));
	}

	private static void setServerPreferenceSummary(String stringValue) {
		switch (getConnectivity()) {
			case App.MQTT_CONNECTIVITY_CONNECTING:
				serverPreference.setSummary("Connecting to " + stringValue);
				break;
			case App.MQTT_CONNECTIVITY_CONNECTED:
				serverPreference.setSummary("Connected to " + stringValue);
				break;
			case App.MQTT_CONNECTIVITY_DISCONNECTING:
				serverPreference.setSummary("Disconnecting from " + stringValue);
				break;
			case App.MQTT_CONNECTIVITY_DISCONNECTED:
				serverPreference.setSummary("Disconnected from " + stringValue);
				break;
			default:
				break;
		}
	}

	private static short getConnectivity() {
		return ((App) context.getApplicationContext()).getState();
	}
}