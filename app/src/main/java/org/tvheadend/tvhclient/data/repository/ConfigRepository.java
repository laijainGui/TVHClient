package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ServerProfileDao;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.dao.TranscodingProfileDao;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TranscodingProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ConfigRepository {
    private String TAG = getClass().getSimpleName();
    private final AppDatabase db;

    public ConfigRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public TranscodingProfile getPlaybackTranscodingProfile() {
        try {
            return new LoadTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TranscodingProfile getRecordingTranscodingProfile() {
        try {
            return new LoadTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "recording").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServerProfile getPlaybackServerProfileById(int id) {
        try {
            return new LoadServerProfileByIdTask(db.serverProfileDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServerProfile getRecordingServerProfileById(int id) {
        try {
            return new LoadServerProfileByIdTask(db.serverProfileDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServerProfile getCastingServerProfileById(int id) {
        try {
            return new LoadServerProfileByIdTask(db.serverProfileDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getAllPlaybackServerProfiles() {
        try {
            return new LoadAllServerProfilesTask(db.serverProfileDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getAllRecordingServerProfiles() {
        try {
            return new LoadAllServerProfilesTask(db.serverProfileDao(), "recording").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updatePlaybackServerProfile(int id) {
        new UpdateServerProfileTask(db.serverStatusDao(), "playback", id).execute();
    }

    public void updateRecordingServerProfile(int id) {
        new UpdateServerProfileTask(db.serverStatusDao(), "recording", id).execute();
    }

    public void updateCastingServerProfile(int id) {
        new UpdateServerProfileTask(db.serverStatusDao(), "casting", id).execute();
    }

    public ServerStatus getServerStatus() {
        try {
            return new LoadServerStatusTask(db.serverStatusDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class LoadServerStatusTask extends AsyncTask<Void, Void, ServerStatus> {
        private final ServerStatusDao dao;

        LoadServerStatusTask(ServerStatusDao dao) {
            this.dao = dao;
        }

        @Override
        protected ServerStatus doInBackground(Void... voids) {
            return dao.loadServerStatusSync();
        }
    }

    protected static class LoadTranscodingProfileTask extends AsyncTask<Void, Void, TranscodingProfile> {
        private final TranscodingProfileDao transcodingProfileDao;
        private final ServerStatusDao serverStatusDao;
        private final String type;

        LoadTranscodingProfileTask(TranscodingProfileDao transcodingProfileDao, ServerStatusDao serverStatusDao, String type) {
            this.transcodingProfileDao = transcodingProfileDao;
            this.serverStatusDao = serverStatusDao;
            this.type = type;
        }

        @Override
        protected TranscodingProfile doInBackground(Void... voids) {
            ServerStatus serverStatus = serverStatusDao.loadServerStatusSync();
            TranscodingProfile profile = null;
            switch (type) {
                case "playback":
                    profile = transcodingProfileDao.loadProfileByIdSync(serverStatus.getPlaybackTranscodingProfileId());
                    break;
                case "recording":
                    profile = transcodingProfileDao.loadProfileByIdSync(serverStatus.getRecordingTranscodingProfileId());
                    break;
            }
            if (profile != null) {
                return profile;
            } else {
                return new TranscodingProfile();
            }
        }
    }

    protected static class LoadServerProfileByIdTask extends AsyncTask<Void, Void, ServerProfile> {
        private final ServerProfileDao serverProfileDao;
        private final int id;

        LoadServerProfileByIdTask(ServerProfileDao serverProfileDao, int id) {
            this.serverProfileDao = serverProfileDao;
            this.id = id;
        }

        @Override
        protected ServerProfile doInBackground(Void... voids) {
            ServerProfile profile = serverProfileDao.loadProfileByIdSync(id);
            if (profile != null) {
                return profile;
            } else {
                return new ServerProfile();
            }
        }
    }

    protected static class LoadAllServerProfilesTask extends AsyncTask<Void, Void, List<ServerProfile>> {
        private final ServerProfileDao dao;
        private final String type;

        LoadAllServerProfilesTask(ServerProfileDao dao, String type) {
            this.dao = dao;
            this.type = type;
        }

        @Override
        protected List<ServerProfile> doInBackground(Void... voids) {
            List<ServerProfile> profiles = null;
            switch (type) {
                case "playback":
                    profiles = dao.loadAllPlaybackProfilesSync();
                    break;
                case "recording":
                    profiles = dao.loadAllRecordingProfilesSync();
                    break;
            }
            if (profiles != null) {
                return profiles;
            } else {
                return new ArrayList<>();
            }
        }
    }

    protected static class UpdateServerProfileTask extends AsyncTask<Void, Void, Void> {
        private final String type;
        private final int id;
        private final ServerStatusDao serverStatusDao;

        UpdateServerProfileTask(ServerStatusDao serverStatusDao, String type, int id) {
            this.serverStatusDao = serverStatusDao;
            this.type = type;
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ServerStatus serverStatus = serverStatusDao.loadServerStatusSync();
            Log.d("Update", "doInBackground: updating profile " + type + ", id " + id);
            switch (type) {
                case "playback":
                    serverStatus.setPlaybackServerProfileId(id);
                    serverStatusDao.update(serverStatus);
                    break;
                case "recording":
                    serverStatus.setRecordingServerProfileId(id);
                    serverStatusDao.update(serverStatus);
                    break;
                case "casting":
                    serverStatus.setCastingServerProfileId(id);
                    serverStatusDao.update(serverStatus);
                    break;
            }
            return null;
        }
    }

    public void updatePlaybackTranscodingProfile(TranscodingProfile playbackProfile) {
        new UpdateTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "playback", playbackProfile).execute();
    }

    public void updateRecordingTranscodingProfile(TranscodingProfile recordingProfile) {
        new UpdateTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "recording", recordingProfile).execute();
    }

    protected static class UpdateTranscodingProfileTask extends AsyncTask<Void, Void, Void> {
        private final TranscodingProfileDao transcodingProfileDao;
        private final String type;
        private final TranscodingProfile profile;
        private final ServerStatusDao serverStatusDao;

        UpdateTranscodingProfileTask(TranscodingProfileDao transcodingProfileDao, ServerStatusDao serverStatusDao, String type, TranscodingProfile profile) {
            this.transcodingProfileDao = transcodingProfileDao;
            this.serverStatusDao = serverStatusDao;
            this.type = type;
            this.profile = profile;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ServerStatus serverStatus = serverStatusDao.loadServerStatusSync();
            switch (type) {
                case "playback":
                    if (profile.getId() == 0) {
                        int id = (int) transcodingProfileDao.insert(profile);
                        serverStatus.setPlaybackTranscodingProfileId(id);
                    } else {
                        transcodingProfileDao.update(profile);
                        serverStatus.setPlaybackTranscodingProfileId(profile.getId());
                    }
                    serverStatusDao.update(serverStatus);
                    break;
                case "recording":
                    if (profile.getId() == 0) {
                        int id = (int) transcodingProfileDao.insert(profile);
                        serverStatus.setRecordingTranscodingProfileId(id);
                    } else {
                        transcodingProfileDao.update(profile);
                        serverStatus.setRecordingTranscodingProfileId(profile.getId());
                    }
                    serverStatusDao.update(serverStatus);
                    break;
            }
            return null;
        }
    }
}
