package org.bcss.collect.naxa.common;

import android.os.Handler;

import org.bcss.collect.android.R;
import org.bcss.collect.android.application.Collect;
import org.bcss.collect.android.listeners.FormDownloaderListener;
import org.bcss.collect.android.logic.FormDetails;
import org.bcss.collect.naxa.common.exception.DownloadRunningException;
import org.bcss.collect.naxa.common.utilities.FieldSightFormListDownloadUtils;
import org.bcss.collect.naxa.login.model.Project;
import org.bcss.collect.naxa.network.APIEndpoint;
import org.bcss.collect.naxa.onboarding.DownloadProgress;
import org.bcss.collect.naxa.onboarding.SyncableItem;
import org.bcss.collect.naxa.onboarding.XMLForm;
import org.bcss.collect.naxa.onboarding.XMLFormBuilder;
import org.bcss.collect.naxa.onboarding.XMLFormDownloadReceiver;
import org.bcss.collect.naxa.onboarding.XMLFormDownloadService;
import org.bcss.collect.naxa.project.data.ProjectLocalSource;
import org.bcss.collect.naxa.sync.DownloadableItemLocalSource;
import org.bcss.collect.naxa.sync.SyncRepository;
import org.odk.collect.android.utilities.FormDownloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.bcss.collect.naxa.common.Constant.DownloadUID.PROJECT_SITES;
import static org.bcss.collect.naxa.common.Constant.EXTRA_OBJECT;
import static org.odk.collect.android.activities.FormDownloadList.FORMDETAIL_KEY;
import static org.odk.collect.android.activities.FormDownloadList.FORMID_DISPLAY;
import static org.odk.collect.android.activities.FormDownloadList.FORMNAME;
import static org.odk.collect.android.activities.FormDownloadList.FORM_ID_KEY;
import static org.odk.collect.android.activities.FormDownloadList.FORM_VERSION_KEY;
import static org.odk.collect.android.utilities.DownloadFormListUtils.DL_AUTH_REQUIRED;
import static org.odk.collect.android.utilities.DownloadFormListUtils.DL_ERROR_MSG;

public class ODKFormRemoteSource {


    private final static ODKFormRemoteSource INSTANCE = new ODKFormRemoteSource();

    public static ODKFormRemoteSource getInstance() {
        return INSTANCE;
    }

    @Deprecated
    public Observable<DownloadProgress> fetchODKForms() {
        int uid = Constant.DownloadUID.ODK_FORMS;
        return Observable.create(emitter -> {
            XMLFormDownloadReceiver xmlFormDownloadReceiver = new XMLFormDownloadReceiver(new Handler());

            xmlFormDownloadReceiver.setReceiver((resultCode, resultData) -> {
                switch (resultCode) {
                    case DownloadProgress.STATUS_RUNNING:
                        break;
                    case DownloadProgress.STATUS_PROGRESS_UPDATE:
                        DownloadProgress progress = (DownloadProgress) resultData.getSerializable(EXTRA_OBJECT);
                        emitter.onNext(progress);
                        DownloadableItemLocalSource.getINSTANCE().updateProgress(Constant.DownloadUID.ALL_FORMS, progress.getTotal(), progress.getProgress());
                        break;
                    case DownloadProgress.STATUS_ERROR:
                        emitter.onError(new RuntimeException(resultData.getString(Constant.EXTRA_MESSAGE)));
                        break;
                    case DownloadProgress.STATUS_FINISHED_FORM:
                        emitter.onComplete();
                        break;
                }
            });

            XMLFormDownloadService.start(Collect.getInstance(), xmlFormDownloadReceiver);
        });
    }


    private void updateProgress(String message, int current, int total) {
        Timber.i("%s %d %d", message, current, total);
    }


