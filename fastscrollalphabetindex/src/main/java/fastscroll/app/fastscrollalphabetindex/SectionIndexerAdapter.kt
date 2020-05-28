package fastscroll.app.fastscrollalphabetindex

import androidx.recyclerview.widget.RecyclerView
import android.widget.SectionIndexer

abstract class SectionIndexerAdapter<VH : RecyclerView.ViewHolder?> : RecyclerView.Adapter<VH>(), SectionIndexer
