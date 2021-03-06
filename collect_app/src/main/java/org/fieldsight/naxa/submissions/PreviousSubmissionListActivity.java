package org.fieldsight.naxa.submissions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.bcss.collect.android.R;;
import org.fieldsight.naxa.common.Constant;
import org.fieldsight.naxa.common.FieldSightUserSession;
import org.fieldsight.naxa.common.PaginationScrollListener;
import org.fieldsight.naxa.generalforms.data.FormResponse;
import org.fieldsight.naxa.network.ApiInterface;
import org.fieldsight.naxa.network.ServiceGenerator;
import org.odk.collect.android.activities.CollectAbstractActivity;
import org.odk.collect.android.utilities.ToastUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.fieldsight.naxa.common.Constant.EXTRA_OBJECT;

public class PreviousSubmissionListActivity extends CollectAbstractActivity implements PaginationAdapter.OnCardClickListener {

    ActionBar actionBar;
    private String fsFormId;
    private String fsFormName;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView listFormHistory;
    private PaginationAdapter adapter;

    ProgressBar progressBar;

    private boolean isLoading ;

    private boolean isLastPage ;

    private String urlFirstPage;
    private String urlNextPage;
    private Toolbar toolbar;
    private CardView cardSubmissionInfo;
    private TextView tvTotalSubmissionMessage;
    private TextView tvListTitle;

    private String tableName;

    FormResponse offlineLatestResponse;


    public static void start(Context context, String fsFormId, String formName, String fsFormRecordName, FormResponse formResponse, String siteId, String respounseCount, String tableName) {
        Intent intent = new Intent(context, PreviousSubmissionListActivity.class);
        intent.putExtra(Constant.BundleKey.KEY_FS_FORM_ID, fsFormId);
        intent.putExtra(Constant.BundleKey.KEY_TABLE_NAME, tableName);
        intent.putExtra(Constant.BundleKey.KEY_FS_FORM_NAME, formName);
        intent.putExtra(Constant.BundleKey.KEY_FS_FORM_RECORD_NAME, fsFormRecordName);
        intent.putExtra(Constant.BundleKey.KEY_SITE_ID, siteId);
        intent.putExtra("count", respounseCount);
        intent.putExtra(EXTRA_OBJECT, formResponse);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_history_list);
        Bundle bundle = getIntent().getExtras();
        fsFormId = bundle.getString(Constant.BundleKey.KEY_FS_FORM_ID);
        fsFormName = bundle.getString(Constant.BundleKey.KEY_FS_FORM_NAME);

        String siteId = bundle.getString(Constant.BundleKey.KEY_SITE_ID);
        tableName = bundle.getString(Constant.BundleKey.KEY_TABLE_NAME);

        offlineLatestResponse = null;
        urlFirstPage = FieldSightUserSession.getServerUrl(this) + "/forms/api/responses/" + fsFormId + "/" + siteId;
        Timber.i(urlFirstPage);
        String count = bundle.getString("count");

        bindUI();
        setupRecyclerView();
        setupToolbar();

        String totalSubmissionMsg;

        if (offlineLatestResponse == null || count == null) {
            totalSubmissionMsg = getString(R.string.msg_no_form_submission);
        } else {
            totalSubmissionMsg = getResources()
                    .getQuantityString(R.plurals.msg_total_submission_info_offline, Integer.parseInt(count), count);
            adapter.add(offlineLatestResponse);
        }

        progressBar.setVisibility(View.GONE);
        cardSubmissionInfo.setVisibility(View.VISIBLE);
        tvListTitle.setVisibility(View.VISIBLE);
        tvTotalSubmissionMessage.setText(totalSubmissionMsg);

        findViewById(R.id.btn_load_prev_submissions)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setupPagination(listFormHistory);
                        cardSubmissionInfo.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        lazyLoadFirstPage(TimeUnit.SECONDS.toMillis(1));
                    }
                });

        setupPagination(listFormHistory);
        cardSubmissionInfo.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        loadFirstPage(urlFirstPage);
    }

    private void lazyLoadFirstPage(long millis) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadFirstPage(urlFirstPage);
            }
        }, millis);
    }

    private void loadFirstPage(String url) {
        ServiceGenerator.createCacheService(ApiInterface.class)
                .getFormHistory(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FormHistoryResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(FormHistoryResponse response) {
                        progressBar.setVisibility(View.GONE);
                        cardSubmissionInfo.setVisibility(View.VISIBLE);

                        tvTotalSubmissionMessage.setText(getString(R.string.msg_no_form_submission));

                        if (response == null) {
                            tvTotalSubmissionMessage.setText(getString(R.string.msg_no_form_submission));
                            return;

                        }

                        if (response.getResults().size() <= 0) {

                            tvTotalSubmissionMessage.setText(getString(R.string.msg_no_form_submission));
                            return;
                        }

                        String totalSubmissionMsg = getResources()
                                .getQuantityString(R.plurals.msg_total_submission_info, response.getCount(), response.getCount());
                        tvTotalSubmissionMessage.setText(totalSubmissionMsg);

                        adapter.clear();
                        adapter.addAll(response.getResults());
                        tvListTitle.setVisibility(View.VISIBLE);
                        long updated = 0L;


                        Timber.i("Saving form response for FormID %s of type %s query %s", fsFormId, tableName, updated);

                        if (response.getNext() == null) {
                            isLastPage = true;
                        } else {
                            urlNextPage = response.getNext();
                            adapter.addLoadingFooter();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        ToastUtils.showLongToast(e.getMessage());
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    private void setupToolbar() {
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreviousSubmissionListActivity.super.onBackPressed();
            }
        });

        if (actionBar != null) {
            String msg = getString(R.string.toolbar_prev_submission_list);
            actionBar.setTitle(msg);
            actionBar.setSubtitle(fsFormName);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

    }

    private void setupRecyclerView() {
        adapter = new PaginationAdapter(this);
        adapter.setCardClickListener(this);
        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        listFormHistory.setLayoutManager(linearLayoutManager);
        listFormHistory.setAdapter(adapter);
        listFormHistory.setItemAnimator(new DefaultItemAnimator());
        listFormHistory.setNestedScrollingEnabled(false);
    }



    private void setupPagination(final RecyclerView rv) {

        rv.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                loadNextPage();
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }


        });
    }

    private void loadNextPage() {
        ServiceGenerator.createService(ApiInterface.class)
                .getFormHistory(urlNextPage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FormHistoryResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(FormHistoryResponse response) {
                        adapter.removeLoadingFooter();
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        adapter.addAll(response.getResults());
                        if (response.getNext() == null) {
                            isLastPage = true;
                        } else {
                            urlNextPage = response.getNext();
                            adapter.addLoadingFooter();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        ToastUtils.showLongToast(e.getMessage());
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void bindUI() {
        toolbar = findViewById(R.id.toolbar);
        listFormHistory = findViewById(R.id.recycler_form_history_list);
        progressBar = findViewById(R.id.main_progress);
        cardSubmissionInfo = findViewById(R.id.card_info);
        tvTotalSubmissionMessage = findViewById(R.id.tv_total_submission_message);
        tvListTitle = findViewById(R.id.tv_list_title);

    }

    @Override
    public void onFormClicked(FormResponse form, View view) {
        Intent toFormDetail = new Intent(this, PreviousSubmissionDetailActivity.class);
        toFormDetail.putExtra(EXTRA_OBJECT, form);
        startActivity(toFormDetail);


    }



}