    public Observable<Project> getByProjectId(Project project) {
        ArrayList<Project> projects = new ArrayList<>();
        projects.add(project);

        return Observable.just(projects)
                .subscribeOn(Schedulers.io())
                .map(mapProjectsToXMLForm())
                .flatMapIterable((Function<ArrayList<XMLForm>, Iterable<XMLForm>>) xmlForms -> xmlForms)
                .flatMap((Function<XMLForm, ObservableSource<HashMap<String, FormDetails>>>) this::downloadFormlist)
                .toList()
                .map(hashMaps -> {
                    HashMap<String, FormDetails> result = new HashMap<>();
                    for (HashMap<String, FormDetails> hashMap : hashMaps) {
                        result.putAll(hashMap);
                    }
                    return result;

                })
                .flatMapObservable(new Function<HashMap<String, FormDetails>, ObservableSource<ArrayList<FormDetails>>>() {
                    @Override
                    public ObservableSource<ArrayList<FormDetails>> apply(HashMap<String, FormDetails> stringFormDetailsHashMap) throws Exception {
                        return formListDownloadingComplete(stringFormDetailsHashMap);
                    }
                })
                .flatMap(new Function<ArrayList<FormDetails>, Observable<Project>>() {
                    @Override
                    public Observable<Project> apply(ArrayList<FormDetails> formDetails) {
                        return downloadSingleForm(formDetails)
                                .toList()
                                .toObservable()
                                .map(lists -> project);
                    }
                });


    }

    public Observable<HashMap<FormDetails, String>> getXMLForms() {
        return checkIfProjectSitesDownloaded()
                .flatMapSingle((Function<SyncableItem, SingleSource<List<Project>>>) syncableItem -> ProjectLocalSource.getInstance().getProjectsMaybe())
                .map(mapProjectsToXMLForm())
                .flatMapIterable((Function<ArrayList<XMLForm>, Iterable<XMLForm>>) xmlForms -> xmlForms)
                .flatMap((Function<XMLForm, ObservableSource<HashMap<String, FormDetails>>>) this::downloadFormlist)
                .map(checkAndThrowDownloadError())
                .toList()
                .toObservable()
                .map(new Function<List<HashMap<String, FormDetails>>, HashMap<String, FormDetails>>() {
                    @Override
                    public HashMap<String, FormDetails> apply(List<HashMap<String, FormDetails>> hashMaps) throws Exception {
                        HashMap<String, FormDetails> result = new HashMap<>();
                        for (HashMap<String, FormDetails> hashMap : hashMaps) {
                            result.putAll(hashMap);
                        }
                        return result;

                    }
                })
                .flatMap(new Function<HashMap<String, FormDetails>, ObservableSource<ArrayList<FormDetails>>>() {
                    @Override
                    public ObservableSource<ArrayList<FormDetails>> apply(HashMap<String, FormDetails> formNamesAndURLs) throws Exception {
                        return formListDownloadingComplete(formNamesAndURLs);
                    }
                })
                .flatMap(new Function<ArrayList<FormDetails>, Observable<HashMap<FormDetails, String>>>() {
                    @Override
                    public Observable<HashMap<FormDetails, String>> apply(ArrayList<FormDetails> formDetails) throws Exception {
                        return downloadSingleForm(formDetails);
                    }
                });
    }


    private Observable<ArrayList<FormDetails>> formListDownloadingComplete(HashMap<String, FormDetails> formNamesAndURLs) {
        return Observable.fromCallable(new Callable<ArrayList<FormDetails>>() {
            @Override
            public ArrayList<FormDetails> call() throws Exception {
                return cleanDownloadedFormList(formNamesAndURLs);
            }
        });
    }

    @SafeVarargs
    private final Observable<HashMap<FormDetails, String>> downloadSingleForm(ArrayList<FormDetails>... values) {



        return Observable.fromCallable(new Callable<HashMap<FormDetails, String>>() {
            @Override
            public HashMap<FormDetails, String> call() throws Exception {
                FormDownloader formDownloader = new FormDownloader(false);
                HashMap<FormDetails, String> result = formDownloader.downloadForms(values[0]);

                for (String value : result.values()) {
                    boolean isDownloadSuccessfully =Collect.getInstance().getString(R.string.success).equals(value);
                    if(!isDownloadSuccessfully){
                        throw new RuntimeException("A form failed to download, causing downloads for the whole project to stop");
                    }
                }

                return result;
            }
        });
//        return Observable.create(new ObservableOnSubscribe<List<String>>() {
//            @Override
//            public void subscribe(ObservableEmitter<List<String>> emitter) throws Exception {
//
//                FormDownloader formDownloader = new FormDownloader(false);
//                formDownloader.setDownloaderListener(new FormDownloaderListener() {
//                    @Override
//                    public void progressUpdate(String currentFile, String progress, String total) {
//                        if (!emitter.isDisposed()) {
//                            emitter.onNext(Arrays.asList(currentFile, progress, total));
//                        }
//
//                        if (progress.equals(total)) {
//                            emitter.onComplete();
//                        }
//                    }
//
//                    @Override
//                    public boolean isTaskCanceled() {
//                        return emitter.isDisposed();
//                    }
//                });
//
//                formDownloader.downloadForms(values[0]);
//            }
//        });

    }

