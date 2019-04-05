package com.anonymous.anonymous.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.anonymous.anonymous.R;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.RTCSession;
import com.connectycube.videochat.RTCTypes;
import com.connectycube.videochat.view.RTCSurfaceView;

import java.util.List;


public class OpponentsFromCallAdapter extends RecyclerView.Adapter<OpponentsFromCallAdapter.ViewHolder> {

    private static final String TAG = OpponentsFromCallAdapter.class.getSimpleName();
    public int itemHeight;

    private List<ConnectycubeUser> opponents;
    private LayoutInflater inflater;


    public OpponentsFromCallAdapter(Context context, List<ConnectycubeUser> opponents, int itemWidth, int itemHeight) {
        this.opponents = opponents;
        this.itemHeight = itemHeight;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.list_item_opponent_from_call, parent, false);
        ViewHolder vh = new ViewHolder(view);
        initCellHeight(vh, itemHeight);
        return vh;
    }

    public void initCellHeight(ViewHolder holder, int height) {
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        params.height = height;
        holder.itemView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return opponents.size();
    }


    public void add(ConnectycubeUser item) {
        opponents.add(item);
        notifyItemRangeChanged(opponents.size() - 1, opponents.size());
    }

    public void removeItem(int index) {
        opponents.remove(index);
        notifyItemRemoved(index);
        notifyItemRangeChanged(index, opponents.size());
    }

    public List<ConnectycubeUser> getOpponents() {
        return opponents;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConnectycubeUser user = opponents.get(position);
        int userId = user.getId();
        holder.opponentsName.setId(userId);
        holder.userId = userId;
        holder.opponentsName.setText(user.getFullName() != null ? user.getFullName() : user.getLogin());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView opponentsName;
        public TextView connectionStatus;
        public RTCSurfaceView opponentView;
        public int userId;

        ViewHolder(View itemView) {
            super(itemView);
            opponentsName = itemView.findViewById(R.id.opponent_name);
            connectionStatus = itemView.findViewById(R.id.connection_status);
            opponentView = itemView.findViewById(R.id.opponent_view);
        }
    }
}