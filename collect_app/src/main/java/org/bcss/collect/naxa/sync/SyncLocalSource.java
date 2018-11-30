package org.bcss.collect.naxa.sync;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.bcss.collect.android.application.Collect;
import org.bcss.collect.naxa.common.Constant;
import org.bcss.collect.naxa.common.database.FieldSightConfigDatabase;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;

public class SyncLocalSource implements BaseLocalDataSourceRX<Sync> {

    private static SyncLocalSource INSTANCE;
    private SyncDAO syncDAO;

    public static SyncLocalSource getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new SyncLocalSource();
        }

        return INSTANCE;
    }


    private SyncLocalSource() {

        FieldSightConfigDatabase database = FieldSightConfigDatabase.getDatabase(Collect.getInstance());//todo inject context
        this.syncDAO = database.getSyncDao();
    }


    @Override
    public LiveData<List<Sync>> getAll() {
        return syncDAO.getAll();
    }

    @Override
    public Completable save(Sync... items) {
        return Completable.fromAction(() -> {
            syncDAO.insert(items);
        });
    }

    @Override
    public Completable save(ArrayList<Sync> items) {
        return Completable.fromAction(() -> {
            syncDAO.insert(items);
        });
    }

    @Override
    public void updateAll(ArrayList<Sync> items) {

    }

    Completable toggleAllChecked() {

        return syncDAO.selectedItemsCount()
                .toObservable()
                .flatMapCompletable(integer -> Completable.fromAction(() -> {
                    if (integer > 0) {
                        syncDAO.markAllAsUnChecked();
                    } else {
                        syncDAO.markAllAsChecked();
                    }

                }));


    }

    public Single<Integer> selectedItemCount() {
        return syncDAO.selectedItemsCount();
    }

    public LiveData<Integer> selectedItemCountLive() {
        return syncDAO.selectedItemsCountLive();
    }

    public LiveData<Integer> runningItemCountLive() {
        return syncDAO.runningItemCountLive(Constant.DownloadStatus.RUNNING);
    }

    public Completable toggleSingleItem(Sync sync) {
        return Completable.fromAction(() -> {
            if (sync.isChecked()) {
                syncDAO.markAsUnchecked(sync.getUid());
            } else {
                syncDAO.markAsChecked(sync.getUid());
            }
        });
    }

    public Single<List<Sync>> getAllChecked() {
        return syncDAO.getAllChecked();
    }


    public void markAsRunning(int uid) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                syncDAO.markSelectedAsRunning(uid, Constant.DownloadStatus.RUNNING);
                clearErrorMessage(uid);
            }
        });

    }

    public void markAsFailed(int uid) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                syncDAO.markSelectedAsRunning(uid, Constant.DownloadStatus.FAILED);
            }
        });

    }

    private void updateErrorMessage(int uid, String errorMessage) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                syncDAO.updateErrorMessage(uid, errorMessage);
            }
        });
    }

    public void addErrorMessage(int uid, String errorMessage) {
        updateErrorMessage(uid, errorMessage);
    }


    public void clearErrorMessage(int uid) {
        updateErrorMessage(uid, "");
    }


    public void markAsPending(int uid) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                syncDAO.markSelectedAsRunning(uid, Constant.DownloadStatus.PENDING);
            }
        });

    }

    public void markAsCompleted(int uid) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                syncDAO.markSelectedAsRunning(uid, Constant.DownloadStatus.COMPLETED);
                clearErrorMessage(uid);
            }
        });
    }

    public void updateProgress(int uid, int total, int progress) {
        AsyncTask.execute(() -> syncDAO.updateProgress(uid, total, progress));
    }
}