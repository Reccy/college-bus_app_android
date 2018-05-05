package aaronmeaney.ie.busstopapp;

import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class BusStopAdapter extends RecyclerView.Adapter<BusStopAdapter.MyViewHolder> {
    private List<BusStop> busStopList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
        }
    }

    public BusStopAdapter(List<BusStop> busStopList) {
        this.busStopList = busStopList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bus_stop_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        BusStop busStop = busStopList.get(position);
        holder.title.setText(busStop.getId());
    }

    @Override
    public int getItemCount() {
        return busStopList.size();
    }
}
