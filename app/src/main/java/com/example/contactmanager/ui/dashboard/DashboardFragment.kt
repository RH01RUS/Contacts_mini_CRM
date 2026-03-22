package com.example.contactmanager.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.contactmanager.R
import com.example.contactmanager.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupRetryButton()

        viewModel.loadStats()
    }

    private fun setupRetryButton() {
        binding.btnRetry.setOnClickListener {
            viewModel.loadStats()
        }
    }

    private fun setupObservers() {
        // Наблюдаем за состоянием загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == true) {
                showLoading()
            } else {
                hideLoading()
            }
        }

        // Наблюдаем за статистикой контактов
        viewModel.contactsStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                hideError()

                binding.tvTotalContacts.text = stats.totalContacts.toString()
                binding.tvNewLeads.text = stats.newLeads.toString()
                binding.tvInProgressLeads.text = stats.inProgressLeads.toString()
                binding.tvNegotiationLeads.text = stats.negotiationLeads.toString()
                binding.tvConvertedLeads.text = stats.convertedLeads.toString()
                binding.tvLostLeads.text = stats.lostLeads.toString()
                binding.tvConversionRate.text = String.format("%.1f%%", stats.conversionRate)
            }
        }

        // Наблюдаем за статистикой событий
        viewModel.eventsStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.tvTotalEvents.text = stats.totalEvents.toString()
                binding.tvCallsCount.text = (stats.eventsByType["Звонок"] ?: 0).toString()
                binding.tvMeetingsCount.text = (stats.eventsByType["Встреча"] ?: 0).toString()
                binding.tvOtherCount.text = (stats.eventsByType["Другое"] ?: 0).toString()
                binding.tvUpcomingEvents.text = stats.upcomingEvents.toString()
                binding.tvEventsToday.text = stats.eventsToday.toString()
                binding.tvEventsThisWeek.text = stats.eventsThisWeek.toString()

                // Проверяем пустое состояние
                val hasContacts = binding.tvTotalContacts.text.toString().toIntOrNull() ?: 0
                if (hasContacts == 0 && stats.totalEvents == 0) {
                    showEmpty()
                } else {
                    hideEmpty()
                }
            }
        }

        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null && errorMessage.isNotEmpty()) {
                showError(errorMessage)
            } else {
                hideError()
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
    }

    private fun hideLoading() {
        binding.progressBar.isVisible = false
    }

    private fun showError(message: String) {
        binding.errorView.isVisible = true
        binding.errorText.text = message
        binding.scrollView.isVisible = false
        binding.emptyView.isVisible = false
    }

    private fun hideError() {
        binding.errorView.isVisible = false
        if (binding.emptyView.isVisible == false) {
            binding.scrollView.isVisible = true
        }
    }

    private fun showEmpty() {
        binding.emptyView.isVisible = true
        binding.scrollView.isVisible = false
        binding.errorView.isVisible = false
    }

    private fun hideEmpty() {
        binding.emptyView.isVisible = false
        if (binding.errorView.isVisible == false) {
            binding.scrollView.isVisible = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}