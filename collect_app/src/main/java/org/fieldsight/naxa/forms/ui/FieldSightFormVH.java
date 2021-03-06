package org.fieldsight.naxa.forms.ui;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.bcss.collect.android.R;;
import org.fieldsight.naxa.forms.data.local.FieldsightFormDetailsv3;
import org.odk.collect.android.logic.FormDetails;

public class FieldSightFormVH extends RecyclerView.ViewHolder {
//    private final TextView tvTitle, tvSubtitle, tvIconText;

//    private final Button btnViewSubmission, btnViewEduMaterial;
    TextView tvFormName, tvLastSubmittedDate;
    ImageView ivEducationMaterials;

    protected FieldSightFormVH(@NonNull View itemView) {
        super(itemView);
//        tvTitle = itemView.findViewById(R.id.tv_form_primary);
//        tvSubtitle = itemView.findViewById(R.id.tv_form_secondary);
//        tvIconText = itemView.findViewById(R.id.form_icon_text);
//        btnViewEduMaterial = itemView.findViewById(R.id.btn_form_edu);
//        btnViewSubmission = itemView.findViewById(R.id.btn_form_responses);
        tvFormName = itemView.findViewById(R.id.tv_form_name);
        tvLastSubmittedDate = itemView.findViewById(R.id.tv_last_submitted);
        ivEducationMaterials = itemView.findViewById(R.id.iv_education_materials);
    }

    public void bindView(FieldsightFormDetailsv3 form) {
        FormDetails formDetails = form.getFormDetails();

        try{
            tvFormName.setText(formDetails.getFormName());
            tvLastSubmittedDate.setText(formDetails.getFormName());
            itemView.setOnClickListener(view -> openForm(form));
            ivEducationMaterials.setOnClickListener(view -> openEducationalMaterial(form));
        }catch (Exception e){

        }


    }

    public void openForm(FieldsightFormDetailsv3 form) {

    }


    public void openEducationalMaterial(FieldsightFormDetailsv3 form) {

    }


    public void openPreviousSubmission(FieldsightFormDetailsv3 form) {

    }


}
