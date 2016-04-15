package org.tvheadend.tvhclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.util.Base64;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

public class ExternalActionActivity extends Activity implements HTSListener, OnRequestPermissionsResultCallback {

    private final static String TAG = ExternalActionActivity.class.getSimpleName();

    private TVHClientApplication app;
    private DatabaseHelper dbh;
    private Connection conn;
    private DownloadManager dm;
    private int action;

    private Channel ch;
    private Recording rec;
    private String baseUrl;

    private String title = "";

    private ProgressBar castProgressBar;
    private TextView castProgressInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.external_action_layout);
        Utils.setLanguage(this);

        app = (TVHClientApplication) getApplication();
        dbh = DatabaseHelper.getInstance(this);
        conn = dbh.getSelectedConnection();
        action = getIntent().getIntExtra(Constants.BUNDLE_EXTERNAL_ACTION, Constants.EXTERNAL_ACTION_PLAY);

        // Check that a valid channel or recording was specified
        ch = app.getChannel(getIntent().getLongExtra(Constants.BUNDLE_CHANNEL_ID, 0));
        rec = app.getRecording(getIntent().getLongExtra(Constants.BUNDLE_RECORDING_ID, 0));

        // Get the title from either the channel or recording
        if (ch != null) {
            title = ch.name;
        } else if (rec != null) {
            title = rec.title;
        } else {
            app.log(TAG, "No channel or recording provided, exiting");
            return;
        }

        // If the cast menu button is connected then assume playing means casting
        if (VideoCastManager.getInstance().isConnected()) {
            action = Constants.EXTERNAL_ACTION_CAST;
            castProgressBar = (ProgressBar) findViewById(R.id.progress);
            castProgressInfo = (TextView) findViewById(R.id.progress_info);
        }
    }

    /**
     *
     */
    private void initAction() {

        // Create the url with the credentials and the host and  
        // port configuration. This one is fixed for all actions
        baseUrl = "http://" + conn.username + ":" + conn.password + "@" + conn.address + ":" + conn.streaming_port;

        switch (action) {
        case Constants.EXTERNAL_ACTION_PLAY:
            // Check if the recording exists in the download folder, if not
            // stream it from the server
            if (rec != null && app.isUnlocked()) {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(path, rec.title + ".mkv");
                app.log(TAG, "Downloaded recording can be played from '" + file.getAbsolutePath()  + "': " + file.exists());
                if (file.exists()) {
                    startPlayback(file.getAbsolutePath(), "video/x-matroska");
                    break;
                }
            }

            // No downloaded recording exists, so continue starting the service
            // to get the url that shall be played. This could either be a
            // channel or a recording.
            Intent intent = new Intent(ExternalActionActivity.this, HTSService.class);
            intent.setAction(Constants.ACTION_GET_TICKET);
            intent.putExtras(getIntent().getExtras());
            this.startService(intent);
            break;

        case Constants.EXTERNAL_ACTION_DOWNLOAD:
            if (rec != null) {
                if (isStoragePermissionGranted()) {
                    prepareDownload();
                }
            }
            break;

        case Constants.EXTERNAL_ACTION_CAST:
            if (ch != null) {
                app.log(TAG, "Starting to cast channel '" + ch.name + "'");
                // In case a channel shall be played the channel uuid is required.
                // It is not provided via the HTSP API, only via the webinterface
                // API. Load the channel UUIDs via server:host/api/epg/events/grid.
                if (ch.uuid != null && ch.uuid.length() > 0) {
                    app.log(TAG, "Channel uuid + " + ch.uuid + " exists");
                    startCasting();
                } else {
                    app.log(TAG, "Channel uuid missing, starting loading thread");
                    new ChannelUuidLoaderTask().execute(conn);
                }
            } else if (rec != null) {
                app.log(TAG, "Starting to cast recording '" + rec.title + "'");
                startCasting();
            }
            break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoCastManager.getInstance().incrementUiCounter();
        app.addListener(this);

        initAction();
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoCastManager.getInstance().decrementUiCounter();
        app.removeListener(this);
    }

    /**
     * Creates the request for the download of the defined recording via the
     * internal download manager.
     */
    private void prepareDownload() {

        String downloadUrl = "http://" + conn.address + ":" + conn.streaming_port + "/dvrfile/" + rec.id;
        String auth = "Basic " + Base64.encodeToString((conn.username + ":" + conn.password).getBytes(), Base64.NO_WRAP);

        try {
            Request request = new Request(Uri.parse(downloadUrl));
            request.addRequestHeader("Authorization", auth);
            request.setTitle(getString(R.string.download));
            request.setDescription(rec.title);
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, rec.title + ".mkv");

            app.log(TAG, "Starting download from url " + downloadUrl);
            startDownload(request);

        } catch (IllegalStateException e) {
            app.log(TAG, "External storage not available, " + e.getLocalizedMessage());
            showErrorDialog(getString(R.string.no_external_storage_available));
        }
    }

    /**
     * Puts the request with the required data in the download queue. When the
     * download has been started it checks after a while if the download has
     * actually started or if it has failed due to insufficient space. This is
     * required because the download manager does not throw this error via a
     * notification.
     *
     * @param request The given download request with all relevant data
     */
    private void startDownload(Request request) {

        dm = (DownloadManager) getSystemService(Service.DOWNLOAD_SERVICE);
        final long id = dm.enqueue(request);

        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor c = dm.query(query);
                while (c.moveToNext()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                    app.log(TAG, "Download " + id + " status is " + status + ", reason " + reason);

                    switch (status) {
                    case DownloadManager.STATUS_FAILED:
                        // Check the reason value if it is insufficient storage space
                        if (reason == 1006) {
                            app.log(TAG, "Download " + id + " failed due to insufficient storage space");
                            showErrorDialog(getString(R.string.download_error_insufficient_space, rec.title));
                        } else if (reason == 407) {
                            app.log(TAG, "Download " + id + " failed due to missing / wrong authentication");
                            showErrorDialog(getString(R.string.download_error_authentication_required, rec.title));
                        } else {
                            finish();
                        }
                        break;

                    case DownloadManager.STATUS_PAUSED:
                        app.log(TAG, "Download " + id + " paused!");
                        finish();
                        break;

                    case DownloadManager.STATUS_PENDING:
                        app.log(TAG, "Download " + id + " pending!");
                        finish();
                        break;

                    case DownloadManager.STATUS_RUNNING:
                        app.log(TAG, "Download " + id + " in progress!");
                        finish();
                        break;

                    case DownloadManager.STATUS_SUCCESSFUL:
                        app.log(TAG, "Download " + id + " complete!");
                        finish();
                        break;

                    default:
                        finish();
                        break;
                    }
                }
            }
        }, 1500);
    }

    /**
     * When the first ticket from the HTSService has been received the URL is
     * created together with the mime and profile data. This is then passed to
     * the startPlayback method which invokes the external media player.
     *
     * @param path   The path to the recording or channel that shall be played
     * @param ticket The ticket id that was given by the server
     */
    private void initPlayback(String path, String ticket) {

        // Set default values if no profile was specified
        Profile profile = dbh.getProfile(conn.playback_profile_id);
        if (profile == null) {
            profile = new Profile();
        }

        // Set the correct MIME type. For 'pass' we assume MPEG-TS
        String mime = "application/octet-stream";
        switch (profile.container) {
            case "mpegps":
                mime = "video/mp2p";
                break;
            case "mpegts":
                mime = "video/mp4";
                break;
            case "matroska":
                mime = "video/x-matroska";
                break;
            case "pass":
                mime = "video/mp2t";
                break;
            case "webm":
                mime = "video/webm";
                break;
        }

        // Create the URL for the external media player that is required to get
        // the stream from the server
        String playUrl = baseUrl + path + "?ticket=" + ticket;

        // If a profile was given, use it instead of the old values
        if (profile.enabled
                && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            playUrl += "&profile=" + profile.name;
        } else {
            playUrl += "&mux=" + profile.container;
            if (profile.transcode) {
                playUrl += "&transcode=1";
                playUrl += "&resolution=" + profile.resolution;
                playUrl += "&acodec=" + profile.audio_codec;
                playUrl += "&vcodec=" + profile.video_codec;
                playUrl += "&scodec=" + profile.subtitle_codec;
            }
        }
        startPlayback(playUrl, mime);
    }

    /**
     * Starts the external media player with the given url and mime information.
     *
     * @param url  The url that shall be played
     * @param mime The mime type that shall be used
     */
    private void startPlayback(String url, String mime) {
        app.log(TAG, "Starting to play from url " + url);

        final Intent playbackIntent = new Intent(Intent.ACTION_VIEW);
        playbackIntent.setDataAndType(Uri.parse(url), mime);

        // Pass on the name of the channel or the recording title to the external 
        // video players. VLC uses itemTitle and MX Player the title string.
        playbackIntent.putExtra("itemTitle", title);
        playbackIntent.putExtra("title", title);

        // Start playing the video now in the UI thread
        this.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    app.log(TAG, "Starting external player");
                    startActivity(playbackIntent);
                    finish();
                } catch (Throwable t) {
                    app.log(TAG, "Can't execute external media player");

                    // Show a confirmation dialog before deleting the recording
                    new MaterialDialog.Builder(ExternalActionActivity.this)
                        .title(R.string.no_media_player)
                        .content(R.string.show_play_store)
                        .positiveText(getString(android.R.string.yes))
                        .negativeText(getString(android.R.string.no))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                try {
                                    app.log(TAG, "Starting play store to download external players");
                                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                    installIntent.setData(Uri.parse("market://search?q=free%20video%20player&c=apps"));
                                    startActivity(installIntent);
                                } catch (Throwable t2) {
                                    app.log(TAG, "Could not start google play store");
                                } finally {
                                    finish();
                                }
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                finish();
                            }
                        })
                        .show();
                }
            }
        });
    }

    /**
     * Creates the url and other required media information for casting. This
     * information is then passed to the cast controller activity.
     */
    private void startCasting() {

        String iconUrl = "";
        String castUrl = "";
        String subtitle = "";
        long duration = 0;
        int streamType = MediaInfo.STREAM_TYPE_NONE;

        if (ch != null) {
            castUrl = baseUrl + "/stream/channel/" + ch.uuid;
            iconUrl = baseUrl + "/" + ch.icon;
            streamType = MediaInfo.STREAM_TYPE_LIVE;
        } else if (rec != null) {
            castUrl = baseUrl + "/dvrfile/" + rec.id;
            subtitle = (rec.subtitle.length() > 0 ? rec.subtitle : rec.summary);
            duration = rec.stop.getTime() - rec.start.getTime();
            iconUrl = baseUrl + "/" + (rec.channel != null ? rec.channel.icon : "");
            streamType = MediaInfo.STREAM_TYPE_BUFFERED;
        }

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon

        // Check if the correct profile was set, if not try to do this
        // TODO if a connection is changed then this could be null
        castUrl += "?profile=" + dbh.getProfile(conn.cast_profile_id).name;

        app.log(TAG, "Casting starts with the following information:");
        app.log(TAG, "Cast title is " + title);
        app.log(TAG, "Cast subtitle is " + subtitle);
        app.log(TAG, "Cast icon is " + iconUrl);
        app.log(TAG, "Cast url is " + castUrl);

        MediaInfo mediaInfo = new MediaInfo.Builder(castUrl)
            .setStreamType(streamType)
            .setContentType("video/webm")
            .setMetadata(movieMetadata)
            .setStreamDuration(duration)
            .build();

        VideoCastManager.getInstance().startVideoCastControllerActivity(this, mediaInfo, 0, true);
        finish();
    }

    /**
     * Called when an activity was closed and this one is active again
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_START_PLAYER) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_TICKET_ADD)) {
            HttpTicket t = (HttpTicket) obj;
            initPlayback(t.path, t.ticket);
        }
    }

    /**
     * Task that opens a connection to the defined URL, authenticates and
     * downloads the available EPG data. If successful the JSON formatted
     * data will be parsed for the available channel UUID.
     *
     * @author rsiebert
     *
     */
    class ChannelUuidLoaderTask extends AsyncTask<Connection, Void, String> {

        private final String SUB_TAG = TAG + ", ChannelUuidLoaderTask";

        protected void onPreExecute() {
            if (castProgressInfo != null) {
                castProgressInfo.setVisibility(View.VISIBLE);
            }
            if (castProgressBar != null) {
                castProgressBar.setVisibility(View.VISIBLE);
            }
        }

        protected String doInBackground(Connection... conns) {
            InputStream is = null;
            String result = "";
            try {
                // Show that data is being loaded
                if (castProgressInfo != null) runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        castProgressInfo.setText(R.string.loading_casting_data);
                    }
                });

                // Create the URL
                Connection c = conns[0];
                int channelCount = (app.getChannels() != null ? app.getChannels().size() : 1000);
                String url = "http://" + c.address + ":" + c.streaming_port + "/api/channel/grid?limit=" + channelCount;
                String auth = "Basic " + Base64.encodeToString((c.username + ":" + c.password).getBytes(), Base64.NO_WRAP);
                app.log(SUB_TAG, "Connecting to " + url);

                // Connect to the server and authenticate
                HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("Authorization", auth);
                conn.connect();

                // Read the received data from the server
                is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                is.close();
                result = sb.toString();

            } catch (Throwable tr) {
                app.log(SUB_TAG, "Error " + tr.getLocalizedMessage());
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // NOP
                }
            }
            return result;
        }

        protected void onPostExecute(String result) {

            // Check if any data was loaded, if not exit
            if (result.length() == 0) {
                app.log(SUB_TAG, "Error loading JSON data");
                showErrorDialog(getString(R.string.error_loading_json_data));
                return;
            } else {
                app.log(SUB_TAG, "JSON data size is " + result.length());
            }

            // Differentiates between a real parsing 
            // exception or that no channel uuid was found
            boolean parsingError = false;

            try {
                app.log(SUB_TAG, "Parsing JSON data");

                // Show that data is being parsed
                if (castProgressInfo != null) {
                    castProgressInfo.setText(R.string.parsing_casting_data);
                }

                // Get the JSON array node and loop through all 
                // entries to save the UUIDs for all channels.
                JSONObject jsonObj = new JSONObject(result);
                JSONArray epgData = jsonObj.getJSONArray("entries");
                for (int i = 0; i < epgData.length(); i++) {
                    JSONObject epgItem = epgData.getJSONObject(i);

                    // Get the uuid from the data
                    String uuid = "";
                    if (epgItem.has("uuid")) {
                        uuid = epgItem.getString("uuid");
                    }
                    // Get the name from the data. It will be 
                    // compared against the selected channel
                    String name = "";
                    if (epgItem.has("name")) {
                        name = epgItem.getString("name");
                    }

                    if (ch.name.equals(name)) {
                        ch.uuid = uuid;
                        app.log(SUB_TAG, "Channel '" + ch.name + "' found, uuid is " + ch.uuid);
                        break;
                    }
                }

            } catch (JSONException e) {
                app.log(SUB_TAG, "Error parsing JSON data, " + e.getLocalizedMessage());
                parsingError = true;

            } finally {
                // Either start casting with the found UUID or 
                // show a message that the UUID could not be found
                if (ch.uuid != null && ch.uuid.length() > 0) {
                    app.log(SUB_TAG, "Found uuid, starting to cast channel " + ch.name);
                    startCasting();
                } else {
                    if (parsingError) {
                        app.log(SUB_TAG, "Showing error because JSON data could not be parsed");
                        showErrorDialog(getString(R.string.error_parsing_json_data));
                    } else {
                        app.log(SUB_TAG, "Showing error because no channel uuid was found for " + ch.name);
                        showErrorDialog(getString(R.string.error_no_channel_info_in_json_data));
                    }
                }
            }
        }
    }

    /**
     * Displays a dialog with the given message.
     *
     * @param msg The message that shall be shown
     */
    private void showErrorDialog(String msg) {
        // Hide the progress bar and text
        if (castProgressInfo != null) {
            castProgressInfo.setVisibility(View.GONE);
        }
        if (castProgressBar != null) {
            castProgressBar.setVisibility(View.GONE);
        }

        new MaterialDialog.Builder(this)
                .content(msg)
                .positiveText("Close")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .show();
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            app.log(TAG, "Permission: " + permissions[0] + " was " + grantResults[0]);
            prepareDownload();
        } else {
            finish();
        }
    }

    /**
     * Android API version 23 and higher requires that the permission to access
     * the external storage must be requested programmatically (if it has not
     * been granted already). The positive or negative request is checked in the
     * onRequestPermissionsResult(...) method.
     *
     * @return True if permission is granted, otherwise false
     */
    @SuppressLint("NewApi")
    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                app.log(TAG,"Permission is granted");
                return true;
            } else {
                app.log(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            app.log(TAG,"Permission is granted");
            return true;
        }
    }
}