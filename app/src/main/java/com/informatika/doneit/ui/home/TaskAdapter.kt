package com.informatika.doneit.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.informatika.doneit.R
import com.informatika.doneit.data.model.Task
import com.informatika.doneit.databinding.TaskListItemBinding

class TaskAdapter(
    val donePressed: ((Task) -> Unit)? = null,
    val editPressed: ((Task) -> Unit)? = null,
    val deletePressed: ((Task) -> Unit)? = null
): RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var showDoneButton = true
    private var showEditButton = true

    private var list: MutableList<Task> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = TaskListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)

        // Manage done button visibility
        holder.binding.doneButton.visibility = if (showDoneButton) View.VISIBLE else View.GONE

        // Manage edit button visibility
        holder.binding.editButton.visibility = if (showEditButton) View.VISIBLE else View.GONE
    }

    fun updateList(list: MutableList<Task>){
        this.list = list
        notifyDataSetChanged()
    }

    fun showDoneButton(show: Boolean) {
        showDoneButton = show
        notifyDataSetChanged()
    }

    fun showEditButton(show: Boolean) {
        showEditButton = show
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class TaskViewHolder(val binding: TaskListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.taskTitle.text = task.title
            binding.taskDescription.text = task.description
            binding.taskPriority.text = task.priority

            // Priority color coding
            when (task.priority) {
                "Low" -> {
                    binding.taskPriority.setTextColor(Color.parseColor("#4ea832"))
                }
                "Medium" -> {
                    binding.taskPriority.setTextColor(Color.parseColor("#de891b"))
                }
                "High" -> {
                    binding.taskPriority.setTextColor(Color.parseColor("#de1b1b"))
                }
            }

            binding.taskDueDate.text = task.dueDate
            binding.taskLocation.text = task.location

            binding.doneButton.setOnClickListener {
                donePressed?.invoke(task)
            }

            binding.editButton.setOnClickListener {
                editPressed?.invoke(task)
            }

            binding.deleteButton.setOnClickListener {
                deletePressed?.invoke(task)
            }
        }
    }
}