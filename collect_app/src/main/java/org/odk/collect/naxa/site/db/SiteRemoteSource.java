package org.odk.collect.naxa.site.db;

import android.arch.lifecycle.MediatorLiveData;

import org.odk.collect.naxa.common.BaseRemoteDataSource;
import org.odk.collect.naxa.common.data.Resource;
import org.odk.collect.naxa.login.model.Site;
import org.odk.collect.naxa.network.APIEndpoint;
import org.odk.collect.naxa.network.ApiInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static org.odk.collect.naxa.common.Constant.SiteStatus.IS_UNVERIFIED_SITE;
import static org.odk.collect.naxa.network.ServiceGenerator.*;

public class SiteRemoteSource implements BaseRemoteDataSource<Site> {

    private static SiteRemoteSource INSTANCE;
    private SiteDao dao;


    public static SiteRemoteSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SiteRemoteSource();
        }
        return INSTANCE;
    }


    @Override
    public void getAll() {

    }

    public void create(List<Site> sites) {

        MediatorLiveData<Resource<Site>> result = new MediatorLiveData<>();

        Observable.just(sites)
                .flatMapIterable((Function<List<Site>, Iterable<Site>>) sites1 -> sites1)
                .filter(site -> site.getIsSiteVerified() == IS_UNVERIFIED_SITE)
                .flatMap((Function<Site, ObservableSource<Site>>) this::uploadSite)
                .map((Function<Site, Site>) site -> {
                    String uploadError = site.getSiteTypeError();

                    if ("identifier".contains(uploadError)) {
                        throw new RuntimeException("Bad identifier");
                    }

                    if ("Invalid pk".contains(uploadError)) {
                        throw new RuntimeException("Invalid pk");
                    }

                    return site;
                })
                .map(new Function<Site, Site>() {
                    @Override
                    public Site apply(Site site) throws Exception {
                        return null;
                    }
                });
//                .subscribe(new SingleObserver<List<Site>>() {
//                    @Override
//                    public void onSubscribe(Disposable d) {
//                        result.setValue(Resource.loading(null));
//                    }
//
//                    @Override
//                    public void onSuccess(List<Site> sites) {
//                        result.setValue(Resource.success(null));
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        result.setValue(Resource.error(e.getMessage(), null));
//                    }
//                });


    }

    private Observable<Site> uploadSite(Site siteLocationPojo) {
        String imagePath = siteLocationPojo.getLogo();
        final File ImageFile = new File(imagePath);
        RequestBody imageRequestBody = null;
        String imageName = "";
        MultipartBody.Part body = null;

        RequestBody SiteNameRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getName());
        RequestBody latRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(siteLocationPojo.getLatitude()));
        RequestBody lonRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(siteLocationPojo.getLongitude()));
        RequestBody identifierRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getIdentifier());
        RequestBody SitePhoneRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getPhone());
        RequestBody SiteAddressRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getAddress());
        RequestBody SitePublicDescRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getPublicDesc());
        RequestBody projectIdRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getProject());
        RequestBody SiteRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(siteLocationPojo.getTypeId()));
        RequestBody isSurvey = RequestBody.create(MediaType.parse("text/plain"), "false");
//        RequestBody metaAttrs = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getMetaAttrs());
//        RequestBody regionId = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getSiteCluster());

        return getRxClient()
                .create(ApiInterface.class)
                .uploadSite(APIEndpoint.ADD_SITE_URL, body, isSurvey
                        , SiteNameRequest, latRequest, lonRequest, identifierRequest, SitePhoneRequest,
                        SiteAddressRequest, SitePublicDescRequest, projectIdRequest, SiteRequest, null, null);
    }
}
