package org.tvheadend.tvhclient.ui.programs;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ProgramDao;
import org.tvheadend.tvhclient.data.entity.ProgramWithRecordingsAndChannels;

import java.util.List;

public class ProgramViewModel extends AndroidViewModel {

    private final ProgramDao dao;

    public ProgramViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application.getApplicationContext());
        dao = db.programDao();
    }

    LiveData<List<ProgramWithRecordingsAndChannels>> getPrograms(int channelId, long time) {
        return dao.loadProgramsFromChannelWithinTime(channelId, time);
    }

    LiveData<ProgramWithRecordingsAndChannels> getProgram(int id) {
        return dao.loadProgramById(id);
    }
}
