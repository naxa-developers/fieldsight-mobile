package org.fieldsight.naxa.common;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;

import org.fieldsight.collect.android.R;
import org.fieldsight.naxa.common.view.BaseRecyclerViewAdapter;
import org.fieldsight.naxa.forms.source.local.FieldSightForm;
import org.fieldsight.naxa.forms.ui.FieldSightFormVH;
import org.fieldsight.naxa.forms.viewmodel.FieldSightFormViewModel;
import org.fieldsight.naxa.helpers.FSInstancesDao;
import org.fieldsight.naxa.stages.data.SubStage;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.provider.FormsProviderAPI;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

public class BaseFormListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FieldSightFormViewModel viewModel;
    private BaseRecyclerViewAdapter<FieldSightForm, FieldSightFormVH> adapter;
    private String siteId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = obtainViewModel(requireActivity());
    }

    protected FieldSightFormViewModel getViewModel() {
        return viewModel;
    }

    protected void fillODKForm(String idString) {
        try {
            long formId = getFormId(idString);
            Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, formId);
            String action = getActivity().getIntent().getAction();

            if (Intent.ACTION_PICK.equals(action)) {
                // caller is waiting on a picked form
                getActivity().setResult(RESULT_OK, new Intent().setData(formUri));
            } else {
                // caller wants to view/edit a form, so launch formentryactivity
                Intent toFormEntry = new Intent(Intent.ACTION_EDIT, formUri);
                toFormEntry.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(toFormEntry);
            }

        } catch (CursorIndexOutOfBoundsException e) {
            DialogFactory.createGenericErrorDialog(getActivity(), getString(R.string.msg_form_not_present)).show();
            Timber.e("Failed to load xml form  %s", e.getMessage());
        } catch (NullPointerException | NumberFormatException e) {
            Timber.e(e);
            DialogFactory.createGenericErrorDialog(getActivity(), e.getMessage()).show();
            Timber.e("Failed to load xml form %s", e.getMessage());
        }
    }

    protected String generateSubmissionUrl(String formDeployedFrom, String creatorsId, String fsFormId) {
        return FSInstancesDao.generateSubmissionUrl(formDeployedFrom, creatorsId, fsFormId);
    }

    private long getFormId(String jrFormId) throws CursorIndexOutOfBoundsException, NullPointerException, NumberFormatException {

        String[] projection = new String[]{FormsProviderAPI.FormsColumns._ID, FormsProviderAPI.FormsColumns.FORM_FILE_PATH};
        String selection = FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? AND " + "(" + FormsProviderAPI.FormsColumns.IS_TEMP_DOWNLOAD + " =? OR " + FormsProviderAPI.FormsColumns.IS_TEMP_DOWNLOAD + " IS NULL)";
        String[] selectionArgs = new String[]{jrFormId, "0"};
        String sortOrder = FormsProviderAPI.FormsColumns._ID + " DESC LIMIT 1";

        Cursor cursor = requireActivity().getContentResolver().query(FormsProviderAPI.FormsColumns.CONTENT_URI,
                projection,
                selection, selectionArgs, sortOrder);

        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(FormsProviderAPI.FormsColumns._ID);
        long formId = Long.parseLong(cursor.getString(columnIndex));

        cursor.close();

        return formId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fieldsight_forms_list_fragment, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_fieldsight_form_list);

        return view;
    }

    protected void setToolbarText(String title, String subtitle) {
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        toolbar.setSubtitle(subtitle);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupListAdapter();
    }

    private void setupListAdapter() {
        LinearLayoutManager manager = new LinearLayoutManager(requireActivity());
        adapter = new BaseRecyclerViewAdapter<FieldSightForm, FieldSightFormVH>(new ArrayList<>(), R.layout.list_item_fieldsight_form) {
            @Override
            public void viewBinded(FieldSightFormVH activityVH, FieldSightForm activity) {
                activityVH.bindView(activity);
            }

            @Override
            public FieldSightFormVH attachViewHolder(View view) {
                return new FieldSightFormVH(view) {
                    @Override
                    public void openForm(FieldSightForm form) {
                        if (TextUtils.equals(form.getFormType(), Constant.FormType.STAGED)) {
                            prepareSubStage( form)
                                    .subscribe(onSubStateSubscribe());
                        } else {
                            cacheFormAndSite(form, siteId);
                            fillODKForm(form.getOdkFormID());
                        }
                    }
                };
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(manager);
    }

    private DisposableObserver<ArrayList<FieldSightForm>> onSubStateSubscribe() {
        return new DisposableObserver<ArrayList<FieldSightForm>>() {
            @Override
            public void onNext(ArrayList<FieldSightForm> fieldSightForms) {
                updateList(fieldSightForms, siteId);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
    }


    private Observable<ArrayList<FieldSightForm>> prepareSubStage(FieldSightForm form) {
        return Observable.fromCallable(new Callable<ArrayList<FieldSightForm>>() {
            @Override
            public ArrayList<FieldSightForm> call() throws Exception {
                Type listType = new TypeToken<List<SubStage>>() {
                }.getType();
                ArrayList<SubStage> subStages = GSONInstance.getInstance().fromJson(form.getMetadata(), listType);
                ArrayList<FieldSightForm> fieldSightForms = new ArrayList<>();
                FieldSightForm fieldSightForm;
                for (SubStage subStage : subStages) {
                    fieldSightForm = new FieldSightForm();
                    fieldSightForm.setFormName(subStage.getName());
                    fieldSightForm.setSiteId(form.getSiteId());
                    fieldSightForm.setProjectId(form.getProjectId());

                    fieldSightForms.add(fieldSightForm);
                }


                return fieldSightForms;
            }
        });
    }

    private void cacheFormAndSite(FieldSightForm form, String siteId) {
        String formDeployedFrom = form.getFormDeployedProjectId() == null ? Constant.FormDeploymentFrom.SITE : Constant.FormDeploymentFrom.PROJECT;
        String submissionUrl = generateSubmissionUrl(formDeployedFrom, siteId, form.getFieldSightFormId());
        SharedPreferenceUtils.saveToPrefs(Collect.getInstance().getApplicationContext(), SharedPreferenceUtils.PREF_VALUE_KEY.KEY_URL, submissionUrl);
        SharedPreferenceUtils.saveToPrefs(Collect.getInstance().getApplicationContext(), SharedPreferenceUtils.PREF_VALUE_KEY.KEY_SITE_ID, siteId);
    }


    private FieldSightFormViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        return ViewModelProviders.of(activity, factory).get(FieldSightFormViewModel.class);
    }

    public void updateList(List<FieldSightForm> fieldSightForms, String siteId) {
        this.siteId = siteId;

        adapter.getData().clear();
        adapter.getData().addAll(fieldSightForms);
        adapter.notifyDataSetChanged();

    }


}
