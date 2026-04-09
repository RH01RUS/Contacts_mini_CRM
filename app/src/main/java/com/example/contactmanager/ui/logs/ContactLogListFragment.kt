package com.example.contactmanager.ui.logs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.data.model.ContactLog
import com.example.contactmanager.databinding.FragmentContactLogListBinding
import com.example.contactmanager.ui.contacts.ContactLogViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class ContactLogListFragment : Fragment() {

    private var _binding: FragmentContactLogListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactLogViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var logAdapter: ContactLogAdapter
    private lateinit var searchView: SearchView  // Теперь это android.widget.SearchView
    private lateinit var emptyView: com.example.contactmanager.databinding.ViewEmptyStateBinding
    private lateinit var progressBar: ProgressBar

    // Переменные для свайпа
    private lateinit var swipeCallback: SwipeToDeleteLogCallback
    private lateinit var itemTouchHelper: ItemTouchHelper

    // Переменные для отмены удаления
    private var recentlyDeletedLog: ContactLog? = null
    private var recentlyDeletedPosition: Int = -1

    companion object {
        private const val TAG = "ContactLogListFragment"
        private val DATE_FORMATTER = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        private val TIME_FORMATTER = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactLogListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setupRecyclerView()
        setupSwipeToDelete()
        setupObservers()
        setupListeners()

        viewModel.loadAllLogs()
    }

    private fun initViews() {
        recyclerView = binding.recyclerView
        searchView = binding.searchView  // Теперь это android.widget.SearchView
        emptyView = binding.emptyView
        emptyView.emptyIcon.setImageResource(R.drawable.ic_history)
        emptyView.emptyTitle.text = getString(R.string.empty_logs_title)
        emptyView.emptySubtitle.text = getString(R.string.empty_logs_subtitle)
        progressBar = binding.progressBar
    }

    private fun setupRecyclerView() {
        logAdapter = ContactLogAdapter(
            onItemClick = { log: ContactLog ->
                showLogDetails(log)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = logAdapter
    }

    private fun setupSwipeToDelete() {
        swipeCallback = object : SwipeToDeleteLogCallback(requireContext()) {
            override fun onLeftSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Свайп влево - удаление
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val deletedLog = logAdapter.getItem(position)

                    recentlyDeletedLog = deletedLog
                    recentlyDeletedPosition = position

                    viewModel.deleteLog(deletedLog)
                    showUndoSnackbar()
                }
            }

            override fun onRightSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                // Свайп вправо - редактирование
                if (position != RecyclerView.NO_POSITION) {
                    val log = logAdapter.getItem(position)
                    showEditLogDialog(log)
                    // Закрываем свайп после показа диалога
                    swipeCallback.closeSwipe(recyclerView, position)
                }
            }
        }

        itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showUndoSnackbar() {
        val snackbar = Snackbar.make(
            requireView(),
            "Запись удалена",
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("Отменить") {
            recentlyDeletedLog?.let { log ->
                viewModel.saveLog(log)
                recentlyDeletedLog = null
                recentlyDeletedPosition = -1
            }
        }

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    recentlyDeletedLog = null
                    recentlyDeletedPosition = -1
                }
            }
        })

        snackbar.show()
    }

    private fun setupObservers() {
        viewModel.logs.observe(viewLifecycleOwner) { logs: List<ContactLog>? ->
            logAdapter.submitList(logs ?: emptyList())
            updateEmptyView(logs.isNullOrEmpty())
            progressBar.isVisible = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading: Boolean? ->
            val shouldShow = isLoading == true && logAdapter.itemCount == 0
            progressBar.isVisible = shouldShow
        }
    }

    private fun setupListeners() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterLogs(newText ?: "")
                return true
            }
        })

        searchView.setOnCloseListener {
            filterLogs("")
            false
        }
    }

    private fun filterLogs(query: String) {
        if (query.isEmpty()) {
            viewModel.loadAllLogs()
        } else {
            viewModel.filterLogs(query)
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.root.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.root.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEditLogDialog(log: ContactLog) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_log, null)

        val spinnerContactType = dialogView.findViewById<Spinner>(R.id.spinnerContactType)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val textSelectedDate = dialogView.findViewById<TextView>(R.id.textSelectedDate)
        val textSelectedTime = dialogView.findViewById<TextView>(R.id.textSelectedTime)
        val seekBarQuality = dialogView.findViewById<SeekBar>(R.id.seekBarQuality)
        val tvQualityValue = dialogView.findViewById<TextView>(R.id.tvQualityValue)
        val editNote = dialogView.findViewById<EditText>(R.id.editNote)
        val tvContactInfo = dialogView.findViewById<TextView>(R.id.tvContactInfo)

        // Отображаем информацию о контакте
        tvContactInfo.text = "${log.contactName}\n${log.contactPhone}"

        var currentDate = log.date
        var currentTime = TIME_FORMATTER.format(log.date)
        var currentQuality = log.quality

        // Устанавливаем начальные значения
        textSelectedDate.text = DATE_FORMATTER.format(log.date)
        textSelectedTime.text = currentTime
        seekBarQuality.progress = currentQuality
        tvQualityValue.text = "${currentQuality}%"
        editNote.setText(log.note ?: "")

        // Настройка типов контакта
        val logTypes = listOf("Звонок", "Встреча", "Email", "Другое")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, logTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerContactType.adapter = typeAdapter

        val typePosition = logTypes.indexOf(log.type)
        if (typePosition >= 0) {
            spinnerContactType.setSelection(typePosition)
        }

        // Обработчик SeekBar
        seekBarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentQuality = progress
                tvQualityValue.text = "$progress%"
                val qualityColor = when (progress) {
                    in 0..20 -> "#FF1744"
                    in 21..40 -> "#FF9100"
                    in 41..60 -> "#FFEA00"
                    in 61..80 -> "#00E676"
                    else -> "#00C853"
                }
                tvQualityValue.setTextColor(android.graphics.Color.parseColor(qualityColor))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Выбор даты
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { time = currentDate }
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    currentDate = calendar.time
                    textSelectedDate.text = DATE_FORMATTER.format(currentDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Выбор времени
        btnSelectTime.setOnClickListener {
            val timeParts = currentTime.split(":")
            val hour = timeParts.getOrNull(0)?.toInt() ?: 12
            val minute = timeParts.getOrNull(1)?.toInt() ?: 0

            TimePickerDialog(
                requireContext(),
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minuteOfHour ->
                    currentTime = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                    textSelectedTime.text = currentTime
                },
                hour,
                minute,
                true
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать запись")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val type = spinnerContactType.selectedItem.toString()
                val note = editNote.text.toString()

                // Объединяем дату и время
                val calendar = Calendar.getInstance().apply {
                    time = currentDate
                    val timeParts = currentTime.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toInt() ?: 0)
                    set(Calendar.MINUTE, timeParts.getOrNull(1)?.toInt() ?: 0)
                }

                val updatedLog = log.copy(
                    date = calendar.time,
                    type = type,
                    quality = currentQuality,
                    note = if (note.isNotEmpty()) note else null
                )

                viewModel.saveLog(updatedLog)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Запись обновлена", Toast.LENGTH_SHORT).show()

                // Закрываем свайп
                val position = logAdapter.getItemPosition(log)
                if (position != -1) {
                    swipeCallback.closeSwipe(recyclerView, position)
                }
            }
        }

        dialog.show()
    }

    private fun showLogDetails(log: ContactLog) {
        val qualityText = when (log.quality) {
            in 0..20 -> "🔴 Плохо"
            in 21..40 -> "🟠 Ниже среднего"
            in 41..60 -> "🟡 Средне"
            in 61..80 -> "🟢 Хорошо"
            else -> "✅ Отлично"
        }

        val details = """
            Контакт: ${log.contactName}
            Телефон: ${log.contactPhone}
            Тип: ${log.type}
            Дата: ${DATE_FORMATTER.format(log.date)}
            Качество: ${log.quality}% - $qualityText
            Заметка: ${log.note ?: "нет"}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Детали записи")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .setNeutralButton("Удалить") { _, _ ->
                confirmDeleteLog(log)
            }
            .show()
    }

    private fun confirmDeleteLog(log: ContactLog) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление записи")
            .setMessage("Вы уверены, что хотите удалить эту запись?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteLog(log)
                Toast.makeText(requireContext(), "Запись удалена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}