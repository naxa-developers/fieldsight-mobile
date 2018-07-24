package org.odk.collect.naxa.common;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.support.v4.app.Fragment;

import org.odk.collect.android.R;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.naxa.network.APIEndpoint;

import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static org.odk.collect.naxa.common.Constant.FormDeploymentFrom.PROJECT;
import static org.odk.collect.naxa.common.Constant.FormDeploymentFrom.SITE;

public class FieldSightFormListFragment extends Fragment {

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
        } catch (NullPointerException | NumberFormatException e) {
            DialogFactory.createGenericErrorDialog(getActivity(), e.getMessage()).show();
            Timber.e("Failed to load xml form %s", e.getMessage());
        } catch (CursorIndexOutOfBoundsException e) {
            DialogFactory.createGenericErrorDialog(getActivity(), getString(R.string.form_not_present)).show();
            Timber.e("Failed to load xml form  %s", e.getMessage());
        }


    }

    protected String generateSubmissionUrl(String formDeployedFrom, String creatorsId, String fsFormId) {
        String submissionUrl = APIEndpoint.BASE_URL + APIEndpoint.FORM_SUBMISSION_PAGE;

        switch (formDeployedFrom) {
            case PROJECT:
                submissionUrl += "project/" + fsFormId + "/" + creatorsId;
                break;
            case SITE:
                submissionUrl += fsFormId + "/" + creatorsId;
                break;
            default:
                throw new RuntimeException("Unknown form deployed");
        }

        return submissionUrl;

    }

    protected long getFormId(String jrFormId) throws CursorIndexOutOfBoundsException, NullPointerException, NumberFormatException {

        String[] projection = new String[]{FormsProviderAPI.FormsColumns._ID, FormsProviderAPI.FormsColumns.FORM_FILE_PATH};
        String selection = FormsProviderAPI.FormsColumns.JR_FORM_ID + "=?";
        String[] selectionArgs = new String[]{jrFormId};
        String sortOrder = FormsProviderAPI.FormsColumns.JR_VERSION + " DESC LIMIT 1";

        Cursor cursor = getActivity().getContentResolver().query(FormsProviderAPI.FormsColumns.CONTENT_URI,
                projection,
                selection, selectionArgs, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(FormsProviderAPI.FormsColumns._ID);
        long formId = Long.parseLong(cursor.getString(columnIndex));

        cursor.close();

        return formId;
    }
}