package org.odk.collect.naxa.site;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.CollectAbstractActivity;
import org.odk.collect.android.utilities.ToastUtils;
import org.odk.collect.naxa.common.Connectivity;
import org.odk.collect.naxa.generalforms.ViewModelFactory;
import org.odk.collect.naxa.generalforms.GeneralFormViewModel;
import org.odk.collect.naxa.login.model.Site;

import static org.odk.collect.naxa.common.Constant.EXTRA_OBJECT;

public class FragmentHostActivity extends CollectAbstractActivity {

    Site loadedSite = null;
    Toolbar toolbar;

    public static void start(Context context, Site site) {
        Intent intent = new Intent(context, FragmentHostActivity.class);
        intent.putExtra(EXTRA_OBJECT, site);
        context.startActivity(intent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_dashboard);
        loadedSite = getIntent().getExtras().getParcelable(EXTRA_OBJECT);
        bindUI();
        setupToolbar();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, SiteDashboardFragment.getInstance(loadedSite), "frag0")
                .commit();


    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void bindUI() {
        toolbar = findViewById(R.id.toolbar_general);
        setSupportActionBar(toolbar);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                break;

            case R.id.action_notificaiton:
                //startActivity(new Intent(this, NotificationListActivity.class));

                break;
            case R.id.action_app_settings:
                //startActivity(new Intent(this, SettingsActivity.class));

                break;
            case R.id.action_logout:
                logout();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Boolean isNotConnectedToNetwork = Connectivity.isNotConnected(getApplicationContext());

        if (isNotConnectedToNetwork) {
            ToastUtils.showShortToastInMiddle(getString(R.string.all_msg_logout_no__internet));
            return;
        }

        ToastUtils.showShortToast("Todo logout dialog");
        //DialogFactoryImpl.createLogoutDialog(this);

    }

    public static GeneralFormViewModel obtainViewModel(FragmentActivity activity) {
        // Use a Factory to inject dependencies into the ViewModel

        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());

        GeneralFormViewModel viewModel =
                ViewModelProviders.of(activity,factory).get(GeneralFormViewModel.class);

        return viewModel;
    }


}