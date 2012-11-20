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

package li.klass.fhem.domain.core;

import static li.klass.fhem.domain.core.ToggleableDevice.ButtonHookType.*;

@SuppressWarnings("unused")
public abstract class ToggleableDevice<T extends Device> extends Device<T> {

    private boolean doInvertOnState = false;

    public enum ButtonHookType {
        NORMAL, ON_OFF_DEVICE, ON_DEVICE, OFF_DEVICE, TOGGLE_DEVICE
    }

    private ButtonHookType buttonHookType = NORMAL;

    public abstract boolean isOnByState();

    public boolean isOnRespectingInvertHook() {
        boolean isOn = isOnByState();
        if (doInvertOnState) isOn = ! isOn;

        return isOn;
    }

    public abstract boolean supportsToggle();

    public void readONOFFDEVICE(String value) {
        ButtonHookType target = ON_OFF_DEVICE;
        readButtonHookType(value, target);
    }

    public void readONDEVICE(String value) {
        readButtonHookType(value, ON_DEVICE);
    }

    public void readOFFDEVICE(String value) {
        readButtonHookType(value, OFF_DEVICE);
    }

    public void readTOGGLEDEVICE(String value) {
        readButtonHookType(value, TOGGLE_DEVICE);
    }

    private void readButtonHookType(String value, ButtonHookType target) {
        if (value.equalsIgnoreCase("true")) {
            buttonHookType = target;
        }
    }

    public void readINVERTSTATE(String value) {
       if (value.equalsIgnoreCase("true")) {
           doInvertOnState = true;
       }
    }

    public ButtonHookType getButtonHookType() {
        return buttonHookType;
    }

    public boolean isSpecialButtonDevice() {
        return buttonHookType != NORMAL;
    }

    public String getOffStateName() {
        return "off";
    }

    public String getOnStateName() {
        return "on";
    }
}
