package com.example.torrent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TorrentAdapter extends RecyclerView.Adapter<TorrentAdapter.TorrentViewHolder> {

    private List<Torrent> torrentList;
    private OnItemClickListener listener;
    private RecyclerView recyclerView;


    public interface OnItemClickListener {
        void onItemClick(int position, Torrent torrent);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TorrentAdapter(List<Torrent> torrentList) {
        this.torrentList = torrentList;
    }

    @NonNull
    @Override
    public TorrentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_torrent, parent, false);
        return new TorrentViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TorrentViewHolder holder, int position) {
        Torrent torrent = torrentList.get(position);
        holder.tvTorrentName.setText(torrent.getName());
        holder.tvTorrentCode.setText(torrent.getCode());
        

        holder.progressBar.setProgress(torrent.getProgress());
        holder.tvProgress.setText(torrent.getProgress() + "%");
        

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(adapterPosition, torrentList.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return torrentList.size();
    }

    public void updateTorrentList(List<Torrent> torrentList) {
        this.torrentList = torrentList;
        notifyDataSetChanged();
    }
    
    public void updateTorrentProgress(int position, int progress) {
        if (position >= 0 && position < torrentList.size()) {

            torrentList.get(position).setProgress(progress);
            

            RecyclerView.ViewHolder holder = recyclerView != null ? 
                recyclerView.findViewHolderForAdapterPosition(position) : null;
                
            if (holder instanceof TorrentViewHolder) {
                TorrentViewHolder torrentHolder = (TorrentViewHolder) holder;
                torrentHolder.progressBar.setProgress(progress);
                torrentHolder.tvProgress.setText(progress + "%");
            } else {

                notifyItemChanged(position);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }
    
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    public static class TorrentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTorrentName;
        TextView tvTorrentCode;
        ProgressBar progressBar;
        TextView tvProgress;

        public TorrentViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            tvTorrentName = itemView.findViewById(R.id.tvTorrentName);
            tvTorrentCode = itemView.findViewById(R.id.tvTorrentCode);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvProgress = itemView.findViewById(R.id.tvProgress);
        }
    }
} 