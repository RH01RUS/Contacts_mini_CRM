package com.example.contactmanager.ui.events

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.data.model.Contact
import com.example.contactmanager.data.model.Event
import com.example.contactmanager.databinding.FragmentEventListBinding
import com.example.contactmanager.ui.contacts.ContactViewModel
import com.example.contactmanager.utils.PermissionHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EventListFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val eventViewModel: EventViewModel by viewModels()
    private val contactViewModel: ContactViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private lateinit var searchView: SearchView
    private lateinit var emptyView: LinearLayout
    private lateinit var emptyTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroupEventType: ChipGroup
    private lateinit var chipAllEvents: Chip
    private lateinit var chipMeeting: Chip
    private lateinit var chipCall: Chip
    private lateinit var chipOther: Chip
    private lateinit var permissionHelper: PermissionHelper

    // Переменные для свайпа
    private lateinit var swipeCallback: SwipeToDeleteEventCallback
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private var contactsList: List<Contact> = listOf()
    private var pendingSyncEvent: Event? = null
    private var recentlyDeletedEvent: Event? = null
    private var recentlyDeletedPosition: Int = -1

    companion object {
        private const val REQUEST_CODE_CALENDAR = 1002
        private const val TAG = "EventListFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        permissionHelper = PermissionHelper(context)

        initViews()
        setupRecyclerView()
        setupSwipeToDelete()
        setupObservers()
        setupListeners()
        setupEventTypeFilters()

        lifecycleScope.launch {
            delay(5000)
            if (progressBar.isVisible) {
                progressBar.isVisible = false
            }
        }

        eventViewModel.loadAllEvents()
    }

    private fun initViews() {
        recyclerView = binding.recyclerView
        searchView = binding.searchView
        emptyView = binding.emptyView
        emptyTextView = binding.emptyTextView
        progressBar = binding.progressBar
        chipGroupEventType = binding.chipGroupEventType
        chipAllEvents = binding.chipAllEvents
        chipMeeting = binding.chipMeeting
        chipCall = binding.chipCall
        chipOther = binding.chipOther
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(
            onItemClick = { event: Event ->
                showEventDetails(event)
            },
            onCallClick = { event: Event ->
                callContact(event.contactPhone)
            },
            onSmsClick = { event: Event ->
                sendSms(event.contactPhone)
            },
            onEmailClick = { event: Event ->
                sendEmail(event.contactEmail, event.contactName)
            },
            onWhatsappClick = { event: Event ->
                openWhatsapp(event.contactPhone)
            },
            onTelegramClick = { event: Event ->
                openTelegram(event.contactPhone)
            },
            onSyncClick = { event: Event ->
                syncEvent(event)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = eventAdapter
    }

    private fun setupSwipeToDelete() {
        swipeCallback = object : SwipeToDeleteEventCallback(requireContext()) {
            override fun onLeftSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Свайп влево - удаление
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val deletedEvent = eventAdapter.getItem(position)

                    recentlyDeletedEvent = deletedEvent
                    recentlyDeletedPosition = position

                    eventViewModel.deleteEvent(deletedEvent)
                    showUndoSnackbar()
                }
            }

            override fun onRightSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                // Свайп вправо - редактирование
                if (position != RecyclerView.NO_POSITION) {
                    val event = eventAdapter.getItem(position)
                    showEditEventDialog(event)
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
            "Событие удалено",
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("Отменить") {
            recentlyDeletedEvent?.let { event ->
                eventViewModel.saveEvent(event)
                recentlyDeletedEvent = null
                recentlyDeletedPosition = -1
            }
        }

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    recentlyDeletedEvent = null
                    recentlyDeletedPosition = -1
                }
            }
        })

        snackbar.show()
    }

    private fun setupObservers() {
        eventViewModel.filteredEvents.observe(viewLifecycleOwner) { eventsList: List<Event>? ->
            Log.d(TAG, "Получено событий: ${eventsList?.size ?: 0}")
            eventAdapter.submitList(eventsList ?: emptyList())
            updateEmptyView(eventsList.isNullOrEmpty())
            progressBar.isVisible = false
        }

        eventViewModel.isLoading.observe(viewLifecycleOwner) { isLoading: Boolean? ->
            val shouldShow = isLoading == true && eventAdapter.itemCount == 0
            progressBar.isVisible = shouldShow
        }

        contactViewModel.allContacts.observe(viewLifecycleOwner) { contacts: List<Contact>? ->
            contactsList = contacts ?: emptyList()
        }

        eventViewModel.syncIntent.observe(viewLifecycleOwner) { intent: Intent? ->
            if (intent != null) {
                try {
                    startActivity(intent)
                    Toast.makeText(requireContext(), "Событие синхронизировано", Toast.LENGTH_SHORT).show()
                    eventViewModel.clearSyncIntent()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при запуске календаря", e)
                }
            }
        }
    }

    private fun setupListeners() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { query: String ->
                    eventViewModel.onSearchQueryChanged(query)
                }
                return true
            }
        })

        searchView.setOnCloseListener {
            eventViewModel.onSearchQueryChanged("")
            false
        }
    }

    private fun setupEventTypeFilters() {
        chipAllEvents.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                chipMeeting.isChecked = false
                chipCall.isChecked = false
                chipOther.isChecked = false
                eventViewModel.clearFilters()
            }
        }

        chipMeeting.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) chipAllEvents.isChecked = false
            eventViewModel.toggleEventTypeFilter("Встреча", isChecked)
        }

        chipCall.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) chipAllEvents.isChecked = false
            eventViewModel.toggleEventTypeFilter("Звонок", isChecked)
        }

        chipOther.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) chipAllEvents.isChecked = false
            eventViewModel.toggleEventTypeFilter("Другое", isChecked)
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEditEventDialog(event: Event) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_event, null)

        val spinnerEventType = dialogView.findViewById<Spinner>(R.id.spinnerEventType)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val textSelectedDate = dialogView.findViewById<TextView>(R.id.textSelectedDate)
        val textSelectedTime = dialogView.findViewById<TextView>(R.id.textSelectedTime)
        val editNote = dialogView.findViewById<EditText>(R.id.editNote)
        val textContactInfo = dialogView.findViewById<TextView>(R.id.textContactInfo)

        textContactInfo.text = "${event.contactName}\n${event.contactPhone}"

        var currentDate = event.date
        var currentTime = event.time

        textSelectedDate.text = dateFormat.format(event.date)
        textSelectedTime.text = event.time
        editNote.setText(event.note ?: "")

        btnSelectDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = currentDate }
            DatePickerDialog(
                requireContext(),
                { _, year: Int, month: Int, dayOfMonth: Int ->
                    cal.set(year, month, dayOfMonth)
                    currentDate = cal.time
                    textSelectedDate.text = dateFormat.format(currentDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSelectTime.setOnClickListener {
            val timeParts = currentTime.split(":")
            val hour = timeParts.getOrNull(0)?.toInt() ?: 12
            val minute = timeParts.getOrNull(1)?.toInt() ?: 0

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay: Int, minuteOfHour: Int ->
                    currentTime = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                    textSelectedTime.text = currentTime
                },
                hour,
                minute,
                true
            ).show()
        }

        val eventTypes = listOf("Встреча", "Звонок", "Другое")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, eventTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEventType.adapter = typeAdapter

        val typePosition = eventTypes.indexOf(event.type)
        if (typePosition >= 0) {
            spinnerEventType.setSelection(typePosition)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать событие")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val type = spinnerEventType.selectedItem.toString()
                val note = editNote.text.toString()

                val updatedEvent = event.copy(
                    date = currentDate,
                    time = currentTime,
                    type = type,
                    note = if (note.isNotEmpty()) note else null
                )

                eventViewModel.saveEvent(updatedEvent)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Событие обновлено", Toast.LENGTH_SHORT).show()

                // Закрываем свайп
                val position = eventAdapter.getItemPosition(event)
                if (position != -1) {
                    swipeCallback.closeSwipe(recyclerView, position)
                }
            }
        }

        dialog.show()
    }

    private fun showEventDetails(event: Event) {
        val details = """
            Контакт: ${event.contactName}
            Телефон: ${event.contactPhone}
            Email: ${event.contactEmail.ifEmpty { "не указан" }}
            Дата: ${dateFormat.format(event.date)} ${event.time}
            Тип: ${event.type}
            Статус: ${event.status.ifEmpty { "не указан" }}
            Категория: ${event.category.ifEmpty { "не указана" }}
            Заметка: ${event.note ?: "нет"}
            ${if (!event.calendarEventId.isNullOrEmpty()) "\n✓ Синхронизировано с календарем" else ""}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Детали события")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun callContact(phoneNumber: String) {
        if (phoneNumber.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось совершить звонок", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Номер телефона не указан", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSms(phoneNumber: String) {
        if (phoneNumber.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$phoneNumber")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось открыть SMS", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Номер телефона не указан", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail(email: String, name: String) {
        if (email.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                    putExtra(Intent.EXTRA_SUBJECT, "Событие от Contact Manager")
                    putExtra(Intent.EXTRA_TEXT, "Здравствуйте, $name!\n\n")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось открыть Email", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Email не указан", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWhatsapp(phoneNumber: String) {
        if (phoneNumber.isNotEmpty()) {
            try {
                val cleanNumber = phoneNumber.replace(Regex("[^+0-9]"), "")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$cleanNumber")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "WhatsApp не установлен", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Номер телефона не указан", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTelegram(phoneNumber: String) {
        if (phoneNumber.isNotEmpty()) {
            try {
                val cleanNumber = phoneNumber.replace(Regex("[^+0-9]"), "")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://t.me/$cleanNumber")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Telegram не установлен", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Номер телефона не указан", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncEvent(event: Event) {
        if (!permissionHelper.hasCalendarPermission()) {
            pendingSyncEvent = event
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                ),
                REQUEST_CODE_CALENDAR
            )
            return
        }

        eventViewModel.syncEventToCalendar(event)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_CALENDAR -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    if (pendingSyncEvent != null) {
                        eventViewModel.syncEventToCalendar(pendingSyncEvent!!)
                        pendingSyncEvent = null
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Разрешение получено, повторите синхронизацию",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Необходимо разрешение для работы с календарем",
                        Toast.LENGTH_LONG
                    ).show()
                    pendingSyncEvent = null
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}