    @SafeVarargs
    private final Observable<HashMap<FormDetails, String>> downloadSingleFormV2(ArrayList<FormDetails>... values) {
        return Observable.fromCallable(new Callable<HashMap<FormDetails, String>>() {
            @Override
            public HashMap<FormDetails, String> call() throws Exception {
                FormDownloader formDownloader = new FormDownloader(false);
                return formDownloader.downloadForms(values[0]);
            }
        });

//        return Observable.create(new ObservableOnSubscribe<List<String>>() {
//            @Override
//            public void subscribe(ObservableEmitter<List<String>> emitter) throws Exception {
//
//
//                formDownloader.setDownloaderListener(new FormDownloaderListener() {
//                    @Override
//                    public void progressUpdate(String currentFile, String progress, String total) {
//                        if (!emitter.isDisposed()) {
//                            emitter.onNext(Arrays.asList(currentFile,progress,total));
//                        }
//
//                        if (progress.equals(total) || emitter.isDisposed()) {
//                            emitter.onComplete();
//                        }
//                    }
//
//                    @Override
//                    public boolean isTaskCanceled() {
//                        return emitter.isDisposed();
//                    }
//                });
//
//                formDownloader.downloadForms(values[0]);
//            }
//        });

    }


    @SafeVarargs
    private final Observable<HashMap<FormDetails, String>> downloadSingleFormv2(ArrayList<FormDetails>... values) {
        return Observable.fromCallable(new Callable<HashMap<FormDetails, String>>() {
            @Override
            public HashMap<FormDetails, String> call() throws Exception {

                FormDownloader formDownloader = new FormDownloader(false);
                formDownloader.setDownloaderListener(new FormDownloaderListener() {
                    @Override
                    public void progressUpdate(String currentFile, String progress, String total) {
                        Timber.i("%s %s %s", currentFile, progress, total);
                    }

                    @Override
                    public boolean isTaskCanceled() {
                        return false;
                    }
                });

                return formDownloader.downloadForms(values[0]);
            }
        });
    }


    private ArrayList<FormDetails> cleanDownloadedFormList(HashMap<String, FormDetails> formNamesAndURLs) {
        HashMap<String, FormDetails> result = new HashMap<>();
        ArrayList<HashMap<String, String>> formList = new ArrayList<>();
        result = formNamesAndURLs;
        ArrayList<HashMap<String, String>> filteredFormList = new ArrayList<>();
        String[] formIdsToDownload;
        HashMap<String, Boolean> formResult = new HashMap<>();
        ArrayList<String> formsFound = new ArrayList<>();

        ArrayList<String> ids = new ArrayList<String>(formNamesAndURLs.keySet());
        for (int i = 0; i < result.size(); i++) {
            String formDetailsKey = ids.get(i);
            FormDetails details = formNamesAndURLs.get(formDetailsKey);

            if ((details.isNewerFormVersionAvailable() || details.areNewerMediaFilesAvailable())) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(FORMNAME, details.getFormName());
                item.put(FORMID_DISPLAY,
                        ((details.getFormVersion() == null) ? "" : (Collect.getInstance().getString(R.string.version) + " "
                                + details.getFormVersion() + " ")) + "ID: " + details.getFormID());
                item.put(FORMDETAIL_KEY, formDetailsKey);
                item.put(FORM_ID_KEY, details.getFormID());
                item.put(FORM_VERSION_KEY, details.getFormVersion());

                // Insert the new form in alphabetical order.
                if (formList.isEmpty()) {
                    formList.add(item);
                } else {
                    int j;
                    for (j = 0; j < formList.size(); j++) {
                        HashMap<String, String> compareMe = formList.get(j);
                        String name = compareMe.get(FORMNAME);
                        if (name.compareTo(formNamesAndURLs.get(ids.get(i)).getFormName()) > 0) {
                            break;
                        }
                    }
                    formList.add(j, item);
                }
            }
        }


