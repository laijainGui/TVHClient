package org.tvheadend.tvhclient.ui.programs;

import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.tasks.ImageDownloadTask;
import org.tvheadend.tvhclient.data.tasks.ImageDownloadTaskCallback;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.utils.UIUtils;
import org.tvheadend.tvhclient.utils.RecordingRemovedCallback;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO update icons (same color, record with profile must differ from regular record...)

public class ProgramDetailsFragment extends BaseFragment implements ImageDownloadTaskCallback, RecordingRemovedCallback {

    @Nullable
    @BindView(R.id.state)
    ImageView state;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.title_label)
    TextView titleLabel;
    @BindView(R.id.summary_label)
    TextView summaryLabel;
    @BindView(R.id.summary)
    TextView summary;
    @BindView(R.id.description_label)
    TextView descLabel;
    @BindView(R.id.description)
    TextView desc;
    @BindView(R.id.channel_label)
    TextView channelLabel;
    @BindView(R.id.channel)
    TextView channelName;
    @BindView(R.id.date)
    TextView date;
    @BindView(R.id.time)
    TextView time;
    @BindView(R.id.duration)
    TextView duration;
    @BindView(R.id.progress)
    TextView progress;
    @BindView(R.id.content_type_label)
    TextView contentTypeLabel;
    @BindView(R.id.content_type)
    TextView contentType;
    @BindView(R.id.series_info_label)
    TextView seriesInfoLabel;
    @BindView(R.id.series_info)
    TextView seriesInfo;
    @BindView(R.id.star_rating_label)
    TextView ratingBarLabel;
    @BindView(R.id.star_rating_text)
    TextView ratingBarText;
    @BindView(R.id.star_rating)
    RatingBar ratingBar;
    @Nullable
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;
    @Nullable
    @BindView(R.id.image)
    ImageView imageView;

    private int eventId;
    private Unbinder unbinder;
    private Program program;
    private Channel channel;
    private Recording recording;

    public static ProgramDetailsFragment newInstance(int eventId) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("eventId", eventId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_details_fragment, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.program_details_fragment_contents);
        stub.inflate();
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbarInterface.setTitle(getString(R.string.details));

        Bundle bundle = getArguments();
        if (bundle != null) {
            eventId = bundle.getInt("eventId", 0);
        }
        if (savedInstanceState != null) {
            eventId = savedInstanceState.getInt("eventId", 0);
        }

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.program_details_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        ProgramViewModel viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        viewModel.getProgram(eventId).observe(this, p -> {
            if (p != null) {
                program = p.getProgram();
                channel = (p.getChannels() != null && p.getChannels().size() > 0) ? p.getChannels().get(0) : null;
                recording = (p.getRecordings() != null && p.getRecordings().size() > 0) ? p.getRecordings().get(0) : null;
                updateUI();
                activity.invalidateOptionsMenu();
            }
        });
    }

    private void updateUI() {

        // Show the program information
        if (state != null) {
            Drawable drawable = UIUtils.getRecordingState(activity, program.getDvrId());
            state.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            state.setImageDrawable(drawable);
        }

        String timeStr = UIUtils.getTimeText(getContext(), program.getStart()) + " - " + UIUtils.getTimeText(getContext(), program.getStop());
        time.setText(timeStr);
        date.setText(UIUtils.getDate(getContext(), program.getStart()));

        String durationTime = getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
        duration.setText(durationTime);

        String progressText = UIUtils.getProgressText(getContext(), program.getStart(), program.getStop());
        progress.setVisibility(!TextUtils.isEmpty(progressText) ? View.VISIBLE : View.GONE);
        progress.setText(progressText);

        titleLabel.setVisibility(!TextUtils.isEmpty(program.getTitle()) ? View.VISIBLE : View.GONE);
        title.setVisibility(!TextUtils.isEmpty(program.getTitle()) ? View.VISIBLE : View.GONE);
        title.setText(program.getTitle());

        summaryLabel.setVisibility(!TextUtils.isEmpty(program.getSummary()) ? View.VISIBLE : View.GONE);
        summary.setVisibility(!TextUtils.isEmpty(program.getSummary()) ? View.VISIBLE : View.GONE);
        summary.setText(program.getSummary());

        descLabel.setVisibility(!TextUtils.isEmpty(program.getDescription()) ? View.VISIBLE : View.GONE);
        desc.setVisibility(!TextUtils.isEmpty(program.getDescription()) ? View.VISIBLE : View.GONE);
        desc.setText(program.getDescription());

        channelLabel.setVisibility(!TextUtils.isEmpty(channel.getChannelName()) ? View.VISIBLE : View.GONE);
        channelName.setVisibility(!TextUtils.isEmpty(channel.getChannelName()) ? View.VISIBLE : View.GONE);
        channelName.setText(channel.getChannelName());

        String seriesInfoText = UIUtils.getSeriesInfo(getContext(), program);
        if (TextUtils.isEmpty(seriesInfoText)) {
            seriesInfoLabel.setVisibility(View.GONE);
            seriesInfo.setVisibility(View.GONE);
        } else {
            seriesInfo.setText(seriesInfoText);
        }

        String ct = UIUtils.getContentTypeText(getContext(), program.getContentType());
        if (TextUtils.isEmpty(ct)) {
            contentTypeLabel.setVisibility(View.GONE);
            contentType.setVisibility(View.GONE);
        } else {
            contentType.setText(ct);
        }

        // Show the rating information as starts
        if (program.getStarRating() < 0) {
            ratingBarLabel.setVisibility(View.GONE);
            ratingBarText.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
        } else {
            ratingBar.setRating((float) program.getStarRating() / 10.0f);
            String value = " (" + program.getStarRating() + "/" + 10 + ")";
            ratingBarText.setText(value);
        }

        // Show the program image if one exists
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (isUnlocked && prefs.getBoolean("pref_show_program_artwork", false)) {
            ImageDownloadTask dt = new ImageDownloadTask(this);
            dt.execute(program.getImage(), String.valueOf(program.getEventId()));
        }
    }

    @Override
    public void notify(Drawable image) {
        if (imageView != null && image != null) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(image);

            // Get the dimensions of the image so the
            // width / height ratio can be determined
            final float w = image.getIntrinsicWidth();
            final float h = image.getIntrinsicHeight();

            if (h > 0) {
                // Scale the image view so it fits the width of the dialog or fragment root view
                final float scale = h / w;
                final float vw = imageView.getRootView().getWidth() - 128;
                final float vh = vw * scale;
                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) vw, (int) vh);
                layoutParams.gravity = Gravity.CENTER;
                imageView.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (program == null) {
            return;
        }
        if (nestedToolbar != null) {
            menu = nestedToolbar.getMenu();
        }

        // Show the play menu item when the current
        // time is between the program start and end time
        long currentTime = new Date().getTime();
        if (currentTime > program.getStart() && currentTime < program.getStop()) {
            menu.findItem(R.id.menu_play).setVisible(true);
        }

        if (recording == null || (!recording.isRecording()
                && !recording.isScheduled())) {
            menu.findItem(R.id.menu_record_once).setVisible(true);
            menu.findItem(R.id.menu_record_once_custom_profile).setVisible(isUnlocked);
            menu.findItem(R.id.menu_record_series).setVisible(serverStatus.getHtspVersion() >= 13);

        } else if (recording.isCompleted()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);
            menu.findItem(R.id.menu_play).setVisible(true);

        } else if (recording.isScheduled() && !recording.isRecording()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);

        } else if (recording.isRecording()) {
            menu.findItem(R.id.menu_record_stop).setVisible(true);

        } else if (recording.isFailed() || recording.isRemoved() || recording.isMissed() || recording.isAborted()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("eventId", eventId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (nestedToolbar == null) {
            inflater.inflate(R.menu.program_details_options_menu, menu);
        } else {
            inflater.inflate(R.menu.external_search_options_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;
            case R.id.menu_record_remove:
                if (recording != null) {
                    if (recording.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(recording.getId(), recording.getTitle(), this);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(recording.getId(), recording.getTitle(), this);
                    }
                }
                return true;
            case R.id.menu_record_stop:
                if (recording != null && recording.isRecording()) {
                    menuUtils.handleMenuStopRecordingSelection(recording.getId(), recording.getTitle());
                }
                return true;
            case R.id.menu_record_once:
                menuUtils.handleMenuRecordSelection(program.getEventId());
                return true;
            case R.id.menu_record_once_custom_profile:
                menuUtils.handleMenuCustomRecordSelection(program.getEventId(), program.getChannelId());
                return true;
            case R.id.menu_record_series:
                menuUtils.handleMenuSeriesRecordSelection(program.getTitle());
                return true;
            case R.id.menu_play:
                menuUtils.handleMenuPlaySelection(program.getChannelId(), -1);
                return true;
            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchWebSelection(program.getTitle());
                return true;
            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(program.getTitle(), program.getChannelId());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRecordingRemoved() {
        activity.finish();
    }
}

    