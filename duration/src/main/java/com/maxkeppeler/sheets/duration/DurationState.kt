package com.maxkeppeler.sheets.duration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import com.maxkeppeker.sheets.core.views.BaseState
import com.maxkeppeler.sheets.duration.models.DurationConfig
import com.maxkeppeler.sheets.duration.models.DurationSelection
import com.maxkeppeler.sheets.duration.utils.getInputKeys
import com.maxkeppeler.sheets.duration.utils.getValuePairs
import com.maxkeppeler.sheets.duration.utils.parseCurrentTime
import com.maxkeppeler.sheets.duration.utils.parseToSeconds
import java.io.Serializable

/**
 * Handles the duration state.
 * @param selection The selection configuration for the dialog view.
 * @param config The general configuration for the dialog view.
 * @param stateData The data of the state when the state is required to be restored.
 */
internal class DurationState(
    val selection: DurationSelection,
    val config: DurationConfig,
    stateData: DurationStateData? = null
) : BaseState() {

    var timeTextValue by mutableStateOf(stateData?.timeTextValue ?: getInitTimeTextValue())
    var timeInfoInSeconds by mutableStateOf(getInitTimeInfoInSeconds())
    var valuePairs by mutableStateOf(getInitValuePairs())
    var indexOfFirstValue by mutableStateOf(getInitIndexOfFirstValue())
    val keys by mutableStateOf(getInputKeys(config))
    var valid by mutableStateOf(isValid())

    private fun getInitTimeTextValue(): StringBuilder {
        return parseCurrentTime(config.timeFormat, config.currentTime)
    }

    private fun getInitTimeInfoInSeconds(): Triple<Long, Long?, Long?> {
        val value = parseToSeconds(timeTextValue, config.timeFormat)
        val minTime = if (value != 0L && value < config.minTime) config.minTime else null
        val maxTime = if (value != 0L && value > config.maxTime) config.maxTime else null
        return Triple(value, minTime, maxTime)
    }

    private fun getInitValuePairs(): List<Pair<String, String>> {
        return getValuePairs(timeTextValue, config)
    }

    private fun getInitIndexOfFirstValue(): Int? {
        return valuePairs
            .indexOfFirst { runCatching { it.first.toInt() != 0 }.getOrNull() ?: false }
            .takeUnless { it == -1 }
    }

    private fun checkValid() {
        valid = isValid()
    }

    private fun refreshTime() {
        timeInfoInSeconds = getInitTimeInfoInSeconds()
        valuePairs = getInitValuePairs()
        indexOfFirstValue = getInitIndexOfFirstValue()
        checkValid()
    }

    private fun isValid(): Boolean = timeInfoInSeconds.first > 0
            && timeInfoInSeconds.second == null
            && timeInfoInSeconds.third == null

    fun onEnterValue(value: String) {
        val newTimeBuilder = timeTextValue.apply {
            if (length >= config.timeFormat.length) {
                repeat(value.length) { deleteCharAt(0) }
            }
            append(value)
        }
        timeTextValue = StringBuilder(newTimeBuilder.toString())
        refreshTime()
    }

    fun onBackspaceAction() {
        val newTimeBuilder = timeTextValue.apply {
            deleteCharAt(lastIndex)
            insert(0, 0.toString())
        }
        timeTextValue = StringBuilder(newTimeBuilder)
        refreshTime()
    }

    fun onEmptyAction() {
        timeTextValue = StringBuilder(parseCurrentTime(config.timeFormat))
        refreshTime()
    }

    fun onFinish() {
        selection.onPositiveClick(timeInfoInSeconds.first)
    }

    companion object {

        /**
         * [Saver] implementation.
         * @param selection The selection configuration for the dialog view.
         * @param config The general configuration for the dialog view.
         */
        fun Saver(
            selection: DurationSelection,
            config: DurationConfig
        ): Saver<DurationState, *> = Saver(
            save = { state -> DurationStateData(state.timeTextValue) },
            restore = { data -> DurationState(selection, config, data) }
        )
    }

    /**
     * Data class that stores the important information of the current state
     * and can be used by the [Saver] to save and restore the state.
     */
    data class DurationStateData(
        val timeTextValue: StringBuilder,
    ) : Serializable
}