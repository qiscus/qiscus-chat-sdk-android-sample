package com.qiscus.mychatui.ui.adapter;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.nirmana.Nirmana;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class SelectContactAdapter extends SortedRecyclerViewAdapter<SelectContactAdapter.SelectableUser, SelectContactAdapter.VH> {

    private Context context;

    public SelectContactAdapter(Context context) {
        this.context = context;
    }

    @Override
    protected Class<SelectableUser> getItemClass() {
        return SelectableUser.class;
    }

    @Override
    protected int compare(SelectableUser item1, SelectableUser item2) {
        return item1.user.compareTo(item2.user);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_select_contact, parent, false));
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(getData().get(position));
    }

    public void addOrUpdate(List<User> contacts) {
        for (User contact : contacts) {
            SelectableUser selectableUser = new SelectableUser(contact);
            int index = findPosition(selectableUser);
            if (index == -1) {
                getData().add(selectableUser);
            } else {
                selectableUser.selected = getData().get(index).selected;
                getData().updateItemAt(index, selectableUser);
            }
        }
        notifyDataSetChanged();
    }

    public List<User> getSelectedContacts() {
        List<User> contacts = new ArrayList<>();
        int size = getData().size();
        for (int i = 0; i < size; i++) {
            if (getData().get(i).selected) {
                contacts.add(getData().get(i).user);
            }
        }
        return contacts;
    }

    static class VH extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {
        private ImageView avatar;
        private TextView name;
        private CheckBox checkBox;

        private SelectableUser selectableUser;

        VH(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            checkBox = itemView.findViewById(R.id.selected);

            checkBox.setOnCheckedChangeListener(this);
        }

        void bind(SelectableUser selectableUser) {
            this.selectableUser = selectableUser;
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(selectableUser.user.getAvatarUrl())
                    .into(avatar);
            name.setText(selectableUser.user.getName());
            checkBox.setSelected(selectableUser.selected);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            selectableUser.selected = isChecked;
        }
    }

    static class SelectableUser implements Parcelable {
        public static final Creator<SelectableUser> CREATOR = new Creator<SelectableUser>() {
            @Override
            public SelectableUser createFromParcel(Parcel in) {
                return new SelectableUser(in);
            }

            @Override
            public SelectableUser[] newArray(int size) {
                return new SelectableUser[size];
            }
        };
        private User user;
        private boolean selected;

        SelectableUser(User user) {
            this.user = user;
        }

        private SelectableUser(Parcel in) {
            user = in.readParcelable(User.class.getClassLoader());
            selected = in.readByte() != 0;
        }

        @Override
        public int describeContents() {
            return hashCode();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(user, flags);
            dest.writeByte((byte) (selected ? 1 : 0));
        }
    }
}
