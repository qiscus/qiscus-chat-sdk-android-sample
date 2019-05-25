package com.qiscus.mychatui.ui.groupchatcreation;

import android.os.Parcel;
import android.os.Parcelable;

import com.qiscus.mychatui.data.model.User;

public class SelectableUser implements Parcelable {
    private User user;
    private boolean selected;

    public SelectableUser(User user) {
        this.user = user;
    }

    private SelectableUser(Parcel in) {
        user = in.readParcelable(User.class.getClassLoader());
        selected = in.readByte() != 0;
    }

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectableUser)) return false;
        SelectableUser that = (SelectableUser) o;
        return user.equals(that.user);
    }
}