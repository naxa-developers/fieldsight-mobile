package org.fieldsight.naxa.forms.data.local;

import android.text.TextUtils;
import android.util.SparseArray;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import org.fieldsight.naxa.common.BaseLocalDataSourceRX;
import org.fieldsight.naxa.common.Constant;
import org.fieldsight.naxa.common.FieldSightDatabase;
import org.fieldsight.naxa.stages.data.Stage;
import org.fieldsight.naxa.stages.data.SubStage;
import org.odk.collect.android.application.Collect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import timber.log.Timber;

public class FieldSightFormsLocalSourcev3 implements BaseLocalDataSourceRX<FieldsightFormDetailsv3> {

    private static FieldSightFormsLocalSourcev3 localSourcev3;
    private final FieldSightFormDetailDAOV3 dao;

    private FieldSightFormsLocalSourcev3() {
        FieldSightDatabase database = FieldSightDatabase.getDatabase(Collect.getInstance());//todo inject context
        this.dao = database.getFieldSightFOrmDAOV3();
    }

    public static FieldSightFormsLocalSourcev3 getInstance() {
        if (localSourcev3 == null) {
            localSourcev3 = new FieldSightFormsLocalSourcev3();
        }
        return localSourcev3;
    }

    public LiveData<List<FieldsightFormDetailsv3>> getFormByType(String formType, String projectId, String siteId, String siteTypeId) {

        MediatorLiveData<List<FieldsightFormDetailsv3>> mediator = new MediatorLiveData<>();
        LiveData<List<FieldsightFormDetailsv3>> formSource = dao.getFormByType(formType, projectId, siteId);

        mediator.addSource(formSource, forms -> {
            if (TextUtils.equals(formType, Constant.FormType.STAGED)) {
                getSortedStages(forms, siteTypeId)
                        .subscribe(new SingleObserver<List<FieldsightFormDetailsv3>>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(List<FieldsightFormDetailsv3> sortedStages) {
                                mediator.removeSource(formSource);
                                mediator.setValue(sortedStages);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Timber.e(e);
                                mediator.removeSource(formSource);
                                mediator.setValue(new ArrayList<>(0));

                            }
                        });
            } else {
                mediator.removeSource(formSource);
                mediator.setValue(forms);
            }
        });

