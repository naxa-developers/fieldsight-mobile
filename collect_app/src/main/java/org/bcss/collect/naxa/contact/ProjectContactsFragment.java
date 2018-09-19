package org.bcss.collect.naxa.contact;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bcss.collect.android.R;
import org.bcss.collect.naxa.common.Phone;
import org.bcss.collect.naxa.common.RecyclerViewEmptySupport;
import org.bcss.collect.naxa.common.ViewModelFactory;
import org.bcss.collect.naxa.project.data.ProjectViewModel;
import org.bcss.collect.naxa.stages.StageViewModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ProjectContactsFragment extends Fragment implements ContactAdapter.ContactDetailListener {

    private ContactAdapter contactAdapter;

    private ProjectContactViewModel viewModel;
    Phone phone;

    @BindView(R.id.root_layout_empty_layout)
    View emptyLayout;
    private Unbinder unbinder;

    @BindView(R.id.recycler_view)
    RecyclerViewEmptySupport recyclerView;

    public static ProjectContactsFragment getInstance() {
        return new ProjectContactsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_site_list, container, false);
        unbinder = ButterKnife.bind(this, view);
        setupRecycleView();

        phone = new Phone(getActivity());
        ViewModelFactory factory = ViewModelFactory.getInstance(getActivity().getApplication());
        viewModel = ViewModelProviders.of(getActivity(), factory).get(ProjectContactViewModel.class);
        viewModel.getContacts()
                .observe(this, new Observer<List<FieldSightContactModel>>() {
                    @Override
                    public void onChanged(@Nullable List<FieldSightContactModel> fieldSightContactModels) {
                        if (fieldSightContactModels != null) {
                            contactAdapter = new ContactAdapter(fieldSightContactModels, ProjectContactsFragment.this, getActivity());
                            recyclerView.swapAdapter(contactAdapter, true);
                        }
                    }
                });

        return view;
    }

    private void setupRecycleView() {
        contactAdapter = new ContactAdapter(new ArrayList<>(0), ProjectContactsFragment.this, getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
//        recyclerView.setEmptyView(emptyLayout,
//                getString(R.string.empty_message_contact),
//                new RecyclerViewEmptySupport.OnEmptyLayoutClickListener() {
//                    @Override
//                    public void onRetryButtonClick() {
//
//                    }
//                });
        recyclerView.setAdapter(contactAdapter);
    }

    @Override
    public void onContactClicked(FieldSightContactModel contactModel) {
        ContactDetailsBottomSheetFragment contactDetailsBottomSheetFragmentDialog = ContactDetailsBottomSheetFragment.getInstance();
        contactDetailsBottomSheetFragmentDialog.setContact(contactModel);
        contactDetailsBottomSheetFragmentDialog.show(getFragmentManager(), "Custom Bottom Sheet");
    }

    @Override
    public void onPhoneButtonClick(FieldSightContactModel contactModel) {
        phone.ringNumber(contactModel.getFull_name(), contactModel.getPhone());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
