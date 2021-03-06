package org.fieldsight.naxa.generalforms;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.bcss.collect.android.R;;
import org.odk.collect.android.application.Collect;
import org.fieldsight.naxa.common.Constant;
import org.fieldsight.naxa.common.OnFormItemClickListener;
import org.fieldsight.naxa.generalforms.data.GeneralForm;
import org.fieldsight.naxa.previoussubmission.model.GeneralFormAndSubmission;
import org.fieldsight.naxa.previoussubmission.model.SubmissionDetail;
import org.odk.collect.android.utilities.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.fieldsight.naxa.common.AnimationUtils.getRotationAnimation;

public class GeneralFormsAdapter extends RecyclerView.Adapter<GeneralFormsAdapter.ViewHolder> {

    private final ArrayList<GeneralFormAndSubmission> generalForms;
    private final OnFormItemClickListener<GeneralForm> listener;

    GeneralFormsAdapter(ArrayList<GeneralFormAndSubmission> totalList, OnFormItemClickListener<GeneralForm> listener) {
        this.generalForms = totalList;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void updateList(List<GeneralFormAndSubmission> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new GeneralFormsDiffCallback(newList, generalForms));
        generalForms.clear();
        generalForms.addAll(newList);
        diffResult.dispatchUpdatesTo(this);

        if (newList.isEmpty()) {
            //triggers observer so it display empty layout - nishon
            this.notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.form_list_item_expanded, null, true);
        return new ViewHolder(view);


    }

    @Override
    public long getItemId(int position) {
        GeneralForm generalForm = generalForms.get(position).getGeneralForm();
        return Long.parseLong(generalForm.getFsFormId());//fsFormId is always a number
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        GeneralForm generalForm = generalForms.get(viewHolder.getAdapterPosition()).getGeneralForm();
        SubmissionDetail submissionDetail = generalForms.get(viewHolder.getAdapterPosition()).getSubmissionDetail();

        viewHolder.tvFormName.setText(generalForm.getName());

        String relativeDateTime = DateTimeUtils.getRelativeTime(generalForm.getDateCreated(), true);
        viewHolder.tvDesc.setText(viewHolder.tvFormName.getContext().getString(R.string.form_created_on, relativeDateTime));
        if (generalForm.getName() != null) {
            viewHolder.tvIconText.setText(generalForm.getName().substring(0, 1).toUpperCase(Locale.getDefault()));
        }
        setSubmissionText(viewHolder, submissionDetail, generalForm);
    }

    private void setSubmissionText(ViewHolder viewHolder, SubmissionDetail submissionDetail, GeneralForm generalForm) {

        String submissionDateTime = "";
        String submittedBy = "";
        String submissionStatus = "";
        String formCreatedOn = "";
        Context context = viewHolder.cardView.getContext();

        if (submissionDetail != null) {
            submittedBy = submissionDetail.getSubmittedBy();
            submissionStatus = submissionDetail.getStatusDisplay();
            submissionDateTime = DateTimeUtils.getRelativeTime(submissionDetail.getSubmissionDateTime(), true);
        }

        if (submissionDetail != null && submissionDetail.getSubmissionDateTime() == null) {
            submissionDateTime = context.getString(R.string.form_pending_submission);
        } else {
            submissionDateTime = context.getString(R.string.form_last_submission_datetime, submissionDateTime);
        }

        if (generalForm.getDateCreated() != null) {
            formCreatedOn = DateTimeUtils.getRelativeTime(generalForm.getDateCreated(), true);
        }

        String formSubtext = generateSubtext(context, submittedBy, submissionStatus, formCreatedOn);

        viewHolder.ivCardCircle.setImageDrawable(getCircleDrawableBackground(submissionStatus));
        viewHolder.tvDesc.setText(submissionDateTime);
        viewHolder.tvSubtext.setText(formSubtext);
    }

    private String generateSubtext(Context context, String submittedBy, String submissionStatus, String formCreatedOn) {
        return context.getString(R.string.form_last_submitted_by, submittedBy == null ? "" : submittedBy)
                + "\n" +
                context.getString(R.string.form_last_submission_status, submissionStatus == null ? "" : submissionStatus)
                + "\n" +
                context.getString(R.string.form_created_on, formCreatedOn == null ? "" : formCreatedOn);
    }


    private Drawable getCircleDrawableBackground(String status) {

        Drawable drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_blue);

        if (status == null) {
            return drawable;

        }
        switch (status) {
            case Constant.FormStatus.APPROVED:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_green);
                break;
            case Constant.FormStatus.FLAGGED:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_yellow);
                break;
            case Constant.FormStatus.REJECTED:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_red);
                break;
            case Constant.FormStatus.PENDING:
            default:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_blue);
                break;
        }

        return drawable;
    }


    @Override
    public int getItemCount() {
        return generalForms.size();
    }

    public ArrayList<GeneralFormAndSubmission> getAll() {
        return generalForms;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView tvFormName, tvDesc, tvIconText, tvSubtext;
        private final Button btnOpenEdu, btnOpenHistory;
        private final ImageView ivCardCircle;
        private final CardView cardView;
        private final ImageButton btnExpandCard;


        public ViewHolder(View view) {
            super(view);

            cardView = view.findViewById(R.id.card_view_form_list_item);

            tvFormName = view.findViewById(R.id.tv_form_primary);
            tvDesc = view.findViewById(R.id.tv_form_secondary);
            tvIconText = view.findViewById(R.id.form_icon_text);
            tvSubtext = view.findViewById(R.id.tv_form_subtext);

            ivCardCircle = view.findViewById(R.id.iv_form_circle);

            btnOpenHistory = view.findViewById(R.id.btn_form_responses);
            btnOpenEdu = view.findViewById(R.id.btn_form_edu);
            btnExpandCard = view.findViewById(R.id.btn_expand_card);

            cardView.setOnClickListener(this);
            btnOpenEdu.setOnClickListener(this);
            btnOpenHistory.setOnClickListener(this);
            btnExpandCard.setOnClickListener(this);

        }


        @Override
        public void onClick(View v) {
            GeneralForm generalForm = generalForms.get(getAdapterPosition()).getGeneralForm();

            switch (v.getId()) {
                case R.id.btn_form_edu:
                    listener.onGuideBookButtonClicked(generalForm, getAdapterPosition());
                    break;
                case R.id.btn_form_responses:
                    listener.onFormHistoryButtonClicked(generalForm);
                    break;
                case R.id.card_view_form_list_item:
                    listener.onFormItemClicked(generalForm, getAdapterPosition());
                    break;
                case R.id.btn_expand_card:

                    boolean isCollapsed = tvSubtext.getVisibility() == View.GONE;
                    if (isCollapsed) {
                        btnExpandCard.startAnimation(getRotationAnimation(180, 0));
                        btnExpandCard.setRotation(180);
                    } else {
                        btnExpandCard.startAnimation(getRotationAnimation(180, 360));
                        btnExpandCard.setRotation(360);
                    }

                    tvSubtext.setVisibility(tvSubtext.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

                    break;
            }
        }
    }


}
