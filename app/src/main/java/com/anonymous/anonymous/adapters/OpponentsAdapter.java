package com.anonymous.anonymous.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.anonymous.anonymous.R;
import com.anonymous.anonymous.utils.ResourceUtils;
import com.anonymous.anonymous.utils.UiUtils;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class OpponentsAdapter extends BaseAdapter {
    private List<ConnectycubeUser> selectedItems;
    private SelectedItemsCountsChangedListener selectedItemsCountChangedListener;

    protected LayoutInflater inflater;
    private List<ConnectycubeUser> objectsList;

    public OpponentsAdapter(Context context, List<ConnectycubeUser> users) {
        this.objectsList = users;
        this.inflater = LayoutInflater.from(context);
        selectedItems = new ArrayList<>();
    }

    public View getView(final int position, View convertView, final ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_opponents_list, null);
            holder = new ViewHolder();
            holder.opponentIcon = convertView.findViewById(R.id.image_opponent_icon);
            holder.opponentName = convertView.findViewById(R.id.opponentsName);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ConnectycubeUser user = getItem(position);

        if (user != null) {
            holder.opponentName.setText(user.getLogin());

            if (selectedItems.contains(user)) {
                convertView.setBackgroundResource(R.color.background_color_selected_user_item);
                holder.opponentIcon.setBackgroundDrawable(
                        UiUtils.getColoredCircleDrawable(ResourceUtils.getColor(R.color.icon_background_color_selected_user)));
                holder.opponentIcon.setImageResource(R.drawable.ic_checkmark);
            } else {
                convertView.setBackgroundResource(R.color.background_color_normal_user_item);
                holder.opponentIcon.setBackgroundDrawable(UiUtils.getColorCircleDrawable(user.getId()));
                holder.opponentIcon.setImageResource(R.drawable.ic_person);
            }
        }

        convertView.setOnClickListener(v -> {
            toggleSelection(position);
            selectedItemsCountChangedListener.onCountSelectedItemsChanged(selectedItems.size());
        });

        return convertView;
    }

    public Collection<ConnectycubeUser> getSelectedItems() {
        return selectedItems;
    }

    @Override
    public int getCount() {
        return objectsList.size();
    }

    @Override
    public ConnectycubeUser getItem(int position) {
        return objectsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void add(ConnectycubeUser item) {
        objectsList.add(item);
        notifyDataSetChanged();
    }

    public List<ConnectycubeUser> getList() {
        return objectsList;
    }

    public void toggleSelection(int position) {
        ConnectycubeUser item = getItem(position);
        toggleSelection(item);
    }

    public void toggleSelection(ConnectycubeUser item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder {
        ImageView opponentIcon;
        TextView opponentName;
    }

    public void setSelectedItemsCountsChangedListener(SelectedItemsCountsChangedListener selectedItemsCountsChanged) {
        if (selectedItemsCountsChanged != null) {
            this.selectedItemsCountChangedListener = selectedItemsCountsChanged;
        }
    }

    public interface SelectedItemsCountsChangedListener {
        void onCountSelectedItemsChanged(int count);
    }
}
