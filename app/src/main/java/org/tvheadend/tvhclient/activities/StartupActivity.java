package org.tvheadend.tvhclient.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.fragments.ChangeLogFragment;
import org.tvheadend.tvhclient.fragments.ConnectionStatusFragment;
import org.tvheadend.tvhclient.fragments.SyncStatusFragment;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.List;

public class StartupActivity extends AppCompatActivity implements ToolbarInterface {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);
        MiscUtils.setLanguage(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the toolbar so that the fragments can set the title
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (savedInstanceState == null) {
            if (isShowChangelogRequired()) {
                Log.d("X", "onCreate: changelog");
                // Show certain fragment depending on the current status.
                ChangeLogFragment fragment = new ChangeLogFragment();
                fragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();

            } else if (!isConnectionDefined()
                    || !isActiveConnectionDefined()
                    || !isNetworkAvailable()) {
                Log.d("X", "onCreate: connectionstatus");
                // Show the fragment with the connection info are defined
                Bundle bundle = new Bundle();
                if (!isConnectionDefined()) {
                    bundle.putString("type", "no_connections");
                } else if (!isActiveConnectionDefined()) {
                    bundle.putString("type", "no_active_connection");
                } else if (!isNetworkAvailable()) {
                    bundle.putString("type", "no_network");
                }
                ConnectionStatusFragment fragment = new ConnectionStatusFragment();
                fragment.setArguments(bundle);
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();

            } else {
                Log.d("X", "onCreate: syncstatus");
                // connect to the server and show the sync status if required
                SyncStatusFragment fragment = new SyncStatusFragment();
                fragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private boolean isConnectionDefined() {
        List<Connection> connectionList = DatabaseHelper.getInstance(getApplicationContext()).getConnections();
        return connectionList != null && connectionList.size() > 0;
    }

    private boolean isActiveConnectionDefined() {
        return DatabaseHelper.getInstance(getApplicationContext()).getSelectedConnection() != null;
    }

    private boolean isShowChangelogRequired() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String appVersionName = sharedPreferences.getString("app_version_name_for_changelog", "");
        return (!BuildConfig.VERSION_NAME.equals(appVersionName));
    }

    private void showContentScreen() {
        // Get the initial screen from the user preference.
        // This determines which screen shall be shown first
        int startScreen = Integer.parseInt(sharedPreferences.getString("defaultMenuPositionPref", "0"));
        Intent intent = new Intent(this, NavigationDrawerActivity.class);
        intent.putExtra("navigation_menu_position", startScreen);
        startActivity(intent);
        finish();
    }

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }
}
