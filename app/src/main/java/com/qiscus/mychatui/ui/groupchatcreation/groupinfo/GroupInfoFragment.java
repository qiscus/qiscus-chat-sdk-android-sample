package com.qiscus.mychatui.ui.groupchatcreation.groupinfo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.ui.GroupChatRoomActivity;
import com.qiscus.mychatui.ui.HomeActivity;
import com.qiscus.mychatui.ui.adapter.OnItemClickListener;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;

import java.util.ArrayList;
import java.util.List;


public class GroupInfoFragment extends Fragment implements GroupInfoPresenter.View {
    private static final String CONTACT_KEY = "CONTACT_KEY";
    private static final String selectMore = "select at least one";
    private static final String groupNameFormat = "Please input group name";

    private RecyclerView recyclerView;
    private ProgressDialog progressDialog;
    private EditText groupNameView;
    private ImageView imgNext;

    private GroupInfoPresenter presenter;

    private List<User> contacts;
    private ContactAdapter adapter;

    public static GroupInfoFragment newInstance(List<User> contacts) {
        GroupInfoFragment fragment = new GroupInfoFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(CONTACT_KEY, (ArrayList<User>) contacts);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Please wait...");

        View view = inflater.inflate(R.layout.fragment_group_info, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewSelected);
        groupNameView = view.findViewById(R.id.group_name_input);
        imgNext = view.findViewById(R.id.img_next);

        imgNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proceedCreateGroup();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        contacts = getArguments().getParcelableArrayList(CONTACT_KEY);
        if (contacts == null) {
            getActivity().finish();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);

        adapter = new ContactAdapter(getActivity(), new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                adapter.remove(contacts.get(position));
            }
        });
        adapter.needRemoveParticipant(true);

        recyclerView.setAdapter(adapter);
        adapter.addOrUpdate(contacts);

        presenter = new GroupInfoPresenter(this, MyApplication.getInstance().getComponent().getChatRoomRepository());
    }

    public void proceedCreateGroup() {
        String groupName = groupNameView.getText().toString();
        boolean groupNameInputted = groupName.trim().length() > 0;
        if (groupNameInputted && selectedContactIsMoreThanOne()) {
            presenter.createGroup(groupName, contacts);
        } else {
            String warningText = (groupNameInputted) ? selectMore : groupNameFormat;
            showErrorMessage(warningText);
        }
    }

    private boolean selectedContactIsMoreThanOne() {
        return this.contacts.size() > 0;
    }

    @Override
    public void showLoading() {
        progressDialog.show();
    }

    @Override
    public void dismissLoading() {
        progressDialog.dismiss();
    }

    @Override
    public void showGroupChatRoomPage(QiscusChatRoom chatRoom) {
        Intent chatIntent = GroupChatRoomActivity.generateIntent(getContext(), chatRoom);
        Intent parentIntent = new Intent(getActivity(), HomeActivity.class);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(getActivity());
        taskStackBuilder.addNextIntentWithParentStack(parentIntent);
        taskStackBuilder.addNextIntent(chatIntent);
        taskStackBuilder.startActivities();
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }
}


