import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.bokumin45.archivedownloader.ArchiveFile
import net.bokumin45.archivedownloader.databinding.ItemFileBinding

class ArchiveFileAdapter(
    private val onItemClick: (ArchiveFile) -> Unit
) : ListAdapter<ArchiveFile, ArchiveFileAdapter.ViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: ArchiveFile) {
            binding.fileName.text = file.name
            binding.fileFormat.text = file.format
            binding.fileSize.text = formatFileSize(file.size)
            binding.root.setOnClickListener { onItemClick(file) }
        }

        private fun formatFileSize(size: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB")
            var sizeInBytes = size.toDouble()
            var unitIndex = 0
            while (sizeInBytes >= 1024 && unitIndex < units.size - 1) {
                sizeInBytes /= 1024
                unitIndex++
            }
            return "%.1f %s".format(sizeInBytes, units[unitIndex])
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<ArchiveFile>() {
        override fun areItemsTheSame(oldItem: ArchiveFile, newItem: ArchiveFile) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: ArchiveFile, newItem: ArchiveFile) =
            oldItem == newItem
    }
}