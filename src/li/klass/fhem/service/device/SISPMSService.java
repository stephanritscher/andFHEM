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

package li.klass.fhem.service.device;

import android.content.Context;
import li.klass.fhem.domain.SISPMSDevice;
import li.klass.fhem.service.CommandExecutionService;
import li.klass.fhem.service.ExecuteOnSuccess;

/**
 * Class accumulating SIS_PMS specific operations.
 */
public class SISPMSService {
    public static final SISPMSService INSTANCE = new SISPMSService();

    private SISPMSService() {
    }

    /**
     * Toggles the state of a SIS_PMS device.
     * @param context context in which the action was started.
     * @param device concerned device
     * @param executeOnSuccess action, which will be performed on success.
     */
    public void toggleState(Context context, SISPMSDevice device, ExecuteOnSuccess executeOnSuccess) {
        if (device.isOn()) {
            CommandExecutionService.INSTANCE.executeSafely(context, "set " + device.getName() + " off", executeOnSuccess);
            device.setState("off");
        } else {
            CommandExecutionService.INSTANCE.executeSafely(context, "set " + device.getName() + " on", executeOnSuccess);
            device.setState("on");
        }
    }
}
