package org.bcss.collect.naxa.substages.data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.bcss.collect.android.application.Collect;
import org.bcss.collect.naxa.common.BaseLocalDataSource;
import org.bcss.collect.naxa.common.Constant;
import org.bcss.collect.naxa.common.FieldSightDatabase;
import org.bcss.collect.naxa.scheduled.data.ScheduleForm;
import org.bcss.collect.naxa.stages.data.SubStage;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class SubStageLocalSource implements BaseLocalDataSource<SubStage> {

    private static SubStageLocalSource INSTANCE;
    private SubStageDAO dao;


    private SubStageLocalSource() {
        FieldSightDatabase database = FieldSightDatabase.getDatabase(Collect.getInstance());//todo inject context
        this.dao = database.getSubStageDAO();
    }

    public static SubStageLocalSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SubStageLocalSource();
        }
        return INSTANCE;
    }

    @Override
    public LiveData<List<SubStage>> getAll() {
        return dao.getAllSubStages();
    }

    public LiveData<List<SubStage>> getByStageId(String stageId, String siteTypeId) {
        MediatorLiveData<List<SubStage>> mediatorLiveData = new MediatorLiveData<>();
        LiveData<List<SubStage>> forms = dao.getByStageId(stageId);


        mediatorLiveData.addSource(forms, new Observer<List<SubStage>>() {
            @Override
            public void onChanged(@Nullable List<SubStage> subStages) {
                if (subStages != null) {

                    Observable.just(subStages)
                            .map(new Function<List<SubStage>, List<SubStage>>() {
                                @Override
                                public List<SubStage> apply(List<SubStage> subStages) throws Exception {
                                    ArrayList<SubStage> filteredSubstages = new ArrayList<>();

                                    for (SubStage subStage : subStages) {
                                        if (TextUtils.isEmpty(siteTypeId)
                                                || Constant.DEFAULT_SITE_TYPE.equals(siteTypeId)
                                                || subStage.getTagIds().contains(siteTypeId)
                                                || subStage.getTagIds().size() == 0) {

                                            filteredSubstages.add(subStage);
                                        }
                                    }
                                    return filteredSubstages;
                                }
                            })
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new DisposableObserver<List<SubStage>>() {
                                @Override
                                public void onNext(List<SubStage> subStages) {
                                    mediatorLiveData.setValue(subStages);
                                    mediatorLiveData.removeSource(forms);

                                }

                                @Override
                                public void onError(Throwable e) {
                                    e.printStackTrace();
                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                }
            }
        });

        return mediatorLiveData;
    }

    @Override
    public void save(SubStage... items) {
        AsyncTask.execute(() -> dao.insert(items));
    }

    @Override
    public void save(ArrayList<SubStage> items) {
        AsyncTask.execute(() -> dao.insert(items));
    }

    @Override
    public void updateAll(ArrayList<SubStage> items) {
        AsyncTask.execute(() -> dao.updateAll(items));
    }

    public Observable<List<SubStage>> getByStageIdMaybe(String id, String siteTypeId ) {
         return dao.getByStageIdMaybe(id).toObservable()
         .flatMap(new Function<List<SubStage>, ObservableSource<List<SubStage>>>() {
             @Override
             public ObservableSource<List<SubStage>> apply(List<SubStage> subStages) throws Exception {
                 ArrayList<SubStage> filteredSubstages = new ArrayList<>();

                 for (SubStage subStage : subStages) {
                     if (TextUtils.isEmpty(siteTypeId)
                             || Constant.DEFAULT_SITE_TYPE.equals(siteTypeId)
                             || subStage.getTagIds().contains(siteTypeId)
                             || subStage.getTagIds().size() == 0) {

                         filteredSubstages.add(subStage);
                     }
                 }
                 return Observable.just(filteredSubstages);
             }
         });
    }


    public LiveData<List<SubStage>> getById(String fsFormId) {
        return dao.getById(fsFormId);
    }
}
