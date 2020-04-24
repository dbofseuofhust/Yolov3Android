package com.jacky.finalexam.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jacky.finalexam.App;
import com.jacky.finalexam.R;
import com.jacky.finalexam.utils.Image;

import java.util.List;
import java.util.Random;

public class CardAdapter extends  RecyclerView.Adapter<CardAdapter.ViewHolder> {
    OnItemClickListener clickListener;
    private List<Image> imageList;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Image image = imageList.get(position);
        ((TextView) (holder.itemView.findViewById(R.id.tv))).setText(image.getTitle());
        ((TextView) (holder.itemView.findViewById(R.id.tv))).setTextColor(Color.WHITE);
        TextPaint paint = ((TextView) (holder.itemView.findViewById(R.id.tv))).getPaint();
        paint.setFakeBoldText(true);
        ((TextView) (holder.itemView.findViewById(R.id.tv))).setTextSize(18);
        Glide.with(App.getContext()).load(image.getImageId()).into(holder.imageView);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != clickListener) {
                    clickListener.onclick(v, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        ImageView imageView;
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.entry);
            imageView = itemView.findViewById(R.id.bg);
            textView = itemView.findViewById(R.id.tv);
        }
    }

    public interface OnItemClickListener {
        void onclick(View view, int position);
    }

    public CardAdapter(List<Image> list, OnItemClickListener clickListener) {
        this.imageList = list;
        this.clickListener = clickListener;
    }
}
