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

package li.klass.fhem.appwidget.update

import android.content.Context
import com.google.common.base.Optional
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import li.klass.fhem.appwidget.ui.widget.base.DeviceAppWidgetView
import li.klass.fhem.settings.SettingsKeys
import li.klass.fhem.update.backend.DeviceListUpdateService
import li.klass.fhem.util.ApplicationProperties
import org.jetbrains.anko.coroutines.experimental.bg
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AppWidgetUpdateService @Inject constructor(
        private val appWidgetInstanceManager: AppWidgetInstanceManager,
        private val appWidgetSchedulingService: AppWidgetSchedulingService,
        private val applicationProperties: ApplicationProperties,
        private val deviceListUpdateService: DeviceListUpdateService
) {

    fun updateAllWidgets() {
        appWidgetInstanceManager.getAllAppWidgetIds()
                .forEach { updateWidget(it) }
    }

    fun updateWidget(appWidgetId: Int) {
        appWidgetInstanceManager.update(appWidgetId)
    }

    fun doRemoteUpdate(context: Context, appWidgetId: Int, callback: () -> Unit) {
        val configuration = appWidgetInstanceManager.getConfigurationFor(appWidgetId)
        val allowRemoteUpdates = applicationProperties.getBooleanSharedPreference(SettingsKeys.ALLOW_REMOTE_UPDATE, true)
        val connectionId = configuration?.connectionId?.orNull()
        if (configuration == null || !allowRemoteUpdates) {
            callback()
            return
        }

        LOG.info("doRemoteUpdate - updating data for widget-id {}, connectionId={}", appWidgetId, connectionId)

        async(UI) {
            bg {
                if (configuration.widgetType.widgetView is DeviceAppWidgetView) {
                    val deviceName = configuration.widgetType.widgetView.deviceNameFrom(configuration)
                    handleDeviceUpdate(deviceName, connectionId, context)
                } else {
                    handleOtherUpdate(connectionId, context)
                }
            }.await()
            callback()
        }
    }

    private fun handleOtherUpdate(connectionId: String?, context: Context) {
        if (appWidgetSchedulingService.shouldUpdateDeviceList(connectionId)) {
            deviceListUpdateService.updateAllDevices(Optional.fromNullable(connectionId), context)
        } else {
            LOG.info("handleOtherUpdate - skipping update, as device list is recent enough")
        }
    }

    private fun handleDeviceUpdate(deviceName: String, connectionId: String?, context: Context) {
        if (appWidgetSchedulingService.shouldUpdateDevice(connectionId, deviceName)) {
            deviceListUpdateService.updateSingleDevice(deviceName, Optional.fromNullable(connectionId), context)
        } else {
            LOG.info("handleDeviceUpdate(deviceName=$deviceName) - skipping update, as device list is recent enough")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AppWidgetUpdateIntentService::class.java)!!
    }
}