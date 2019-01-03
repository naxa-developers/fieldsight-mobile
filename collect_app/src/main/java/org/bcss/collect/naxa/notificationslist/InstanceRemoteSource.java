package org.bcss.collect.naxa.notificationslist;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import org.bcss.collect.android.application.Collect;
import org.bcss.collect.android.dao.InstancesDao;
import org.bcss.collect.android.dto.Instance;
import org.bcss.collect.android.provider.InstanceProviderAPI;
import org.bcss.collect.naxa.common.RxDownloader.RxDownloader;
import org.bcss.collect.naxa.data.FieldSightNotification;
import org.bcss.collect.naxa.network.APIEndpoint;
import org.bcss.collect.naxa.network.ApiInterface;
import org.bcss.collect.naxa.network.ServiceGenerator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.bcss.collect.naxa.common.Constant.FormDeploymentFrom.PROJECT;
import static org.bcss.collect.naxa.common.Constant.FormDeploymentFrom.SITE;

public class InstanceRemoteSource {
    private static InstanceRemoteSource INSTANCE;


    public static InstanceRemoteSource getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new InstanceRemoteSource();
        }
        return INSTANCE;
    }


    Observable<Uri> downloadInstances(FieldSightNotification fieldSightNotification) {
        String downloadUrl = String.format(APIEndpoint.BASE_URL + "/forms/api/instance/download_submission/%s", fieldSightNotification.getFormSubmissionId());
        return downloadInstance(mapNotificationToInstance(fieldSightNotification), downloadUrl);
    }

    Observable<HashMap<String, String>> downloadAttachedMedia(FieldSightNotification loadedFieldSightNotification) {


       return ServiceGenerator.getRxClient()
                .create(ApiInterface.class)
                .getInstanceMediaList(loadedFieldSightNotification.getFormSubmissionId())
               .subscribeOn(Schedulers.io());

    }


    private Observable<Uri> downloadInstance(Instance.Builder instance, String downloadUrl) {

        return Observable.just(instance)
                .flatMap((Function<Instance.Builder, ObservableSource<Uri>>) instance1 -> {
                    String instanceName = instance1.build().getDisplayName();
                    String formattedInstanceName = addDateTimeToFileName(instanceName);
                    formattedInstanceName = formatFileName(formattedInstanceName);
                    //todo: value returned from getExternalStorageDiectory is twice, so removing one
                    String pathToDownload = Collect.INSTANCES_PATH.replace(Environment.getExternalStorageDirectory().toString(), "");

                    pathToDownload = pathToDownload + File.separator + formattedInstanceName;
                    pathToDownload = formatFileName(pathToDownload);

                    return new RxDownloader(Collect.getInstance())
                            .download(downloadUrl,
                                    formattedInstanceName.concat(".xml"),
                                    pathToDownload,
                                    "*/*",
                                    false)
                            .map(processOneInstance(instance1));
                });
    }

    private Function<String, Uri> processOneInstance(Instance.Builder instance) {
        return path -> {
            path = path.replace("file://", "");
            instance.instanceFilePath(path);
            return saveInstance(instance.build());
        };
    }

    private Uri saveInstance(Instance instance) {
        InstancesDao dao = new InstancesDao();
        ContentValues values = dao.getValuesFromInstanceObject(instance);
        Uri instanceUri = dao.saveInstance(values);
        Timber.i("Downloaded and saved instance at %s", instanceUri);
        return instanceUri;

    }


    private Instance.Builder mapNotificationToInstance(FieldSightNotification notificationFormDetail) {
        String siteId = notificationFormDetail.getSiteId() == null ? "0" : notificationFormDetail.getSiteId();//survey form have 0 as siteId
        String fsFormIdProject = notificationFormDetail.getFsFormIdProject();
        String formName = notificationFormDetail.getFormName();
        String jrFormId = notificationFormDetail.getIdString();
        String fsFormId = notificationFormDetail.getFsFormId();
        String formVersion = notificationFormDetail.getFormVersion();
        String url = InstancesDao.generateSubmissionUrl(
                fsFormIdProject != null ? PROJECT : SITE,
                siteId, fsFormId);


        return new Instance.Builder()
                .status(InstanceProviderAPI.STATUS_COMPLETE)
                .jrFormId(jrFormId)
                .jrVersion(formVersion)
                .submissionUri(url)
                .fieldSightSiteId(siteId)
                .displayName(formName)
                .canEditWhenComplete("true")
                .lastStatusChangeDate(System.currentTimeMillis())
                .displaySubtext("");
    }

    private String formatFileName(String text) {
        return text.replace(" ", "_");
    }

    private String getColumnString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }


    private String getInstanceFolderPath(String formName) {

        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        String instancePath = Collect.INSTANCES_PATH.replace(Environment.getExternalStorageDirectory().toString(), "");
        return instancePath + File.separator + formName + "_" + time;
    }

    private String addDateTimeToFileName(String formName) {
        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        return formName + "_" + time;
    }


}
