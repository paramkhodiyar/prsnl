package com.prsnl.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prsnl.storage.repository.DailyBarStat
import com.prsnl.storage.repository.OverallWritingStats
import com.prsnl.storage.repository.StatsRepository
import com.prsnl.storage.repository.TopFolderStat
import com.prsnl.storage.repository.TopNotebookStat
import com.prsnl.storage.repository.YearlyWrappedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WritingStatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _overallStats = MutableStateFlow(OverallWritingStats())
    val overallStats: StateFlow<OverallWritingStats> = _overallStats.asStateFlow()

    private val _topNotebooks = MutableStateFlow<List<TopNotebookStat>>(emptyList())
    val topNotebooks: StateFlow<List<TopNotebookStat>> = _topNotebooks.asStateFlow()

    private val _topFolders = MutableStateFlow<List<TopFolderStat>>(emptyList())
    val topFolders: StateFlow<List<TopFolderStat>> = _topFolders.asStateFlow()

    private val _recentDailyBars = MutableStateFlow<List<DailyBarStat>>(emptyList())
    val recentDailyBars: StateFlow<List<DailyBarStat>> = _recentDailyBars.asStateFlow()

    private val _yearlyWrapped = MutableStateFlow<YearlyWrappedData?>(null)
    val yearlyWrapped: StateFlow<YearlyWrappedData?> = _yearlyWrapped.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Collect overall stats
                launch {
                    statsRepository.getOverallStatsFlow()
                        .catch { emit(OverallWritingStats()) }
                        .collect { _overallStats.value = it }
                }

                // Collect top notebooks
                launch {
                    statsRepository.getTopNotebooksFlow(5)
                        .catch { emit(emptyList()) }
                        .collect { _topNotebooks.value = it }
                }

                // Collect top folders
                launch {
                    statsRepository.getTopFoldersFlow(5)
                        .catch { emit(emptyList()) }
                        .collect { _topFolders.value = it }
                }

                // Collect recent daily activity (7 days)
                launch {
                    statsRepository.getRecentDailyActivityFlow(7)
                        .catch { emit(emptyList()) }
                        .collect { _recentDailyBars.value = it }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadYearlyWrapped(year: String = LocalDate.now().year.toString()) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = statsRepository.getYearlyWrapped(year)
                _yearlyWrapped.value = data
            } catch (e: Exception) {
                android.util.Log.e("WritingStatsVM", "Failed to load yearly wrapped", e)
            }
        }
    }

    fun recalculateStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                statsRepository.recalculateAllStatsFromNotebooks()
            } catch (e: Exception) {
                android.util.Log.e("WritingStatsVM", "Failed to recalculate stats", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
