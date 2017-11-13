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

package li.klass.fhem.devices.detail.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ScrollView
import android.widget.Toast
import com.google.common.base.Optional
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import li.klass.fhem.R
import li.klass.fhem.constants.Actions.DISMISS_EXECUTING_DIALOG
import li.klass.fhem.constants.Actions.SHOW_EXECUTING_DIALOG
import li.klass.fhem.constants.BundleExtraKeys.*
import li.klass.fhem.dagger.ApplicationComponent
import li.klass.fhem.devices.list.favorites.backend.FavoritesService
import li.klass.fhem.domain.core.DeviceType
import li.klass.fhem.domain.core.FhemDevice
import li.klass.fhem.fragments.core.BaseFragment
import li.klass.fhem.graph.backend.GraphDefinitionsForDeviceService
import li.klass.fhem.service.advertisement.AdvertisementService
import li.klass.fhem.update.backend.RoomListService
import li.klass.fhem.update.backend.RoomListUpdateService
import li.klass.fhem.util.device.DeviceActionUtil
import li.klass.fhem.widget.notification.NotificationSettingView
import org.jetbrains.anko.coroutines.experimental.bg
import javax.inject.Inject

class DeviceDetailFragment : BaseFragment() {
    @Inject
    lateinit var favoritesService: FavoritesService
    @Inject
    lateinit var advertisementService: AdvertisementService
    @Inject
    lateinit var roomListUpdateService: RoomListUpdateService
    @Inject
    lateinit var roomListService: RoomListService
    @Inject
    lateinit var graphDefinitionsForDeviceService: GraphDefinitionsForDeviceService

    private var deviceName: String? = null
    private var device: FhemDevice? = null
    private var connectionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun setArguments(args: Bundle) {
        super.setArguments(args)
        setArgumentsFrom(args)
    }

    private fun setArgumentsFrom(args: Bundle?) {
        if (args == null) {
            return
        }
        deviceName = args.getString(DEVICE_NAME)
        connectionId = args.getString(CONNECTION_ID)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState!!.putString(DEVICE_NAME, deviceName)
        outState.putString(CONNECTION_ID, connectionId)
    }

    override fun inject(applicationComponent: ApplicationComponent) {
        applicationComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setArgumentsFrom(savedInstanceState)
        val superView = super.onCreateView(inflater, container, savedInstanceState)
        if (superView != null) return superView

        val view = inflater!!.inflate(R.layout.device_detail_view, container, false)
        advertisementService.addAd(view, activity)

        return view
    }

    override fun update(refresh: Boolean) {
        hideEmptyView()
        deviceName ?: return

        if (refresh) activity.sendBroadcast(Intent(SHOW_EXECUTING_DIALOG))
        async(UI) {
            val device = bg {
                if (refresh) {
                    roomListUpdateService.updateSingleDevice(deviceName!!, Optional.fromNullable(connectionId), activity)
                }
                roomListService.getDeviceForName<FhemDevice>(deviceName, Optional.fromNullable(connectionId), activity)
            }.await()
            activity.sendBroadcast(Intent(DISMISS_EXECUTING_DIALOG))
            if (device.isPresent) {
                this@DeviceDetailFragment.device = device.get()
                val adapter = DeviceType.getAdapterFor<FhemDevice>(device.get())
                if (adapter != null) {
                    activity.invalidateOptionsMenu()
                    val scrollView = findScrollView()
                    if (scrollView != null) {
                        scrollView.removeAllViews()
                        scrollView.addView(adapter.createDetailView(activity, device.get(), emptySet(), connectionId))
                    }
                    loadGraphs()
                }
            }
        }
    }

    private fun loadGraphs() {
        device ?: return
        val myContext = context

        async(UI) {
            val graphs = bg {
                graphDefinitionsForDeviceService.graphDefinitionsFor(myContext, device!!.xmlListDevice, Optional.absent())
            }.await()

            val detailView = findScrollView()!!.getChildAt(0)
            val adapter = DeviceType.getAdapterFor<FhemDevice>(device)
            adapter?.attachGraphs(activity, detailView, graphs, connectionId, device)
            detailView.invalidate()
        }
    }

    private fun findScrollView(): ScrollView? = view!!.findViewById(R.id.deviceDetailView)

    override fun getTitle(context: Context): CharSequence? {
        var name = arguments.getString(DEVICE_DISPLAY_NAME)
        if (name == null) {
            name = arguments.getString(DEVICE_NAME)
        }
        return name
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (device != null) {
            inflater!!.inflate(R.menu.device_menu, menu)
            if (favoritesService.isFavorite(deviceName ?: "", activity)) {
                menu!!.removeItem(R.id.menu_favorites_add)
            } else {
                menu!!.removeItem(R.id.menu_favorites_remove)
            }
            menu.removeItem(R.id.menu_rename)
            menu.removeItem(R.id.menu_delete)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {

            R.id.menu_favorites_add -> {
                callUpdating(favoritesService::addFavorite, R.string.context_favoriteadded)
            }
            R.id.menu_favorites_remove -> {
                callUpdating(favoritesService::removeFavorite, R.string.context_favoriteremoved)
            }
            R.id.menu_room -> DeviceActionUtil.moveDevice(activity, device)
            R.id.menu_alias -> DeviceActionUtil.setAlias(activity, device)
            R.id.menu_notification -> NotificationSettingView(activity, deviceName).show(activity)
            else -> return false
        }
        return super.onOptionsItemSelected(item)
    }

    private fun callUpdating(actionToCall: (Context, String) -> Unit, toastStringId: Int) {
        deviceName ?: return
        async(UI) {
            bg {
                actionToCall(activity, deviceName!!)
            }.await()
            showToast(toastStringId)
            update(false)
        }
    }

    private fun showToast(textStringId: Int) {
        Toast.makeText(activity, textStringId, Toast.LENGTH_SHORT).show()
    }

    override fun canChildScrollUp(): Boolean =
            super.canChildScrollUp() || findScrollView()!!.scrollY > 0
}
