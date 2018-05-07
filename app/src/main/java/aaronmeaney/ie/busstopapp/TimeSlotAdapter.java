package aaronmeaney.ie.busstopapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.MyViewHolder> {
    private Bus bus;
    private List<TimeSlot> timeSlots;
    private AdapterHailCallback hailCallback;
    private AdapterViewBusStopCallback viewBusStopCallback;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView subtitle;
        public ImageView hailBtn;
        public ImageView viewBusStopBtn;

        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            hailBtn = view.findViewById(R.id.left_btn);
            viewBusStopBtn = view.findViewById(R.id.right_btn);
        }
    }

    public TimeSlotAdapter(Bus bus, List<TimeSlot> timeSlots, AdapterHailCallback hailCallback, AdapterViewBusStopCallback viewBusStopCallback) {
        this.bus = bus;
        this.timeSlots = timeSlots;
        this.hailCallback = hailCallback;
        this.viewBusStopCallback = viewBusStopCallback;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bus_stop_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final BusStop busStop = timeSlots.get(position).getBusStop();

        holder.title.setText(busStop.getId());
        holder.subtitle.setText("ETA: " + timeSlots.get(position).getTime());
        holder.hailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hailCallback.onHailClickedCallback(bus, busStop);
            }
        });

        holder.viewBusStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewBusStopCallback.onViewBusStopClicked(busStop);
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    @Override
    public String toString() {
        return "TimeSlotAdapter{" +
                "timeSlots=" + Arrays.toString(timeSlots.toArray()) +
                '}';
    }

    public void update(List<TimeSlot> newTimeSlots) {
        timeSlots.clear();
        timeSlots.addAll(newTimeSlots);
        this.notifyDataSetChanged();
    }
}
