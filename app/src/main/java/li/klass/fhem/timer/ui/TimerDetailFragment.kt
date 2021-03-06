/*
 * AndFHEM - Open Source Android application to control a FHEM home automation
 * server.
 *
 * Copyright (c) 2011, Matthias Klass or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU GENERAL PUBLIC LICENSE, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU GENERAL PUBLIC LICENSE
 * for more details.
 *
 * You should have received a copy of the GNU GENERAL PUBLIC LICENSE
 * along with this distribution; if not, write to:
 *   Free Software Foundation, Inc.
 *   51 Franklin Street, Fifth Floor
 *   Boston, MA  02110-1301  USA
 */

package li.klass.fhem.timer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TableRow
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.timer_detail.view.*
import kotlinx.coroutines.*
import li.klass.fhem.R
import li.klass.fhem.adapter.devices.genericui.AvailableTargetStatesDialogUtil.showSwitchOptionsMenu
import li.klass.fhem.adapter.devices.genericui.availableTargetStates.OnTargetStateSelectedCallback
import li.klass.fhem.constants.Actions.DISMISS_EXECUTING_DIALOG
import li.klass.fhem.constants.Actions.SHOW_TOAST
import li.klass.fhem.constants.BundleExtraKeys.STRING_ID
import li.klass.fhem.devices.backend.at.*
import li.klass.fhem.domain.core.FhemDevice
import li.klass.fhem.fragments.core.BaseFragment
import li.klass.fhem.fragments.device.DeviceNameListFragment
import li.klass.fhem.update.backend.DeviceListService
import li.klass.fhem.util.DialogUtil
import li.klass.fhem.util.getNavigationResult
import li.klass.fhem.widget.TimePickerWithSeconds.getFormattedValue
import li.klass.fhem.widget.TimePickerWithSecondsDialog
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isBlank
import org.joda.time.LocalTime
import javax.inject.Inject

