package com.informatika.doneit.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import com.informatika.doneit.R
import com.informatika.doneit.data.model.Task
import com.informatika.doneit.databinding.FragmentHomeBinding
import com.informatika.doneit.ui.auth.AuthViewModel
import com.informatika.doneit.util.UiState
import com.informatika.doneit.util.hide
import com.informatika.doneit.util.show
import com.informatika.doneit.util.snackbar

@AndroidEntryPoint
class HomeFragment : Fragment() {
    val TAG = "HomeFragment"
    private lateinit var binding: FragmentHomeBinding
    private val viewModel: TaskViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var currentSortPriority: String? = null
    val adapter by lazy {
        TaskAdapter(
            donePressed = { task -> donePressed(task) },
            deletePressed = { task -> deletePressed(task) },
            editPressed = { task -> editPressed(task) }
        )
    }
    private var taskList = mutableListOf<Task>()
    private var incompleteTaskList = mutableListOf<Task>()
    private var completeTaskList = mutableListOf<Task>()

    private var doubleBackToExitPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (this::binding.isInitialized){
            return binding.root
        }else {
            binding = FragmentHomeBinding.inflate(inflater, container, false)
            return binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addTaskButton.setOnClickListener {
            navigateToAddTask()
        }

        binding.taskList.layoutManager = LinearLayoutManager(requireContext())
        binding.taskList.adapter = adapter

        authViewModel.getSession {
            viewModel.getTasks(it)
        }

        setupPriorityDropdown()

        setupBackPress()

        observer()
        tabsObserver()
    }

    private fun setupPriorityDropdown() {
        val priorityArray = resources.getStringArray(R.array.priority)
        val priorityAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, priorityArray)

        binding.priorityDropdown.setAdapter(priorityAdapter)
        binding.priorityDropdown.setOnItemClickListener { _, _, position, _ ->
            currentSortPriority = priorityArray[position]
            filterAndSortTasks()
        }

        // Add a clear filter option
        binding.priorityDropdown.setOnLongClickListener {
            // Clear the filter
            currentSortPriority = null
            binding.priorityDropdown.setText("", false)
            filterAndSortTasks()
            true
        }
    }

    private fun filterAndSortTasks() {
        val currentTab = binding.tabs.selectedTabPosition
        val taskList = if (currentTab == 0) incompleteTaskList else completeTaskList

        // Filter by priority if a priority is selected
        val filteredList = if (currentSortPriority != null) {
            taskList.filter { it.priority == currentSortPriority }
        } else {
            taskList
        }

        // Sort by priority
        val sortedList = filteredList.sortedWith(compareBy {
            when (it.priority) {
                "High" -> 0
                "Medium" -> 1
                "Low" -> 2
                else -> 3
            }
        })

        // Update visibility of no items text
        if (sortedList.isEmpty()) {
            binding.noItemsText.visibility = View.VISIBLE
            binding.taskList.visibility = View.GONE
        } else {
            binding.noItemsText.visibility = View.GONE
            binding.taskList.visibility = View.VISIBLE
        }

        // Customize no items text based on filter
        binding.noItemsText.text = if (currentSortPriority != null) {
            "No ${currentSortPriority} priority tasks"
        } else {
            "No tasks found"
        }

        // Convert to mutable list for adapter
        adapter.updateList(sortedList.toMutableList())
    }

    private fun navigateToAddTask() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, AddTaskFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun observer() {
        viewModel.tasks.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.show()
                }
                is UiState.Success -> {
                    binding.progressBar.hide()
                    updateTaskLists(state.data)
                }
                is UiState.Failure -> {
                    binding.progressBar.hide()
                    snackbar("Error loading tasks")
                }
                else -> {
                    binding.progressBar.hide()
                }
            }
        }

        viewModel.doneTask.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val updatedTask = state.data.first
                    updateTaskStatusLocally(updatedTask)
                    snackbar("Task status updated!")
                }
                is UiState.Failure -> {
                    snackbar("Failed to update task status")
                }
                else -> {}
            }
        }
    }

    private fun updateTaskStatusLocally(updatedTask: Task) {
        incompleteTaskList.removeAll { it.id == updatedTask.id }
        completeTaskList.removeAll { it.id == updatedTask.id }

        if (updatedTask.completed) {
            completeTaskList.add(updatedTask)
        } else {
            incompleteTaskList.add(updatedTask)
        }

        val currentTab = binding.tabs.selectedTabPosition
        adapter.updateList(if (currentTab == 0) incompleteTaskList else completeTaskList)
    }

    private fun updateTaskLists(tasks: List<Task>) {
        taskList.clear()
        incompleteTaskList.clear()
        completeTaskList.clear()

        taskList.addAll(tasks)

        for (task in taskList) {
            if (!task.completed) {
                incompleteTaskList.add(task)
            } else {
                completeTaskList.add(task)
            }
        }

        // Reset priority filter and sort
        currentSortPriority = null
        binding.priorityDropdown.setText("", false)
        filterAndSortTasks()
    }

    private fun tabsObserver() {
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        adapter.showDoneButton(true)
                        adapter.showEditButton(true)
                        filterAndSortTasks()
                    }
                    1 -> {
                        adapter.showDoneButton(false)
                        adapter.showEditButton(false)
                        filterAndSortTasks()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun deletePressed(task: Task) {
        viewModel.deleteTask(task)
        snackbar("Task deleted successfully!")
        removeTaskLocally(task)
    }

    private fun donePressed(task: Task) {
        viewModel.doneTask(task)
    }

    private fun setupBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        if (doubleBackToExitPressedOnce) {
            // Exit the app
            requireActivity().finishAffinity()
            return
        }

        // Set flag to true
        doubleBackToExitPressedOnce = true

        // Show toast message
        Toast.makeText(requireContext(), "Press again to exit", Toast.LENGTH_SHORT).show()

        // Reset flag after 2 seconds
        handler.postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }

    private fun removeTaskLocally(task: Task) {
        incompleteTaskList.removeAll { it.id == task.id }
        completeTaskList.removeAll { it.id == task.id }

        val currentTab = binding.tabs.selectedTabPosition
        adapter.updateList(if (currentTab == 0) incompleteTaskList else completeTaskList)
    }

    private fun editPressed(task: Task) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, AddTaskFragment(task))
            .addToBackStack(null)
            .commit()
    }
}