        return mediator;
    }


    public MediatorLiveData<List<Stage>> getStageForms(String projectId, String siteId, String siteTypeId) {

        MediatorLiveData<List<Stage>> mediator = new MediatorLiveData<>();
        LiveData<List<FieldsightFormDetailsv3>> formSource = dao.getFormByType(Constant.FormType.STAGED, projectId, siteId);

        mediator.addSource(formSource, forms -> {
            getStageAndSubStages(forms, siteTypeId)
                    .subscribe(new SingleObserver<List<Stage>>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(List<Stage> stages) {
                            mediator.removeSource(formSource);
                            mediator.setValue(stages);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e);
                        }
                    });

        });

        return mediator;
    }


    public Single<List<Stage>> getStageAndSubStages(List<FieldsightFormDetailsv3> forms, String siteTypeId) {
        return getSortedStages(forms, siteTypeId)
                .map(new Function<List<FieldsightFormDetailsv3>, List<Stage>>() {
                    @Override
                    public List<Stage> apply(List<FieldsightFormDetailsv3> formDetailsv3s) throws Exception {

                        List<Integer> stagesId = new ArrayList<>();
                        List<Stage> stages = new ArrayList<>();
                        Stage stage;

                        SubStage subStage = null;
                        SparseArray<List<SubStage>> groupByStage = new SparseArray<>();

                        for (FieldsightFormDetailsv3 form : formDetailsv3s) {
                            StageSubStage stageAndSubStage = FieldsightFormDetailsv3.getStageAndSubstage(form.getMetaAttributes());

                            stage = new Stage();
                            stage.setDescription(stageAndSubStage.getStageDescription());
                            stage.setName(stageAndSubStage.getStageName());
                            stage.setOrder(Integer.valueOf(stageAndSubStage.getStageOrder()));


                            if(!stagesId.contains(stage.getOrder())){
                                stages.add(stage);
                                stagesId.add(stage.getOrder());
                            }


                            subStage = new SubStage();
                            subStage.setName(stageAndSubStage.getSubstageName());
                            subStage.setDescription(stageAndSubStage.getSubstageDescription());
                            subStage.setJrFormId(form.getFormDetails().getFormID());
                            String formDeployedFrom = form.getProject() == null ? Constant.FormDeploymentFrom.SITE : Constant.FormDeploymentFrom.PROJECT;
                            subStage.setFormDeployedFrom(formDeployedFrom);
                            subStage.setFsFormId(form.getId());

                            // Group subStage
                            int stageOrder = Integer.parseInt(stageAndSubStage.getStageOrder());
                            if (groupByStage.get(stageOrder) == null) {
                                List<SubStage> list = new ArrayList<>();
                                list.add(subStage);
                                groupByStage.put(stageOrder, list);
                            } else {
                                groupByStage.get(stageOrder).add(subStage);
                            }
                        }

                        // add subStage to stages
                        for (Stage stage1 : stages) {
                            Integer stageOrder = stage1.getOrder();
                            stage1.setSubStage((ArrayList<SubStage>) groupByStage.get(stageOrder));
                        }


                        return new ArrayList<>(stages);
                    }
                });
    }

    private Single<List<FieldsightFormDetailsv3>> getSortedStages
            (List<FieldsightFormDetailsv3> forms, String siteTypeId) {
        return Observable.just(forms)
                .map(fieldsightFormDetailsv3s -> {
                    Collections.shuffle(fieldsightFormDetailsv3s);
                    return fieldsightFormDetailsv3s;
                })
                .flatMapIterable((Function<List<FieldsightFormDetailsv3>, Iterable<FieldsightFormDetailsv3>>) formDetailsv3s -> formDetailsv3s)
                .filter(new Predicate<FieldsightFormDetailsv3>() {
                    @Override
                    public boolean test(FieldsightFormDetailsv3 formDetailsv3) {
                        StageSubStage stage = FieldsightFormDetailsv3.getStageAndSubstage(formDetailsv3.getMetaAttributes());

                        return TextUtils.isEmpty(siteTypeId)
                                || Constant.DEFAULT_SITE_TYPE.equals(siteTypeId)
                                || stage.getSubstageTags().contains(siteTypeId)
                                || stage.getSubstageTags().size() == 0;

                    }
                })
                .toList()
                .map(new Function<List<FieldsightFormDetailsv3>, List<FieldsightFormDetailsv3>>() {
                    @Override
                    public List<FieldsightFormDetailsv3> apply(List<FieldsightFormDetailsv3> formDetailsv3) {
                        Collections.sort(formDetailsv3, new Comparator<FieldsightFormDetailsv3>() {
                            @Override
                            public int compare(FieldsightFormDetailsv3 t1, FieldsightFormDetailsv3 t2) {
                                StageSubStage stage1 = FieldsightFormDetailsv3.getStageAndSubstage(t1.getMetaAttributes());
                                StageSubStage stage2 = FieldsightFormDetailsv3.getStageAndSubstage(t2.getMetaAttributes());

                                Double stageOneOrder = Double.parseDouble(stage1.getStageOrder() + "." + stage1.getSubstageOrder());
                                Double stageTwoOrder = Double.parseDouble(stage2.getStageOrder() + "." + stage2.getSubstageOrder());

                                return stageOneOrder.compareTo(stageTwoOrder);
                            }
                        });


                        return formDetailsv3;
                    }
                });
    }

    public void saveForms(FieldsightFormDetailsv3... fieldSightForm) {
        dao.insert(fieldSightForm);
    }

    @Override
    public LiveData<List<FieldsightFormDetailsv3>> getAll() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Completable save(FieldsightFormDetailsv3... items) {
        return Completable.fromAction(() -> dao.insert(items));
    }

    @Override
    public void save(ArrayList<FieldsightFormDetailsv3> items) {
        dao.insert(items);
    }


    @Override
    public void updateAll(ArrayList<FieldsightFormDetailsv3> items) {

    }

    public List<FieldsightFormDetailsv3> getEducationMaterial(String projectId) {
        return dao.getEducationMaterailByProjectIds(projectId);
    }

    public FieldsightFormDetailsv3 getByFsFormId(String fsFormId) {
        return dao.getByFsFormId(fsFormId);
    }
}