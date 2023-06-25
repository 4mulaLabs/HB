package com.example.heartbeat;

import android.media.Image;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DestinationViewHolder extends RecyclerView.ViewHolder {
    TextView ipView;
    ImageView statusImageView;
    ImageView closeButtonView;


    public DestinationViewHolder(@NonNull View itemView, DestinationAdapter.OnItemClickListener listener) {
        super(itemView);
        ipView = itemView.findViewById(R.id.ip);
        statusImageView = itemView.findViewById(R.id.statusImage);
        closeButtonView = itemView.findViewById(R.id.closeButton);

        closeButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onItemClick(getAdapterPosition());
            }
        });
    }
}
