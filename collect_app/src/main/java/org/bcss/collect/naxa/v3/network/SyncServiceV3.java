package org.bcss.collect.naxa.v3.network;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.bcss.collect.naxa.common.Constant;
import org.bcss.collect.naxa.common.ODKFormRemoteSource;
import org.bcss.collect.naxa.common.rx.RetrofitException;
import org.bcss.collect.naxa.educational.EducationalMaterialsRemoteSource;
import org.bcss.collect.naxa.login.model.Project;
import org.bcss.collect.naxa.login.model.Site;
import org.bcss.collect.naxa.site.db.SiteLocalSource;
import org.bcss.collect.naxa.site.db.SiteRemoteSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SyncServiceV3 extends IntentService {
    /***
     *
     *
     * @Author: Yubaraj Poudel
     * @Since : 14/05/2019
     */

    ArrayList<Project> selectedProject;
    HashMap<String, List<Syncable>> selectedMap = null;

    public SyncServiceV3() {
        super("SyncserviceV3");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            selectedProject = Objects.requireNonNull(intent).getParcelableArrayListExtra("projects");
            Timber.i("SyncServiceV3 slectedProject size = %d", selectedProject.size());


            selectedMap = (HashMap<String, List<Syncable>>) intent.getSerializableExtra("selection");

            for (String key : selectedMap.keySet()) {
                Timber.i(readaableSyncParams(key, selectedMap.get(key)));
            }

            //Start syncing sites
//            Disposable disposable = downloadByRegionObservable(selectedProject, selectedMap)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribeWith(new DisposableObserver<Object>() {
//                        @Override
//                        public void onNext(Object o) {
//                            //unused
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            Timber.e(e);
//                        }
//
//                        @Override
//                        public void onComplete() {
//                            //unused
//                        }
//                    });


//            Disposable projectEduMatObservable = Observable.just(selectedProject)
//                    .flatMapIterable((Function<ArrayList<Project>, Iterable<Project>>) projects -> projects)
//                    .filter(project -> selectedMap.get(project.getId()).get(2).sync)
//                    .flatMap(new Function<Project, Observable<String>>() {
//                        @Override
//                        public Observable<String> apply(Project project) throws Exception {
//                            saveState(project.getId(), 2, "", true, Constant.DownloadStatus.RUNNING);
//                            return EducationalMaterialsRemoteSource.getInstance().getByProjectId(project.getId()).toObservable();
//                        }
//                    })
//                    .subscribe(new Consumer<String>() {
//                        @Override
//                        public void accept(String projectId) throws Exception {
//                            saveState(projectId, 2, "", false, Constant.DownloadStatus.COMPLETED);
//                            Timber.i("Project Educational material Sync completed");
//                        }
//                    }, throwable -> {
//                        String url = getFailedFormUrl(throwable)[0];
//                        String projectId = getFailedFormUrl(throwable)[1];
//                        saveState(projectId, 2, url, false, Constant.DownloadStatus.FAILED);
//                        Timber.d("Download for educational stopped at %s for %s", url, projectId);
//                    });


            Disposable formsDownloadObservable = Observable.just(selectedProject)
                    .flatMapIterable((Function<ArrayList<Project>, Iterable<Project>>) projects -> projects)

                    .filter(project -> selectedMap.get(project.getId()).get(1).sync)
                    .flatMap(new Function<Project, Observable<Project>>() {
                        @Override
                        public Observable<Project> apply(Project project) throws Exception {
                            saveState(project.getId(), 1, "", true, Constant.DownloadStatus.RUNNING);
                            return ODKFormRemoteSource.getInstance().getByProjectId(project);
                        }
                    })
                    .subscribeWith(new DisposableObserver<Project>() {
                        @Override
                        public void onNext(Project project) {
                            markAsCompleted(project.getId(), 1);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            String url = getFailedFormUrl(throwable)[0];
                            String projectId = getFailedFormUrl(throwable)[1];
                            markAsFailed(projectId, 1, url);
                            Timber.e(throwable);
                        }

                        @Override
                        public void onComplete() {
                        }
                    });


        } catch (
                NullPointerException e) {
            Timber.e(e);
        }

    }

    private void markAsFailed(String projectId, int type, String failedUrl) {
        saveState(projectId, type, failedUrl, false, Constant.DownloadStatus.FAILED);
        Timber.e("Download stopped %s for project %s", failedUrl, projectId);
    }

    private void markAsRunning(String projectId, int type) {
        saveState(projectId, type, "", true, Constant.DownloadStatus.RUNNING);
    }


    private void markAsCompleted(String projectId, int type) {
        saveState(projectId, type, "", false, Constant.DownloadStatus.COMPLETED);
    }

    private void saveState(String projectId, int type, String failedUrl, boolean started, int status) {
        Timber.d("saving for for %d stopped at %s for %s", type, failedUrl, projectId);
        if (selectedMap != null && selectedMap.containsKey(projectId)) {
//            selectedMap.get(projectId).get(type).completed = true;
            SyncStat syncStat = new SyncStat(projectId, type + "", failedUrl, started, status, System.currentTimeMillis());
            SyncLocalSourcev3.getInstance().save(syncStat);
        }
    }

    private String[] getFailedFormUrl(Throwable throwable) {
        String failedUrl = "";
        String projectId = "";
        if (throwable instanceof RetrofitException) {
            RetrofitException retrofitException = (RetrofitException) throwable;
            String msg = retrofitException.getMessage();
            failedUrl = retrofitException.getUrl();
            projectId = retrofitException.getProjectId();

        }

        return new String[]{failedUrl, projectId};
    }


    private Function<Project, Observable<Object>> getSitesObservable() {
        return new Function<Project, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Project project) throws Exception {
                Observable<Object> regionSitesObservable = Observable.just(project.getRegionList())
                        .flatMapIterable((Function<List<Region>, Iterable<Region>>) regions -> regions)
                        .flatMapSingle(new Function<Region, SingleSource<SiteResponse>>() {
                            @Override
                            public SingleSource<SiteResponse> apply(Region region) {
                                return SiteRemoteSource.getInstance().getSitesByRegionId(region.getId())
                                        .doOnSuccess(saveSites());
                            }
                        })
                        .concatMap(new Function<SiteResponse, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(SiteResponse siteResponse) throws Exception {
                                if (siteResponse.getNext() == null) {
                                    return Observable.just(siteResponse);
                                }

                                return Observable.just(siteResponse)
                                        .concatWith(getSitesByUrl(siteResponse.getNext()));

                            }
                        });


                Observable<Object> projectObservable = Observable.just(project)
                        .filter(Project::getHasClusteredSites)
                        .flatMapSingle((Function<Project, SingleSource<SiteResponse>>) project1 -> SiteRemoteSource.getInstance().getSitesByProjectId(project1.getId()).doOnSuccess(saveSites()))
                        .concatMap(new Function<SiteResponse, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(SiteResponse siteResponse) {
                                if (siteResponse.getNext() == null) {
                                    return Observable.just(siteResponse);
                                }
                                return Observable.just(siteResponse)
                                        .concatWith(getSitesByUrl(siteResponse.getNext()));

                            }
                        });


                return Observable.concat(projectObservable, regionSitesObservable)
                        .doOnSubscribe(disposable -> markAsRunning(project.getId(), 0))
                        .onErrorReturn(throwable -> {
                            String url = getFailedFormUrl(throwable)[0];
                            markAsFailed(project.getId(), 0, url);
                            return project.getId();
                        })
                        .doOnNext(o -> {
                            boolean hasErrorBeenThrown = o instanceof String;
                            if (!hasErrorBeenThrown) {//error has been thrown
                                markAsCompleted(project.getId(), 0);
                            }
                        });
            }
        };
    }

    private Observable<Object> downloadByRegionObservable(ArrayList<Project> selectedProject, HashMap<String, List<Syncable>> selectedMap) {
        return Observable.just(selectedProject)
                .flatMapIterable((Function<ArrayList<Project>, Iterable<Project>>) projects -> projects)
                .filter(project -> selectedMap.get(project.getId()).get(0).sync)
                .flatMap(getSitesObservable());


    }


    private Consumer<? super SiteResponse> saveSites() {
        return (Consumer<SiteResponse>) siteResponse -> {
            Timber.i("Saving %d sites", siteResponse.getResult().size());

            SiteLocalSource.getInstance().save((ArrayList<Site>) siteResponse.getResult());
        };
    }

    private ObservableSource<? extends SiteResponse> getSitesByUrl(String url) {
        return SiteRemoteSource.getInstance().getSitesByURL(url)
                .toObservable()
                .filter(new Predicate<SiteResponse>() {
                    @Override
                    public boolean test(SiteResponse siteResponse) {
                        return siteResponse.getNext() != null;
                    }
                })
                .doOnNext(saveSites())
                .flatMap(new Function<SiteResponse, ObservableSource<? extends SiteResponse>>() {
                    @Override
                    public ObservableSource<? extends SiteResponse> apply(SiteResponse siteResponse) {
                        return getSitesByUrl(siteResponse.getNext());
                    }
                });
    }


    private String readaableSyncParams(String projectName, List<Syncable> list) {
        String logString = "";
        for (Syncable syncable : list) {
            logString += "\n title = " + syncable.getTitle() + ", sync = " + syncable.getSync();
        }
        return String.format("%s \n params = %s", projectName, logString);
    }
}

