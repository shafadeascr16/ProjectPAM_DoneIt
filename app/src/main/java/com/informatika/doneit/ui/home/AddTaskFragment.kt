package com.informatika.doneit.ui.home

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.type.DateTime
import dagger.hilt.android.AndroidEntryPoint
import com.informatika.doneit.R
import com.informatika.doneit.data.model.Task
import com.informatika.doneit.databinding.FragmentAddTaskBinding
import com.informatika.doneit.ui.auth.AuthViewModel
import com.informatika.doneit.util.UiState
import com.informatika.doneit.util.snackbar
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddTaskFragment(private val task: Task? = null) : BottomSheetDialogFragment() {

    val TAG: String = "AddTaskFragment"
    lateinit var binding: FragmentAddTaskBinding
    private var closeFunction: ((Boolean) -> Unit)? = null
    private var saveTaskSuccess: Boolean = false
    private var isValid = true
    val viewModel: TaskViewModel by viewModels()
    val authViewModel: AuthViewModel by viewModels()

    private lateinit var datePicker: MaterialDatePicker<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddTaskBinding.bind(view)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setupKeyboardHandling()

        setupBackPress()

        task?.let {
            binding.taskName.setText(it.title)
            binding.taskDescription.setText(it.description)
            binding.dueDateDropdown.setText(it.dueDate)
            binding.priorityDropdown.setText(it.priority)
            binding.locationTextField.setText(it.location)
        }

        val priorityArray = resources.getStringArray(R.array.priority)
        val arrayAdapter = activity?.let { ArrayAdapter(it, R.layout.dropdown_item, priorityArray) }
        binding.priorityDropdown.setAdapter(arrayAdapter)

        // Inisialisasi MaterialDatePicker hanya sekali
        datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .build()

        binding.dueDateDropdown.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(childFragmentManager, "DATE_PICKER")
            }
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = selection ?: return@addOnPositiveButtonClickListener
            val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(selectedDate))
            binding.dueDateDropdown.setText(formattedDate)
        }

        binding.addTaskButton.setOnClickListener {
            if (!validateTask()) {
                return@setOnClickListener
            }

            val taskId = task?.id ?: UUID.randomUUID().toString()

            val title = binding.taskName.text.toString()
            val description = binding.taskDescription.text.toString()
            val priority = binding.priorityDropdown.text.toString()
            val date = binding.dueDateDropdown.text.toString()
            val location = binding.locationTextField.text.toString()

            val newTask = Task(
                id = taskId,
                title = title,
                description = description,
                priority = priority,
                dueDate = date,
                location = location
            )

            if (task == null) {
                viewModel.addTask(newTask)
            } else {
                viewModel.updateTask(newTask)
            }

            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.container, HomeFragment())
                ?.commit()
        }

        observer()
        setupValidationListeners()
    }

    private fun setupValidationListeners() {
        binding.taskName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val taskName = s.toString().trim()
                if (taskName.isEmpty()) {
                    binding.taskName.error = "Task name is required"
                } else if (!isValidTaskName(taskName)) {
                    binding.taskName.error = "Invalid task name"
                } else {
                    binding.taskName.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.priorityDropdown.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val priority = s.toString().trim()
                if (priority.isEmpty()) {
                    binding.priorityDropdown.error = "Priority is required"
                } else if (!isValidPriority(priority)) {
                    binding.priorityDropdown.error = "Invalid priority"
                } else {
                    binding.priorityDropdown.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun validateTask(): Boolean {
        var isValid = true

        // Validate task name
        val taskName = binding.taskName.text.toString().trim()
        if (taskName.isEmpty()) {
            binding.taskName.error = "Task name is required"
            isValid = false
        } else if (!isValidTaskName(taskName)) {
            binding.taskName.error = "Invalid task name"
            isValid = false
        } else {
            binding.taskName.error = null
        }

        // Validate priority
        val priority = binding.priorityDropdown.text.toString().trim()
        if (priority.isEmpty()) {
            binding.priorityDropdown.error = "Priority is required"
            isValid = false
        } else if (!isValidPriority(priority)) {
            binding.priorityDropdown.error = "Invalid priority"
            isValid = false
        } else {
            binding.priorityDropdown.error = null
        }

        return isValid
    }

    private fun isValidTaskName(taskName: String): Boolean {
        // Regex to allow letters, numbers, spaces, and some special characters
        val taskNameRegex = "^[a-zA-Z0-9 .,-]{1,50}$"
        return taskName.matches(taskNameRegex.toRegex())
    }

    private fun isValidPriority(priority: String): Boolean {
        // Ensure priority is one of the predefined options
        val validPriorities = arrayOf("Low", "Medium", "High")
        return priority in validPriorities
    }

    fun observer() {
        viewModel.addTask.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.addTaskButton.isEnabled = false
                }
                is UiState.Success -> {
                    saveTaskSuccess = true
                    snackbar("Task added successfully")
                    closeFunction?.invoke(true)
                    this.dismiss()
                }
                is UiState.Failure -> {
                    snackbar("Save failed, please try again")
                    binding.addTaskButton.isEnabled = true
                }
            }
        }

        viewModel.updateTask.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.addTaskButton.isEnabled = false
                }
                is UiState.Success -> {
                    saveTaskSuccess = true
                    snackbar("Task updated successfully")
                    closeFunction?.invoke(true)
                    this.dismiss()
                }
                is UiState.Failure -> {
                    snackbar("Save failed, please try again")
                    binding.addTaskButton.isEnabled = true
                }
            }
        }
    }

    private fun setupKeyboardHandling() {
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                v.clearFocus()
            }
            false
        }

        binding.taskName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.taskDescription.requestFocus()
                true
            } else false
        }

        binding.taskDescription.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.priorityDropdown.requestFocus()
                true
            } else false
        }
    }

    private fun setupBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun navigateBack() {
        parentFragmentManager.popBackStack()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        closeFunction?.invoke(saveTaskSuccess)
    }

    fun setDismissListener(function: ((Boolean) -> Unit)?) {
        closeFunction = function
    }
}
