package fastscroll.app.alphabetindex;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fastscroll.app.fastscrollalphabetindex.SectionIndexerAdapter;

/**
 * Created by frantic on 2/2/18.
 */

public class DataAdapter extends SectionIndexerAdapter<DataAdapter.MyHolder> implements Comparable {

    private List<Data> dataList;
    private ArrayList<Integer> mSectionPositions;

    DataAdapter(List<Data> dataList) {
        this.dataList = dataList;
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, null);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position) {
        Data data = dataList.get(position);
        holder.title.setText(data.getTitle());
    }

    @Override
    public int getItemCount() {
        return (dataList == null) ? 0 : dataList.size();
    }

    @Override
    public Object[] getSections() {
        List<String> sections = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        for (int i = 0, size = dataList.size(); i < size; i++) {
            String section = String.valueOf(dataList.get(i).getTitle().charAt(0)).toUpperCase();
            if (!sections.contains(section)) {
                sections.add(section);
                mSectionPositions.add(i);
            }
        }
        return sections.toArray(new String[0]);
    }

    @Override
    public int getPositionForSection(int i) {
        return mSectionPositions.get(i);
    }

    @Override
    public int getSectionForPosition(int i) {
        return 0;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return 0;
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        TextView title;

        MyHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
        }
    }
}
