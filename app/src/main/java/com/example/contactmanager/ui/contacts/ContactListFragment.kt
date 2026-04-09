package com.example.contactmanager.ui.contacts

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.data.model.Contact
import com.example.contactmanager.data.model.ContactLog
import com.example.contactmanager.data.model.Event
import com.example.contactmanager.data.model.LeadCategory
import com.example.contactmanager.data.model.LeadStatus
import com.example.contactmanager.databinding.FragmentContactListBinding
import com.example.contactmanager.ui.events.EventViewModel
import com.example.contactmanager.ui.logs.ContactLogAdapter
import com.example.contactmanager.utils.ContactPickerHelper
import com.example.contactmanager.utils.PermissionHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ContactListFragment : Fragment() {

    private var _binding: FragmentContactListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels()
    private lateinit var contactLogViewModel: ContactLogViewModel

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var searchView: SearchView
    private lateinit var fabAddContact: FloatingActionButton
    private lateinit var emptyView: com.example.contactmanager.databinding.ViewEmptyStateBinding
    private lateinit var progressBar: ProgressBar

    // Чипсы для категорий
    private lateinit var chipGroupCategory: ChipGroup
    private lateinit var chipCategoryAll: Chip
    private lateinit var chipCategoryHot: Chip
    private lateinit var chipCategoryWarm: Chip
    private lateinit var chipCategoryCold: Chip

    // Чипсы для статусов
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipNew: Chip
    private lateinit var chipInProgress: Chip
    private lateinit var chipNegotiation: Chip
    private lateinit var chipConverted: Chip
    private lateinit var chipLost: Chip

    private lateinit var contactPickerHelper: ContactPickerHelper
    private lateinit var permissionHelper: PermissionHelper

    private var recentlyDeletedContact: Contact? = null
    private var recentlyDeletedPosition: Int = -1

    // Переменные для свайпа
    private lateinit var swipeCallback: SwipeToDeleteCallback
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    companion object {
        private const val REQUEST_CODE_CONTACTS = 1001
        private const val TAG = "ContactListFragment"
        private const val SEARCH_MIN_LENGTH = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        contactPickerHelper = ContactPickerHelper(context)
        permissionHelper = PermissionHelper(context)
        contactLogViewModel = ViewModelProvider(this)[ContactLogViewModel::class.java]

        initViews()
        setupRecyclerView()
        setupSwipeToDelete()
        setupObservers()
        setupListeners()
        setupChipFilters()
        setupCategoryChipFilters()

        lifecycleScope.launch {
            delay(5000)
            if (progressBar.isVisible) {
                Log.w(TAG, "Прогресс не скрылся после 5 секунд, принудительно скрываем")
                progressBar.isVisible = false
            }
        }

        viewModel.loadAllContacts()
    }

    private fun initViews() {
        recyclerView = binding.recyclerView
        searchView = binding.searchView
        fabAddContact = binding.fabAddContact
        emptyView = binding.emptyView
        emptyView.emptyIcon.setImageResource(R.drawable.ic_contacts_empty)
        emptyView.emptyTitle.text = getString(R.string.empty_contacts_title)
        emptyView.emptySubtitle.text = getString(R.string.empty_contacts_subtitle)
        progressBar = binding.progressBar

        chipGroupCategory = binding.chipGroupCategory
        chipCategoryAll = binding.chipCategoryAll
        chipCategoryHot = binding.chipCategoryHot
        chipCategoryWarm = binding.chipCategoryWarm
        chipCategoryCold = binding.chipCategoryCold

        chipGroupStatus = binding.chipGroupStatus
        chipAll = binding.chipAll
        chipNew = binding.chipNew
        chipInProgress = binding.chipInProgress
        chipNegotiation = binding.chipNegotiation
        chipConverted = binding.chipConverted
        chipLost = binding.chipLost
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(
            onItemClick = { contact ->
                showContactDetails(contact)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = contactAdapter
    }

    private fun setupSwipeToDelete() {
        swipeCallback = object : SwipeToDeleteCallback(requireContext()) {
            override fun onLeftSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Свайп влево - удаление
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val contact = contactAdapter.getItem(position)

                    recentlyDeletedContact = contact
                    recentlyDeletedPosition = position
                    viewModel.deleteContact(contact)
                    showUndoSnackbar()
                }
            }

            override fun onRightSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                // Свайп вправо - редактирование
                if (position != RecyclerView.NO_POSITION) {
                    val contact = contactAdapter.getItem(position)
                    showEditContactDialog(contact)
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
            "Контакт удален",
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("Отменить") {
            recentlyDeletedContact?.let { contact ->
                viewModel.saveContact(contact)
                recentlyDeletedContact = null
                recentlyDeletedPosition = -1
            }
        }

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    recentlyDeletedContact = null
                    recentlyDeletedPosition = -1
                }
            }
        })

        snackbar.show()
    }

    private fun setupObservers() {
        viewModel.filteredContacts.observe(viewLifecycleOwner) { contacts ->
            Log.d(TAG, "Filtered contacts: ${contacts?.size ?: 0}")
            contactAdapter.submitList(contacts ?: emptyList())
            updateEmptyView(contacts.isNullOrEmpty())
            progressBar.isVisible = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val shouldShow = isLoading == true && contactAdapter.itemCount == 0
            if (progressBar.isVisible != shouldShow) {
                progressBar.isVisible = shouldShow
            }
            Log.d(TAG, "isLoading: $isLoading, itemCount: ${contactAdapter.itemCount}, showProgress: $shouldShow")
        }
    }

    private fun setupListeners() {
        fabAddContact.setOnClickListener {
            showAddContactDialog()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { query ->
                    viewModel.onSearchQueryChanged(query)
                }
                return true
            }
        })

        searchView.setOnCloseListener {
            viewModel.onSearchQueryChanged("")
            false
        }
    }

    private fun setupChipFilters() {
        chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chipNew.isChecked = false
                chipInProgress.isChecked = false
                chipNegotiation.isChecked = false
                chipConverted.isChecked = false
                chipLost.isChecked = false
                viewModel.clearStatusFilters()
            }
        }

        chipNew.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipAll.isChecked = false
            viewModel.toggleStatusFilter(LeadStatus.NEW, isChecked)
        }

        chipInProgress.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipAll.isChecked = false
            viewModel.toggleStatusFilter(LeadStatus.IN_PROGRESS, isChecked)
        }

        chipNegotiation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipAll.isChecked = false
            viewModel.toggleStatusFilter(LeadStatus.NEGOTIATION, isChecked)
        }

        chipConverted.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipAll.isChecked = false
            viewModel.toggleStatusFilter(LeadStatus.CONVERTED, isChecked)
        }

        chipLost.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipAll.isChecked = false
            viewModel.toggleStatusFilter(LeadStatus.LOST, isChecked)
        }
    }

    private fun setupCategoryChipFilters() {
        chipCategoryAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chipCategoryHot.isChecked = false
                chipCategoryWarm.isChecked = false
                chipCategoryCold.isChecked = false
                viewModel.clearCategoryFilters()
            }
        }

        chipCategoryHot.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipCategoryAll.isChecked = false
            viewModel.toggleCategoryFilter(LeadCategory.HOT, isChecked)
        }

        chipCategoryWarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipCategoryAll.isChecked = false
            viewModel.toggleCategoryFilter(LeadCategory.WARM, isChecked)
        }

        chipCategoryCold.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipCategoryAll.isChecked = false
            viewModel.toggleCategoryFilter(LeadCategory.COLD, isChecked)
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

    private fun showAddContactDialog() {
        val options = arrayOf(
            "Создать новый контакт",
            "Импортировать из телефонной книги"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Добавить контакт")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showNewContactDialog()
                    1 -> showContactPickerDialog()
                }
            }
            .show()
    }

    private fun showNewContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail)
        val editCompany = dialogView.findViewById<EditText>(R.id.editCompany)
        val editPosition = dialogView.findViewById<EditText>(R.id.editPosition)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("❄️ Холодный", "⭐ Теплый", "🔥 Горячий")
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Новый контакт")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = editName.text.toString().trim()
                val phone = editPhone.text.toString().trim()
                val email = editEmail.text.toString().trim()
                val company = editCompany.text.toString().trim()
                val position = editPosition.text.toString().trim()
                val selectedCategory = when(spinnerCategory.selectedItemPosition) {
                    0 -> LeadCategory.COLD
                    1 -> LeadCategory.WARM
                    2 -> LeadCategory.HOT
                    else -> LeadCategory.COLD
                }

                when {
                    name.isEmpty() -> {
                        Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show()
                    }
                    phone.isEmpty() -> {
                        Toast.makeText(requireContext(), "Введите телефон", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val newContact = Contact(
                            name = name,
                            phone = phone,
                            email = email,
                            company = if (company.isNotEmpty()) company else null,
                            position = if (position.isNotEmpty()) position else null,
                            status = LeadStatus.NEW,
                            category = selectedCategory
                        )
                        viewModel.saveContact(newContact)
                        Toast.makeText(requireContext(), "Контакт добавлен", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditContactDialog(contact: Contact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_contact, null)

        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail)
        val editCompany = dialogView.findViewById<EditText>(R.id.editCompany)
        val editPosition = dialogView.findViewById<EditText>(R.id.editPosition)
        val editNote = dialogView.findViewById<EditText>(R.id.editNote)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerStatus)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        editName.setText(contact.name)
        editPhone.setText(contact.phone)
        editEmail.setText(contact.email)
        editCompany.setText(contact.company ?: "")
        editPosition.setText(contact.position ?: "")
        editNote.setText(contact.note ?: "")

        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Новый", "В работе", "Переговоры", "Конвертирован", "Потерян")
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter
        spinnerStatus.setSelection(LeadStatus.values().indexOf(contact.status))

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("❄️ Холодный", "⭐ Теплый", "🔥 Горячий")
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        val categoryPosition = when(contact.category) {
            LeadCategory.COLD -> 0
            LeadCategory.WARM -> 1
            LeadCategory.HOT -> 2
        }
        spinnerCategory.setSelection(categoryPosition)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать контакт")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = editName.text.toString().trim()
                val phone = editPhone.text.toString().trim()
                val email = editEmail.text.toString().trim()
                val company = editCompany.text.toString().trim()
                val position = editPosition.text.toString().trim()
                val note = editNote.text.toString().trim()
                val selectedStatus = LeadStatus.values()[spinnerStatus.selectedItemPosition]
                val selectedCategory = when(spinnerCategory.selectedItemPosition) {
                    0 -> LeadCategory.COLD
                    1 -> LeadCategory.WARM
                    2 -> LeadCategory.HOT
                    else -> LeadCategory.COLD
                }

                when {
                    name.isEmpty() -> Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show()
                    phone.isEmpty() -> Toast.makeText(requireContext(), "Введите телефон", Toast.LENGTH_SHORT).show()
                    else -> {
                        val updatedContact = contact.copy(
                            name = name, phone = phone, email = email,
                            company = if (company.isNotEmpty()) company else null,
                            position = if (position.isNotEmpty()) position else null,
                            note = if (note.isNotEmpty()) note else null,
                            status = selectedStatus, category = selectedCategory
                        )
                        viewModel.saveContact(updatedContact)
                        Toast.makeText(requireContext(), "Контакт обновлен", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                // При отмене закрываем свайп
                val position = contactAdapter.getItemPosition(contact)
                if (position != -1) {
                    swipeCallback.closeSwipe(recyclerView, position)
                }
            }
            .create()

        dialog.show()
    }

    private fun showContactDetails(contact: Contact) {
        val categoryText = when(contact.category) {
            LeadCategory.HOT -> "🔥 Горячий"
            LeadCategory.WARM -> "⭐ Теплый"
            LeadCategory.COLD -> "❄️ Холодный"
        }

        val details = """
        Имя: ${contact.name}
        Телефон: ${contact.phone}
        Email: ${contact.email.ifEmpty { "не указан" }}
        Компания: ${contact.company ?: "не указана"}
        Должность: ${contact.position ?: "не указана"}
        Статус: ${getStatusText(contact.status)}
        Категория: $categoryText
        Примечание: ${contact.note ?: "нет"}
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Детали контакта")
            .setMessage(details)
            .setPositiveButton("OK") { _, _ ->
                // Закрыть диалог
            }
            .setNeutralButton("Журнал") { _, _ ->
                showContactLogs(contact)
            }
            .setNegativeButton("События") { _, _ ->
                showContactEvents(contact)
            }
            .show()
    }

    private fun getStatusText(status: LeadStatus): String {
        return when(status) {
            LeadStatus.NEW -> "Новый"
            LeadStatus.IN_PROGRESS -> "В работе"
            LeadStatus.NEGOTIATION -> "Переговоры"
            LeadStatus.CONVERTED -> "Конвертирован"
            LeadStatus.LOST -> "Потерян"
        }
    }

    private fun getCategoryText(category: LeadCategory): String {
        return when(category) {
            LeadCategory.HOT -> "🔥 Горячий"
            LeadCategory.WARM -> "⭐ Теплый"
            LeadCategory.COLD -> "❄️ Холодный"
        }
    }

    private fun showContactEvents(contact: Contact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_contact_events, null)

        val tvContactName = dialogView.findViewById<TextView>(R.id.tvContactName)
        val tvContactPhone = dialogView.findViewById<TextView>(R.id.tvContactPhone)
        val btnAddEvent = dialogView.findViewById<Button>(R.id.btnAddEvent)
        val chipNew = dialogView.findViewById<Chip>(R.id.chipNew)
        val chipInProgress = dialogView.findViewById<Chip>(R.id.chipInProgress)
        val chipNegotiation = dialogView.findViewById<Chip>(R.id.chipNegotiation)
        val chipConverted = dialogView.findViewById<Chip>(R.id.chipConverted)
        val chipLost = dialogView.findViewById<Chip>(R.id.chipLost)

        tvContactName.text = contact.name
        tvContactPhone.text = contact.phone

        eventViewModel.getEventsByContact(contact.id).observe(viewLifecycleOwner) { events: List<Event> ->
            val meetingCount = events.count { it.type == "Встреча" }
            val callCount = events.count { it.type == "Звонок" }
            val otherCount = events.count { it.type == "Другое" }

            chipNew.text = "📅 Встречи ($meetingCount)"
            chipInProgress.text = "📞 Звонки ($callCount)"
            chipNegotiation.text = "📝 Другое ($otherCount)"
            chipConverted.visibility = View.GONE
            chipLost.text = "📊 Всего: ${events.size}"
        }

        btnAddEvent.setOnClickListener {
            val currentDialog = dialogView.parent as? androidx.appcompat.app.AlertDialog
            currentDialog?.dismiss()
            showAddEventForContact(contact)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("События контакта")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun showContactLogs(contact: Contact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_contact_logs, null)

        val tvContactInfo = dialogView.findViewById<TextView>(R.id.tvContactInfo)
        val btnAddLog = dialogView.findViewById<Button>(R.id.btnAddLog)
        val logsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = dialogView.findViewById<TextView>(R.id.emptyView)

        tvContactInfo.text = "${contact.name}\n${contact.phone}"

        val logAdapter = ContactLogAdapter { log ->
            showLogDetails(log)
        }
        logsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        logsRecyclerView.adapter = logAdapter

        contactLogViewModel.loadLogsForContact(contact.id)
        contactLogViewModel.logs.observe(viewLifecycleOwner) { logs ->
            logAdapter.submitList(logs)
            if (logs.isEmpty()) {
                logsRecyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                logsRecyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
        }

        btnAddLog.setOnClickListener {
            showAddContactLogDialog(contact)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Журнал контактов")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun showAddContactLogDialog(contact: Contact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact_log, null)

        val tvContactInfo = dialogView.findViewById<TextView>(R.id.tvContactInfo)
        val spinnerContactType = dialogView.findViewById<Spinner>(R.id.spinnerContactType)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val textSelectedDate = dialogView.findViewById<TextView>(R.id.textSelectedDate)
        val textSelectedTime = dialogView.findViewById<TextView>(R.id.textSelectedTime)
        val seekBarQuality = dialogView.findViewById<SeekBar>(R.id.seekBarQuality)
        val tvQualityValue = dialogView.findViewById<TextView>(R.id.tvQualityValue)
        val editNote = dialogView.findViewById<EditText>(R.id.editNote)

        tvContactInfo.text = "${contact.name}\n${contact.phone}"

        // Настройка спиннера типов
        val contactTypes = listOf("Звонок", "SMS", "Email", "WhatsApp", "Telegram",  "Личная встреча")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, contactTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerContactType.adapter = typeAdapter

        // Настройка ползунка качества
        seekBarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = "$progress%"
                tvQualityValue.text = percent
                val color = when (progress) {
                    in 0..20 -> "#F44336"
                    in 21..40 -> "#FF7043"
                    in 41..60 -> "#FFC107"
                    in 61..80 -> "#8BC34A"
                    else -> "#4CAF50"
                }
                tvQualityValue.setTextColor(Color.parseColor(color))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        seekBarQuality.progress = 50
        tvQualityValue.text = "50%"

        // Дата и время
        var selectedDate = Date()
        var selectedTime = "12:00"

        val calendar = Calendar.getInstance()
        selectedDate = calendar.time
        selectedTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        textSelectedDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedDate)
        textSelectedTime.text = selectedTime

        btnSelectDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth)
                    selectedDate = cal.time
                    textSelectedDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSelectTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    textSelectedTime.text = selectedTime
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить запись в журнал")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val type = spinnerContactType.selectedItem.toString()
                val quality = seekBarQuality.progress
                val note = editNote.text.toString()

                // Комбинируем дату и время
                val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedDate)
                val dateTimeStr = "$dateStr $selectedTime"
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val finalDate = try {
                    dateFormat.parse(dateTimeStr) ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                Log.d(TAG, "Добавление записи: тип=$type, качество=$quality, дата=$finalDate, заметка=$note")

                val newLog = ContactLog(
                    contactId = contact.id,
                    contactName = contact.name,
                    contactPhone = contact.phone,
                    date = finalDate,
                    type = type,
                    quality = quality,
                    note = if (note.isNotEmpty()) note else null
                )

                contactLogViewModel.saveLog(newLog)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Запись добавлена", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showLogDetails(log: ContactLog) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val qualityColor = when (log.quality) {
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
            Дата: ${dateFormat.format(log.date)}
            Качество: ${log.quality}% - $qualityColor
            Заметка: ${log.note ?: "нет"}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Детали записи")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .setNeutralButton("Удалить") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Удаление записи")
                    .setMessage("Вы уверены, что хотите удалить эту запись?")
                    .setPositiveButton("Удалить") { _, _ ->
                        contactLogViewModel.deleteLog(log)
                        Toast.makeText(requireContext(), "Запись удалена", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            .show()
    }

    private fun showAddEventForContact(contact: Contact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)

        val spinnerContact = dialogView.findViewById<Spinner>(R.id.spinnerContact)
        val spinnerEventType = dialogView.findViewById<Spinner>(R.id.spinnerEventType)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val textSelectedDate = dialogView.findViewById<TextView>(R.id.textSelectedDate)
        val textSelectedTime = dialogView.findViewById<TextView>(R.id.textSelectedTime)
        val editNote = dialogView.findViewById<EditText>(R.id.editNote)

        val contactNames = listOf(contact.name)
        val contactAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, contactNames)
        contactAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerContact.adapter = contactAdapter
        spinnerContact.isEnabled = false

        val contactStatus = getStatusText(contact.status)
        val contactCategory = getCategoryText(contact.category)

        var localSelectedDate = Date()
        var localSelectedTime = "12:00"

        val calendar = Calendar.getInstance()
        localSelectedDate = calendar.time
        localSelectedTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        textSelectedDate.text = dateFormat.format(localSelectedDate)
        textSelectedTime.text = localSelectedTime

        btnSelectDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth)
                    localSelectedDate = cal.time
                    textSelectedDate.text = dateFormat.format(localSelectedDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSelectTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    localSelectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    textSelectedTime.text = localSelectedTime
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        val eventTypes = listOf("Встреча", "Звонок", "Другое")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, eventTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEventType.adapter = typeAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить событие для ${contact.name}")
            .setView(dialogView)
            .setPositiveButton("Добавить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val type = spinnerEventType.selectedItem.toString()
                val note = editNote.text.toString()

                val newEvent = Event(
                    contactId = contact.id,
                    contactName = contact.name,
                    contactPhone = contact.phone,
                    contactEmail = contact.email,
                    date = localSelectedDate,
                    time = localSelectedTime,
                    type = type,
                    note = if (note.isNotEmpty()) note else null,
                    status = contactStatus,
                    category = contactCategory
                )

                eventViewModel.saveEvent(newEvent)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Событие добавлено", Toast.LENGTH_SHORT).show()
                showContactEvents(contact)
            }
        }

        dialog.show()
    }

    private fun deleteContact(contact: Contact) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление контакта")
            .setMessage("Вы уверены, что хотите удалить контакт '${contact.name}'?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteContact(contact)
                Toast.makeText(requireContext(), "Контакт удален", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showContactPickerDialog() {
        if (!permissionHelper.hasContactsPermission()) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CODE_CONTACTS)
            return
        }
        showContactSearchDialog()
    }

    private fun showContactSearchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_contacts, null)
        val editSearch = dialogView.findViewById<EditText>(R.id.editSearch)
        val btnSearch = dialogView.findViewById<Button>(R.id.btnSearch)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val listView = dialogView.findViewById<ListView>(R.id.listView)
        val emptyText = dialogView.findViewById<TextView>(R.id.emptyText)

        editSearch.imeOptions = EditorInfo.IME_ACTION_SEARCH
        editSearch.setSingleLine(true)

        var searchJob: Job? = null
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Поиск контактов")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .create()

        fun performContactSearch() {
            val query = editSearch.text.toString().trim()
            when {
                query.isEmpty() -> Toast.makeText(requireContext(), "Введите имя или номер телефона", Toast.LENGTH_SHORT).show()
                query.length < SEARCH_MIN_LENGTH -> Toast.makeText(requireContext(), "Введите минимум $SEARCH_MIN_LENGTH символа", Toast.LENGTH_SHORT).show()
                else -> {
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editSearch.windowToken, 0)
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        performSearch(query, progressBar, listView, emptyText, dialog)
                    }
                }
            }
        }

        btnSearch.setOnClickListener { performContactSearch() }
        editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performContactSearch()
                true
            } else false
        }
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                listView.visibility = View.GONE
                emptyText.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        dialog.show()
    }

    private suspend fun performSearch(
        query: String, progressBar: ProgressBar, listView: ListView, emptyText: TextView, dialog: AlertDialog
    ) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            listView.visibility = View.GONE
            emptyText.visibility = View.GONE
        }
        try {
            val results = withContext(Dispatchers.IO) { contactPickerHelper.searchContacts(query) }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                if (results.isEmpty()) {
                    emptyText.text = "Контакты не найдены"
                    emptyText.visibility = View.VISIBLE
                } else {
                    showContactSelectionDialog(results, dialog)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                emptyText.text = "Ошибка поиска: ${e.message}"
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun showContactSelectionDialog(contacts: List<ContactPickerHelper.PhoneContact>, parentDialog: AlertDialog) {
        parentDialog.dismiss()
        val contactNames = contacts.map { "${it.name}\n${it.phoneNumber}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите контакт")
            .setItems(contactNames) { _, which ->
                importContact(contacts[which])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun importContact(phoneContact: ContactPickerHelper.PhoneContact) {
        val newContact = Contact(
            name = phoneContact.name,
            phone = phoneContact.phoneNumber,
            email = "",
            company = null,
            position = null,
            note = null,
            status = LeadStatus.NEW,
            category = LeadCategory.COLD
        )
        viewModel.saveContact(newContact)
        Toast.makeText(requireContext(), "Контакт '${phoneContact.name}' импортирован", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showContactPickerDialog()
            } else {
                Toast.makeText(requireContext(), "Необходимо разрешение на чтение контактов", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}