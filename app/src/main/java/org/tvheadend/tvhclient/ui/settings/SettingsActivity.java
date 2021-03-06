package org.tvheadend.tvhclient.ui.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.misc.ChangeLogActivity;
import org.tvheadend.tvhclient.ui.misc.InfoActivity;
import org.tvheadend.tvhclient.ui.misc.UnlockerActivity;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.File;

public class SettingsActivity extends AppCompatActivity implements ToolbarInterface, FolderChooserDialog.FolderCallback {
    private String settingType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        MiscUtils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            settingType = getIntent().getStringExtra("setting_type");
            if (settingType == null) {
                // Show the default fragment Create the default settings
                SettingsFragment fragment = new SettingsFragment();
                fragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
            } else {
                Fragment fragment = null;
                Intent intent;
                switch (settingType) {
                    case "list_connections":
                        fragment = new SettingsListConnectionsFragment();
                        break;
                    case "user_interface":
                        fragment = new SettingsUserInterfaceFragment();
                        break;
                    case "notifications":
                        fragment = new SettingsNotificationFragment();
                        break;
                    case "profiles":
                        fragment = new SettingsProfilesFragment();
                        break;
                    case "casting":
                        fragment = new SettingsCastingFragment();
                        break;
                    case "transcoding":
                        fragment = new SettingsTranscodingFragment();
                        break;
                    case "advanced":
                        fragment = new SettingsAdvancedFragment();
                        break;
                    case "information":
                        intent = new Intent(this, InfoActivity.class);
                        startActivity(intent);
                        break;
                    case "unlocker":
                        intent = new Intent(this, UnlockerActivity.class);
                        startActivity(intent);
                        break;
                    case "changelog":
                        intent = new Intent(this, ChangeLogActivity.class);
                        startActivity(intent);
                        break;
                }
                if (fragment != null) {
                    fragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("setting_type", settingType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        // If a settings fragment is currently visible, let the fragment
        // handle the back press, otherwise the setting activity.
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
        } else {
            super.onBackPressed();
        }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        Fragment f = getFragmentManager().findFragmentById(R.id.main);
        if (f != null && f.isAdded() && f instanceof FolderChooserDialogCallback) {
            ((FolderChooserDialogCallback) f).onFolderSelected(folder);
        }
    }

    @Override
    public void onFolderChooserDismissed(@NonNull FolderChooserDialog dialog) {
        // NOP
    }
}