        filteredFormList.addAll(formList);

        ArrayList<FormDetails> filesToDownload = new ArrayList<>();

        for (FormDetails formDetails : formNamesAndURLs.values()) {
            String formId = formDetails.getFormID();

            formsFound.add(formId);
            filesToDownload.add(formDetails);

        }


        return filesToDownload;

    }


    private Function<HashMap<String, FormDetails>, HashMap<String, FormDetails>> checkAndThrowDownloadError() {
        return result -> {

            if (result.containsKey(DL_AUTH_REQUIRED)) {
                throw new RuntimeException("Bad token");
            } else if (result.containsKey(DL_ERROR_MSG)) {
                //todo: give better reason why it failed
                throw new RuntimeException("Download failed");
            }

            return result;
        };


    }

    private Observable<HashMap<String, FormDetails>> downloadFormlist(XMLForm xmlForm) {
        Timber.i("Downloading odk forms from %s", xmlForm.getDownloadUrl());
        return Observable.fromCallable(() -> new FieldSightFormListDownloadUtils().downloadFormList(xmlForm, false))
                .doOnNext(new Consumer<HashMap<String, FormDetails>>() {
                    @Override
                    public void accept(HashMap<String, FormDetails> result) throws Exception {
                        if (result.containsKey(DL_AUTH_REQUIRED)) {
                            throw new RuntimeException("Bad token");
                        } else if (result.containsKey(DL_ERROR_MSG)) {
                            //todo: give better reason why it failed
                            throw new RuntimeException("Download failed");
                        }
                    }
                });
    }

    private Function<List<Project>, ArrayList<XMLForm>> mapProjectsToXMLForm() {
        return projects -> {
            XMLForm xmlForm = null;
            String baseurl = FieldSightUserSession.getServerUrl(Collect.getInstance());
            ArrayList<XMLForm> formsToDownload = new ArrayList<>();

            for (Project project : projects) {
                xmlForm = new XMLFormBuilder()
                        .setFormCreatorsId(project.getId())
                        .setIsCreatedFromProject(false)
                        .setDownloadUrl(baseurl + APIEndpoint.ASSIGNED_FORM_LIST_SITE.concat(project.getId()))
                        .createXMLForm();
                formsToDownload.add(xmlForm);

                xmlForm = new XMLFormBuilder()
                        .setFormCreatorsId(project.getId())
                        .setIsCreatedFromProject(true)
                        .setDownloadUrl(baseurl + APIEndpoint.ASSIGNED_FORM_LIST_PROJECT.concat(project.getId()))
                        .createXMLForm();
                formsToDownload.add(xmlForm);
            }

            return formsToDownload;
        };
    }

    private Observable<SyncableItem> checkIfProjectSitesDownloaded() {
        int timeToWaitInSeconds = 3;
        return SyncRepository.getInstance()
                .getStatusById(PROJECT_SITES)
                .map(syncableItem -> {
                    if (syncableItem.isProgressStatus()) {
                        throw new DownloadRunningException("Waiting until project and sites are downloaded");
                    }
                    if (syncableItem.getDownloadingStatus() == Constant.DownloadStatus.PENDING) {
//                        throw new FormDownloadFailedException("Download project sites first");
                    }
                    return syncableItem;
                })
                .toObservable()
                .retryWhen(throwableObservable -> throwableObservable.flatMap((Function<Throwable, ObservableSource<?>>) throwable -> {
                    if (throwable instanceof DownloadRunningException) {
                        Timber.i("Polling for project sites");
                        return Observable.timer(timeToWaitInSeconds, TimeUnit.SECONDS);
                    }
                    return Observable.just(throwable);
                }));
    }


}
