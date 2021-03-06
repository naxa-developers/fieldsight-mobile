package org.fieldsight.naxa.contact;

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import org.odk.collect.android.application.Collect;
import org.fieldsight.naxa.common.BaseLocalDataSource;
import org.fieldsight.naxa.common.FieldSightDatabase;

import java.util.ArrayList;
import java.util.List;

public class ContactLocalSource implements BaseLocalDataSource<FieldSightContactModel> {

    private static ContactLocalSource contactLocalSource;
    private final ContacstDao dao;


    private ContactLocalSource() {
        FieldSightDatabase database = FieldSightDatabase.getDatabase(Collect.getInstance());//todo inject context
        this.dao = database.getContactsDao();
    }


    public synchronized static ContactLocalSource getInstance() {
        if (contactLocalSource == null) {
            contactLocalSource = new ContactLocalSource();
        }
        return contactLocalSource;
    }


    @Override
    public LiveData<List<FieldSightContactModel>> getAll() {
        return dao.getAll();
    }

    @Override
    public void save(FieldSightContactModel... items) {
        AsyncTask.execute(() -> dao.insert(items));
    }

    @Override
    public void save(ArrayList<FieldSightContactModel> items) {
        AsyncTask.execute(() -> dao.insert(items));
    }

    @Override
    public void updateAll(ArrayList<FieldSightContactModel> items) {
        AsyncTask.execute(() -> {
            dao.deleteAll();
            dao.insert(items);
        });
    }
}
