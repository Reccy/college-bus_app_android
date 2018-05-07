package aaronmeaney.ie.busstopapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.MyViewHolder> {
    private List<TimeSlot> timeSlots;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView subtitle;

        public MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);
        }
    }

    public TimeSlotAdapter(List<TimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bus_stop_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        BusStop busStop = timeSlots.get(position).getBusStop();
        holder.title.setText(busStop.getId());
        holder.subtitle.setText("ETA: " + timeSlots.get(position).getTime());
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }
}
