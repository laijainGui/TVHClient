package org.tvheadend.tvhclient.ui.recordings.recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.service.EpgSyncService;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.recordings.base.BaseRecordingAddEditFragment;
import org.tvheadend.tvhclient.ui.recordings.common.DateTimePickerCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingProfileListCallback;
import org.tvheadend.tvhclient.utils.ChannelListSelectionCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnTextChanged;
import butterknife.Unbinder;

public class RecordingAddEditFragment extends BaseRecordingAddEditFragment implements BackPressedInterface, ChannelListSelectionCallback, DateTimePickerCallback, RecordingPriorityListCallback, RecordingProfileListCallback {
    private String TAG = getClass().getSimpleName();

    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.stop_time)
    TextView stopTimeTextView;
    @BindView(R.id.start_date)
    TextView startDateTextView;
    @BindView(R.id.stop_date)
    TextView stopDateTextView;
    @BindView(R.id.is_enabled)
    CheckBox isEnabledCheckbox;
    @BindView(R.id.start_extra)
    EditText startExtraEditText;
    @BindView(R.id.stop_extra)
    EditText stopExtraEditText;
    @BindView(R.id.title)
    EditText titleEditText;
    @BindView(R.id.title_label)
    TextView titleLabelTextView;
    @BindView(R.id.subtitle)
    EditText subtitleEditText;
    @BindView(R.id.subtitle_label)
    TextView subtitleLabelTextView;
    @BindView(R.id.description)
    EditText descriptionEditText;
    @BindView(R.id.description_label)
    TextView descriptionLabelTextView;
    @BindView(R.id.channel_label)
    TextView channelNameLabelTextView;
    @BindView(R.id.channel)
    TextView channelNameTextView;
    @BindView(R.id.dvr_config_label)
    TextView recordingProfileLabelTextView;
    @BindView(R.id.priority)
    TextView priorityTextView;
    @BindView(R.id.dvr_config)
    TextView recordingProfileNameTextView;

    private Unbinder unbinder;
    private int dvrId = 0;
    private Recording recording;
    private int recordingProfileNameId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_add_edit_fragment, container, false);
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

        // Get the selected profile from the connection and select it from the recording config list
        ServerProfile profile = profileRepository.getRecordingServerProfile();
        if (profile != null) {
            for (int i = 0; i < recordingProfilesList.length; i++) {
                if (recordingProfilesList[i].equals(profile.getName())) {
                    recordingProfileNameId = i;
                    break;
                }
            }
        }

        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                dvrId = bundle.getInt("dvrId");
            }
        } else {
            dvrId = savedInstanceState.getInt("id");
        }

        // Create the view model that stores the connection model
        RecordingViewModel viewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        recording = viewModel.getRecordingByIdSync(dvrId);
        updateUI();

        toolbarInterface.setTitle(dvrId > 0 ?
                getString(R.string.edit_recording) :
                getString(R.string.add_recording));
    }

    private void updateUI() {

        titleLabelTextView.setVisibility(serverStatus.getHtspVersion() >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setVisibility(serverStatus.getHtspVersion() >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setText(recording.getTitle());

        subtitleLabelTextView.setVisibility(serverStatus.getHtspVersion() >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setVisibility(serverStatus.getHtspVersion() >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setText(recording.getSubtitle());

        descriptionLabelTextView.setVisibility(serverStatus.getHtspVersion() >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setVisibility(serverStatus.getHtspVersion() >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setText(recording.getDescription());

        stopTimeTextView.setText(getTimeStringFromTimeInMillis(recording.getStop()));
        stopTimeTextView.setOnClickListener(view -> handleTimeSelection(recording.getStop(), RecordingAddEditFragment.this, "stopTime"));

        stopDateTextView.setText(getDateStringFromTimeInMillis(recording.getStop()));
        stopDateTextView.setOnClickListener(view -> handleDateSelection(recording.getStop(), RecordingAddEditFragment.this, "stopDate"));

        stopExtraEditText.setText(String.valueOf(recording.getStopExtra()));

        if (recording.isScheduled() || recording.isRecording()) {
            channelNameLabelTextView.setVisibility(View.GONE);
            channelNameTextView.setVisibility(View.GONE);
        } else {
            Channel channel = repository.getChannelByIdSync(recording.getChannelId());
            channelNameTextView.setText(channel != null ? channel.getChannelName() : getString(R.string.no_channel));
            channelNameTextView.setOnClickListener(view -> {
                // Determine if the server supports recording on all channels
                boolean allowRecordingOnAllChannels = serverStatus.getHtspVersion() >= 21;
                handleChannelListSelection(recording.getChannelId(), RecordingAddEditFragment.this, allowRecordingOnAllChannels);
            });
        }

        isEnabledCheckbox.setVisibility(serverStatus.getHtspVersion() >= 23 && !recording.isRecording() ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(recording.getEnabled() == 1);

        priorityTextView.setVisibility(!recording.isRecording() ? View.VISIBLE : View.GONE);
        priorityTextView.setText(priorityList[recording.getPriority()]);
        priorityTextView.setOnClickListener(view -> handlePrioritySelection(priorityList, recording.getPriority(), RecordingAddEditFragment.this));

        if (recordingProfilesList.length == 0 || recording.isRecording()) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);

            recordingProfileNameTextView.setText(recordingProfilesList[recordingProfileNameId]);
            recordingProfileNameTextView.setOnClickListener(view -> handleRecordingProfileSelection(recordingProfilesList, recordingProfileNameId, RecordingAddEditFragment.this));
        }

        startTimeTextView.setVisibility(!recording.isRecording() ? View.VISIBLE : View.GONE);
        startTimeTextView.setText(getTimeStringFromTimeInMillis(recording.getStart()));
        startTimeTextView.setOnClickListener(view -> handleTimeSelection(recording.getStart(), RecordingAddEditFragment.this, "startTime"));

        startDateTextView.setVisibility(!recording.isRecording() ? View.VISIBLE : View.GONE);
        startDateTextView.setText(getDateStringFromTimeInMillis(recording.getStart()));
        startDateTextView.setOnClickListener(view -> handleDateSelection(recording.getStart(), RecordingAddEditFragment.this, "startDate"));

        // Add the additional pre- and post recording values in minutes
        startExtraEditText.setVisibility(!recording.isRecording() ? View.VISIBLE : View.GONE);
        startExtraEditText.setText(String.valueOf(recording.getStartExtra()));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("dvrId", dvrId);
        outState.putInt("configName", recordingProfileNameId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cancel();
                return true;
            case R.id.menu_save:
                save();
                return true;
            case R.id.menu_cancel:
                cancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        if (TextUtils.isEmpty(recording.getTitle()) && serverStatus.getHtspVersion() >= 21) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), getString(R.string.error_empty_title), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (recording.getChannelId() == 0) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), "Please select a channel", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (dvrId > 0) {
            updateRecording();
        } else {
            addRecording();
        }
    }

    private void addRecording() {
        Intent intent = getIntentData();
        intent.setAction("addDvrEntry");
        activity.startService(intent);
        activity.finish();
    }

    private void updateRecording() {
        Intent intent = getIntentData();
        intent.setAction("updateDvrEntry");
        intent.putExtra("id", dvrId);
        activity.startService(intent);
        activity.finish();
    }

    private Intent getIntentData() {
        Intent intent = new Intent(activity, EpgSyncService.class);
        intent.putExtra("title", recording.getTitle());
        intent.putExtra("subtitle", recording.getSubtitle());
        intent.putExtra("description", recording.getDescription());
        // Pass on seconds not milliseconds
        intent.putExtra("stop", recording.getStop());
        intent.putExtra("stopExtra", recording.getStopExtra());

        if (!recording.isScheduled()) {
            intent.putExtra("channelId", recording.getChannelId());
        }
        if (!recording.isRecording()) {
            // Pass on seconds not milliseconds
            intent.putExtra("start", recording.getStart());
            intent.putExtra("startExtra", recording.getStartExtra());
            intent.putExtra("priority", recording.getPriority());
            intent.putExtra("enabled", recording.getEnabled());
        }
        // Add the recording profile if available and enabled
        ServerProfile profile = profileRepository.getRecordingServerProfile();
        if (profile != null && profile.isEnabled()
                && (recordingProfileNameTextView.getText().length() > 0)
                && serverStatus.getHtspVersion() >= 16
                && isUnlocked) {
            // Use the selected profile. If no change was done in the
            // selection then the default one from the connection setting will be used
            intent.putExtra("configName", recordingProfileNameTextView.getText().toString());
        }
        return intent;
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private void cancel() {
        // Show confirmation dialog to cancel
        new MaterialDialog.Builder(activity)
                .content(R.string.cancel_edit_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        activity.finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.cancel();
                    }
                }).show();
    }

    @Override
    public void onChannelIdSelected(int which) {
        if (which > 0) {
            recording.setChannelId(which);
            Channel channel = repository.getChannelByIdSync(which);
            channelNameTextView.setText(channel.getChannelName());
        } else {
            channelNameTextView.setText(R.string.all_channels);
        }
    }

    @Override
    public void onTimeSelected(long milliSeconds, String tag) {
        if (tag.equals("startTime")) {
            recording.setStart(milliSeconds);
            startTimeTextView.setText(getTimeStringFromTimeInMillis(milliSeconds));
        } else if (tag.equals("stopTime")) {
            recording.setStop(milliSeconds);
            stopTimeTextView.setText(getTimeStringFromTimeInMillis(milliSeconds));
        }
    }

    @Override
    public void onDateSelected(long milliSeconds, String tag) {
        if (tag.equals("startDate")) {
            recording.setStart(milliSeconds);
            startDateTextView.setText(getDateStringFromTimeInMillis(milliSeconds));
        } else if (tag.equals("stopDate")) {
            recording.setStop(milliSeconds);
            stopDateTextView.setText(getDateStringFromTimeInMillis(milliSeconds));
        }
    }

    @Override
    public void onPrioritySelected(int which) {
        priorityTextView.setText(priorityList[which]);
        recording.setPriority(which);
    }

    @Override
    public void onProfileSelected(int which) {
        recordingProfileNameTextView.setText(recordingProfilesList[which]);
        recordingProfileNameId = which;
    }

    @Override
    public void onBackPressed() {
        cancel();
    }

    @OnTextChanged(R.id.title)
    void onTitleTextChanged(CharSequence text, int start, int count, int after) {
        Log.d(TAG, "onTitleTextChanged() called with: text = [" + text + "], start = [" + start + "], count = [" + count + "], after = [" + after + "]");
        recording.setTitle(text.toString());
    }

    @OnTextChanged(R.id.subtitle)
    void onNameTextChanged(CharSequence text, int start, int count, int after) {
        Log.d(TAG, "onNameTextChanged() called with: text = [" + text + "], start = [" + start + "], count = [" + count + "], after = [" + after + "]");
        recording.setSubtitle(text.toString());
    }

    @OnTextChanged(R.id.description)
    void onDirectoryTextChanged(CharSequence text, int start, int count, int after) {
        Log.d(TAG, "onDirectoryTextChanged() called with: text = [" + text + "], start = [" + start + "], count = [" + count + "], after = [" + after + "]");
        recording.setDescription(text.toString());
    }

    @OnCheckedChanged(R.id.is_enabled)
    void onEnabledCheckboxChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "onEnabledCheckboxChanged() called with: buttonView = [" + buttonView + "], isChecked = [" + isChecked + "]");
        recording.setEnabled(isChecked ? 1 : 0);
    }

    @OnTextChanged(R.id.start_extra)
    void onStartExtraTextChanged(CharSequence text, int start, int count, int after) {
        try {
            recording.setStartExtra(Long.valueOf(text.toString()));
        } catch (NumberFormatException ex) {
            recording.setStartExtra(2);
        }
    }

    @OnTextChanged(R.id.stop_extra)
    void onStopExtraTextChanged(CharSequence text, int start, int count, int after) {
        try {
            recording.setStopExtra(Long.valueOf(text.toString()));
        } catch (NumberFormatException ex) {
            recording.setStopExtra(2);
        }
    }
}
