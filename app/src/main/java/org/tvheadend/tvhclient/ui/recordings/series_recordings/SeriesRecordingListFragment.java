package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.common.RecyclerTouchListener;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.ui.common.RecyclerViewTouchCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingAddEditActivity;
import org.tvheadend.tvhclient.ui.recordings.recordings.RecordingDetailsActivity;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class SeriesRecordingListFragment extends BaseFragment {

    private SeriesRecordingRecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    protected int selectedListPosition;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recyclerview_fragment, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbarInterface.setTitle(getString(R.string.series_recordings));

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("list_position", 0);
        }

        recyclerViewAdapter = new SeriesRecordingRecyclerViewAdapter(getContext(), new ArrayList<>(), serverStatus.getHtspVersion());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(activity.getApplicationContext(), recyclerView, new RecyclerViewTouchCallback() {
            @Override
            public void onClick(View view, int position) {
                showRecordingDetails(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                showPopupMenu(view);
            }
        }));

        SeriesRecordingViewModel viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        viewModel.getRecordings().observe(this, recordings -> {
            recyclerViewAdapter.addItems(recordings);
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showRecordingDetails(selectedListPosition);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("list_position", selectedListPosition);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                Intent intent = new Intent(getActivity(), RecordingAddEditActivity.class);
                intent.putExtra("type", "series_recording");
                getActivity().startActivity(intent);
                return true;
            case R.id.menu_record_remove_all:
                CopyOnWriteArrayList<SeriesRecording> list = new CopyOnWriteArrayList<>(recyclerViewAdapter.getItems());
                menuUtils.handleMenuRemoveAllSeriesRecordingSelection(list);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recording_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!sharedPreferences.getBoolean("hideMenuDeleteAllRecordingsPref", false) && recyclerViewAdapter.getItemCount() > 1) {
            menu.findItem(R.id.menu_record_remove_all).setVisible(true);
        }
        menu.findItem(R.id.menu_add).setVisible(isUnlocked);
    }

    protected void showRecordingDetails(int position) {
        selectedListPosition = position;
        SeriesRecording recording = recyclerViewAdapter.getItem(position);
        if (recording == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(getActivity(), RecordingDetailsActivity.class);
            intent.putExtra("id", recording.getId());
            intent.putExtra("type", "series_recording");
            activity.startActivity(intent);
        } else {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the program details fragment.
            // TODO getListView().setItemChecked(position, true);
            // Check what fragment is currently shown, replace if needed.
            SeriesRecordingDetailsFragment recordingDetailsFragment = (SeriesRecordingDetailsFragment) getFragmentManager().findFragmentById(R.id.right_fragment);
            if (recordingDetailsFragment == null || !recordingDetailsFragment.getShownId().equals(recording.getId())) {
                // Make new fragment to show this selection.
                recordingDetailsFragment = SeriesRecordingDetailsFragment.newInstance(recording.getId());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.right_fragment, recordingDetailsFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    public void showPopupMenu(View view) {
        final SeriesRecording seriesRecording = (SeriesRecording) view.getTag();
        if (getActivity() == null || seriesRecording == null) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.series_recordings_popup_menu, popupMenu.getMenu());
        (popupMenu.getMenu().findItem(R.id.menu_edit)).setVisible(isUnlocked);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_edit:
                    Intent intent = new Intent(getActivity(), RecordingAddEditActivity.class);
                    intent.putExtra("id", seriesRecording.getId());
                    intent.putExtra("type", "series_recording");
                    getActivity().startActivity(intent);
                    return true;
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(seriesRecording.getTitle());
                    return true;
                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(seriesRecording.getTitle());
                    return true;
                case R.id.menu_record_remove:
                    menuUtils.handleMenuRemoveSeriesRecordingSelection(seriesRecording.getId(), seriesRecording.getTitle(), null);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
    }
}
