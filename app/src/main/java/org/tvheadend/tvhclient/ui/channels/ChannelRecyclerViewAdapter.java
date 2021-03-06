package org.tvheadend.tvhclient.ui.channels;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChannelRecyclerViewAdapter extends RecyclerView.Adapter<ChannelRecyclerViewAdapter.RecyclerViewHolder> implements Filterable {
    private String TAG = getClass().getSimpleName();

    private final ChannelClickCallback channelClickCallback;
    private List<Channel> channelList;
    private List<Channel> channelListFiltered;
    private SharedPreferences sharedPreferences;
    private Context context;
    private int selectedPosition = 0;

    ChannelRecyclerViewAdapter(Context context, List<Channel> channelList, ChannelClickCallback channelClickCallback) {
        this.context = context;
        this.channelList = channelList;
        this.channelListFiltered = channelList;
        this.channelClickCallback = channelClickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.channel_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        Channel channel = channelListFiltered.get(position);
        holder.itemView.setTag(channel);

        boolean showChannelName = sharedPreferences.getBoolean("showChannelNamePref", true);
        boolean showProgressbar = sharedPreferences.getBoolean("showProgramProgressbarPref", true);
        boolean showSubtitle = sharedPreferences.getBoolean("showProgramSubtitlePref", true);
        boolean showNextProgramTitle = sharedPreferences.getBoolean("showNextProgramPref", true);
        boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);
        boolean showGenreColors = sharedPreferences.getBoolean("showGenreColorsChannelsPref", false);
        boolean playUponChannelClick = sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true);

        // Sets the correct indication when the dual pane mode is active
        // If the item is selected the the arrow will be shown, otherwise
        // only a vertical separation line is displayed.
        if (holder.dualPaneListItemSelection != null) {
            boolean lightTheme = sharedPreferences.getBoolean("lightThemePref", true);
            if (selectedPosition == position) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            }
        }

        // Set the initial values
        holder.progressbar.setProgress(0);
        holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

        holder.channelTextView.setText(channel.getChannelName());
        holder.channelTextView.setVisibility(showChannelName ? View.VISIBLE : View.GONE);

        // Show the regular or large channel icons. Otherwise show the channel name only
        // Assign the channel icon image or a null image
        Bitmap iconBitmap = MiscUtils.getCachedIcon(context, channel.getChannelIcon());
        holder.iconImageView.setImageBitmap(iconBitmap);
        holder.iconTextView.setText(channel.getChannelName());

        if (showChannelIcons) {
            holder.iconImageView.setVisibility(iconBitmap != null ? ImageView.VISIBLE : ImageView.INVISIBLE);
            holder.iconTextView.setVisibility(iconBitmap == null ? ImageView.VISIBLE : ImageView.INVISIBLE);
        } else {
            holder.iconImageView.setVisibility(View.GONE);
            holder.iconTextView.setVisibility(View.GONE);
        }

        if (playUponChannelClick) {
            holder.iconImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    channelClickCallback.onChannelClick(channel.getChannelId());
                }
            });
            holder.iconTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    channelClickCallback.onChannelClick(channel.getChannelId());
                }
            });
        }

        if (channel.getProgramId() > 0) {
            holder.titleTextView.setText(channel.getProgramTitle());

            holder.subtitleTextView.setText(channel.getProgramSubtitle());
            holder.subtitleTextView.setVisibility(showSubtitle && !TextUtils.isEmpty(channel.getProgramSubtitle()) ? View.VISIBLE : View.GONE);

            String time = UIUtils.getTimeText(context, channel.getProgramStart()) + " - " + UIUtils.getTimeText(context, channel.getProgramStop());
            holder.timeTextView.setText(time);
            holder.timeTextView.setVisibility(View.VISIBLE);

            String durationTime = context.getString(R.string.minutes, (int) ((channel.getProgramStop() - channel.getProgramStart()) / 1000 / 60));
            holder.durationTextView.setText(durationTime);
            holder.durationTextView.setVisibility(View.VISIBLE);

            holder.progressbar.setProgress(getProgressPercentage(channel.getProgramStart(), channel.getProgramStop()));
            holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

            if (showGenreColors) {
                int color = UIUtils.getGenreColor(context, channel.getProgramContentType(), 0);
                holder.genreTextView.setBackgroundColor(color);
                holder.genreTextView.setVisibility(View.VISIBLE);
            } else {
                holder.genreTextView.setVisibility(View.GONE);
            }
        } else {
            // The channel does not provide program data. Hide certain views
            holder.titleTextView.setText(R.string.no_data);
            holder.subtitleTextView.setVisibility(View.GONE);
            holder.progressbar.setVisibility(View.GONE);
            holder.timeTextView.setVisibility(View.GONE);
            holder.durationTextView.setVisibility(View.GONE);
            holder.genreTextView.setVisibility(View.GONE);
            holder.nextTitleTextView.setVisibility(View.GONE);
        }

        if (channel.getNextProgramId() > 0) {
            holder.nextTitleTextView.setVisibility(showNextProgramTitle ? View.VISIBLE : View.GONE);
            holder.nextTitleTextView.setText(context.getString(R.string.next_program, channel.getNextProgramTitle()));
        } else {
            holder.nextTitleTextView.setVisibility(View.GONE);
        }
    }

    void addItems(List<Channel> channelList) {
        Log.d(TAG, "addItems() called with: channelList = [" + channelList + "]");
        this.channelList = channelList;
        this.channelListFiltered = channelList;

        if (channelList == null || selectedPosition > channelList.size()) {
            Log.d(TAG, "addItems: selectedPosition > channelList.size() resetting");
            selectedPosition = 0;
        }
    }

    @Override
    public int getItemCount() {
        return channelListFiltered.size();
    }

    public void setPosition(int pos) {
        Log.d(TAG, "setPosition() called with: pos = [" + pos + "]");
        selectedPosition = pos;
    }

    public Channel getItem(int position) {
        Log.d(TAG, "getItem() called with: position = [" + position + "]");
        return channelListFiltered.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    channelListFiltered = channelList;
                } else {
                    List<Channel> filteredList = new ArrayList<>();
                    for (Channel row : channelList) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (row.getChannelName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }
                    channelListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = channelListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                channelListFiltered = (ArrayList<Channel>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.icon)
        ImageView iconImageView;
        @BindView(R.id.icon_text)
        TextView iconTextView;
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.subtitle)
        TextView subtitleTextView;
        @BindView(R.id.next_title)
        TextView nextTitleTextView;
        @BindView(R.id.channel)
        TextView channelTextView;
        @BindView(R.id.time)
        TextView timeTextView;
        @BindView(R.id.duration)
        TextView durationTextView;
        @BindView(R.id.progressbar)
        ProgressBar progressbar;
        @BindView(R.id.state)
        ImageView stateImageView;
        @BindView(R.id.genre)
        TextView genreTextView;
        @Nullable
        @BindView(R.id.dual_pane_list_item_selection)
        ImageView dualPaneListItemSelection;

        RecyclerViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private int getProgressPercentage(long start, long stop) {
        // Get the start and end times to calculate the progress.
        double durationTime = (stop - start);
        double elapsedTime = new Date().getTime() - start;
        // Show the progress as a percentage
        double percentage = 0;
        if (durationTime > 0) {
            percentage = elapsedTime / durationTime;
        }
        return (int) Math.floor(percentage * 100);
    }
}