class TimerDetailFragment @Inject constructor(
        private val deviceListService: DeviceListService,
        private val atService: AtService
) : BaseFragment() {
    private var timerDevice: TimerDevice? = null

    @Transient
    private var targetDevice: FhemDevice? = null

    private val args: TimerDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = super.onCreateView(inflater, container, savedInstanceState)
        if (view != null) {
            return view
        }
        view = inflater.inflate(R.layout.timer_detail, container, false)
        val context = activity ?: return null

        bindRepetitionSpinner(view, context)
        bindSelectDeviceButton(view)
        bindTimerTypeSpinner(view, context)
        bindSwitchTimeButton(view)
        bindIsActiveCheckbox(view)
        bindTargetStateButton(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timerNameInput = view.timerNameInput
        setTimerName("", view)
        if (isModify) {
            timerNameInput.isEnabled = false
        }
        view.targetDeviceName.text = ""

        if (isModify && targetDevice == null && args.deviceName != null) {
            setTimerDeviceValuesForName(args.deviceName!!)
        }

        updateTargetDevice(targetDevice, view)
        updateTimerInformation(timerDevice)
        updateTargetStateRowVisibility(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.timer_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.reset -> {
                timerDevice ?: return false
                setValuesForCurrentTimerDevice(timerDevice!!)
                return true
            }
            R.id.save -> {
                save()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun bindTargetStateButton(view: View) {
        view.targetStateSet.setOnClickListener {
            onTargetStateClick(view)
        }
    }

    private fun onTargetStateClick(view: View) {
        val context = activity ?: return
        val device = targetDevice ?: return
        showSwitchOptionsMenu(context, device, object : OnTargetStateSelectedCallback {
            override suspend fun onStateSelected(device: FhemDevice, targetState: String) {
                setTargetState(targetState, view)
            }

            override suspend fun onSubStateSelected(device: FhemDevice, state: String, subState: String) {
                coroutineScope {
                    onStateSelected(device, "$state $subState")
                }
            }

            override suspend fun onNothingSelected(device: FhemDevice) {}
        })
    }

    private fun bindSelectDeviceButton(view: View) {
        getNavigationResult<FhemDevice>()?.observe(viewLifecycleOwner, Observer { device ->
            updateTargetDevice(device, view)
        })
        view.targetDeviceSet.setOnClickListener {
            findNavController()
                    .navigate(TimerDetailFragmentDirections.actionTimerDetailFragmentToDeviceNameSelectionFragment(
                            DEVICE_FILTER, null
                    ))
        }
    }

    private fun bindIsActiveCheckbox(view: View) {
        val isActiveCheckbox = view.isActive
        if (!isModify) {
            isActiveCheckbox.isChecked = true
            isActiveCheckbox.isEnabled = false
        }
    }

    private fun bindSwitchTimeButton(view: View) {
        val switchTimeChangeButton = view.findViewById<Button>(R.id.switchTimeSet)
        switchTimeChangeButton.setOnClickListener {
            val switchTime = getSwitchTime(view) ?: LocalTime.now()
            activity?.let {
                TimePickerWithSecondsDialog(it, switchTime.hourOfDay, switchTime.minuteOfHour, switchTime.secondOfMinute,
                        object : TimePickerWithSecondsDialog.TimePickerWithSecondsListener {
                            override fun onTimeChanged(okClicked: Boolean, hours: Int, minutes: Int, seconds: Int, formattedText: String) {
                                setSwitchTime(LocalTime(hours, minutes, seconds), view)
                            }
                        }).show()
            }
        }
    }

    private fun bindTimerTypeSpinner(view: View, context: Context) {

        view.timerType.adapter = ArrayAdapter<String>(context, R.layout.spinnercontent).apply {
            TimerType.values()
                    .map { view.context.getString(it.text) }
                    .forEach { this.add(it) }
        }
    }

    private fun bindRepetitionSpinner(view: View, context: Context) {
        val repetitionSpinner = view.timerRepetition
        val repetitionAdapter = ArrayAdapter<String>(context, R.layout.spinnercontent)
        AtRepetition.values()
                .forEach { repetitionAdapter.add(view.context.getString(it.stringId)) }
        repetitionSpinner.adapter = repetitionAdapter
    }

    private fun save() {
        val view = view ?: return
        val safeContext = context ?: return

        val switchTime = getSwitchTime(view)
        val timerDeviceName = getTimerName(view)

        if (targetDevice == null || isBlank(getTargetState(view)) || switchTime == null || timerDeviceName == null) {
            activity?.sendBroadcast(Intent(SHOW_TOAST)
                    .putExtra(STRING_ID, R.string.incompleteConfigurationError))
            return
        }

        if (!isModify) {
            if (timerDeviceName.contains(" ")) {
                DialogUtil.showAlertDialog(safeContext, R.string.error, R.string.error_timer_name_spaces)
                return
            }
        }

        val timerDevice = TimerDevice(
                name = timerDeviceName,
                isActive = view.isActive.isChecked,
                definition = TimerDefinition(
                        switchTime = switchTime,
                        repetition = getRepetition(view),
                        type = getType(view),
                        targetDeviceName = targetDevice!!.name,
                        targetState = getTargetState(view),
                        targetStateAppendix = "" // TODO never set?
                ),
                next = timerDevice?.next ?: ""
        )

        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                if (isModify) {
                    atService.modify(timerDevice)
                } else {
                    atService.createNew(timerDevice)
                }
            }
            findNavController().popBackStack()
        }
    }

    private fun updateTargetDevice(targetDevice: FhemDevice?, view: View?) {
        if (view == null || targetDevice == null) {
            return
        }
        this@TimerDetailFragment.targetDevice = targetDevice
        view.targetDeviceName.text = targetDevice.name

        if (!updateTargetStateRowVisibility(view)) {
            setTargetState(getString(R.string.unknown), view)
        }
    }

    private fun updateTargetStateRowVisibility(view: View?): Boolean {
        if (view == null) return false

        val targetDeviceRow = view.findViewById<TableRow>(R.id.targetStateRow)
        return if (targetDevice == null) {
            targetDeviceRow.visibility = View.GONE
            false
        } else {
            targetDeviceRow.visibility = View.VISIBLE
            true
        }
    }

    private fun setTimerDeviceValuesForName(timerDeviceName: String) {
        checkNotNull(timerDeviceName)
        val myActivity = activity ?: return

        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                atService.getTimerDeviceFor(timerDeviceName)
            }?.let {
                setValuesForCurrentTimerDevice(it)
                myActivity.sendBroadcast(Intent(DISMISS_EXECUTING_DIALOG))
            }
        }
    }

    private fun setValuesForCurrentTimerDevice(device: TimerDevice) {
        this.timerDevice = device
        updateTimerInformation(timerDevice)

        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                deviceListService.getDeviceForName(device.definition.targetDeviceName)
            }?.let {
                updateTargetDevice(it, this@TimerDetailFragment.view)
            }
        }
    }

    private fun updateTimerInformation(timerDevice: TimerDevice?) {
        val view = view
        view ?: return
        timerDevice ?: return

        val definition = timerDevice.definition
        setType(definition.type, view)
        setRepetition(definition.repetition, view)
        view.isActive.isChecked = timerDevice.isActive
        setSwitchTime(definition.switchTime, view)
        setTimerName(timerDevice.name, view)
        setTargetState(listOfNotNull(definition.targetState, definition.targetStateAppendix).joinToString(" "), view)
    }

    private fun setTimerName(timerDeviceName: String, view: View) {
        view.timerNameInput.setText(timerDeviceName)
    }

    private fun getTimerName(view: View): String? = StringUtils.trimToNull(view.timerNameInput.text.toString())

    private fun setTargetState(targetState: String, view: View) {
        view.targetState.setText(targetState)
    }

    private fun getTargetState(view: View): String = view.targetState.text.toString()

    private fun setSwitchTime(switchTime: LocalTime, view: View) {
        view.switchTimeContent.text = getFormattedValue(switchTime.hourOfDay, switchTime.minuteOfHour, switchTime.secondOfMinute)
    }

    private fun getSwitchTime(view: View): LocalTime? {
        val text = view.switchTimeContent.text.toString()
        val parts = text.split(":").toList()
        if (parts.size != 3) {
            return null
        }
        return LocalTime(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]))
    }

    private fun setRepetition(repetition: AtRepetition, view: View) {
        view.timerRepetition.setSelection(repetition.ordinal)
    }

    private fun getRepetition(view: View): AtRepetition =
            AtRepetition.values()[view.timerRepetition.selectedItemPosition]

    private fun setType(type: TimerType, view: View) {
        view.timerType.setSelection(type.ordinal)
    }

    private fun getType(view: View): TimerType =
            TimerType.values()[view.timerType.selectedItemPosition]

    override suspend fun update(refresh: Boolean) {}

    override fun getTitle(context: Context) = context.getString(R.string.timer)

    private val isModify: Boolean
        get() = args.deviceName?.isNotEmpty() ?: false

    companion object {
        private val DEVICE_FILTER = object : DeviceNameListFragment.DeviceFilter {
            override fun isSelectable(device: FhemDevice): Boolean = device.setList.size() > 0
        }
    }
}
