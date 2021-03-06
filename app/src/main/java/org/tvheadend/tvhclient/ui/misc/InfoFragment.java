package org.tvheadend.tvhclient.ui.misc;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.tasks.FileLoaderCallback;
import org.tvheadend.tvhclient.data.tasks.HtmlFileLoaderTask;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;

import java.util.regex.Pattern;

// TODO update used libraries in the html files

public class InfoFragment extends Fragment implements FileLoaderCallback {

    private WebView webView;
    private HtmlFileLoaderTask htmlFileLoaderTask;
    private ProgressBar loadingProgressBar;
    private AppCompatActivity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.webview_fragment, container, false);
        webView = v.findViewById(R.id.webview);
        loadingProgressBar = v.findViewById(R.id.loading);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setTitle(getString(R.string.pref_information));
            toolbarInterface.setSubtitle(null);
        }
        setHasOptionsMenu(true);
        htmlFileLoaderTask = new HtmlFileLoaderTask(activity, "info_help", "en", this);
        htmlFileLoaderTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        htmlFileLoaderTask.cancel(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.information_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;
            case R.id.menu_changelog:
                Intent intent = new Intent(activity, ChangeLogActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void notify(String content) {
        if (content != null) {
            // Replace the placeholder in the html file with the real version
            String version = BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_VERSION + ")";
            content = (Pattern.compile("APP_VERSION").matcher(content).replaceAll(version));

            webView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);
            loadingProgressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }
}
