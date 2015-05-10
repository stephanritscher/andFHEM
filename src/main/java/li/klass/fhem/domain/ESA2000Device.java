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

package li.klass.fhem.domain;

import li.klass.fhem.domain.core.DeviceFunctionality;
import li.klass.fhem.domain.core.FhemDevice;
import li.klass.fhem.domain.core.XmllistAttribute;
import li.klass.fhem.domain.genericview.ShowField;
import li.klass.fhem.resources.ResourceIdMapper;

@SuppressWarnings("unused")
public class ESA2000Device extends FhemDevice<ESA2000Device> {
    @ShowField(description = ResourceIdMapper.currentUsage, showInOverview = true)
    @XmllistAttribute("actual")
    private String current;

    @ShowField(description = ResourceIdMapper.dayUsage, showInOverview = true)
    @XmllistAttribute("day")
    private String day;

    @ShowField(description = ResourceIdMapper.monthUsage, showInOverview = true)
    @XmllistAttribute("month")
    private String month;

    @ShowField(description = ResourceIdMapper.yearUsage, showInOverview = true)
    @XmllistAttribute("year")
    private String year;

    @ShowField(description = ResourceIdMapper.dayLastUsage, showInOverview = false)
    @XmllistAttribute("day_last")
    private String dayLast;

    public String getCurrent() {
        return current;
    }

    public String getDay() {
        return day;
    }

    public String getMonth() {
        return month;
    }

    public String getYear() {
        return year;
    }

    public String getDayLast() {
        return dayLast;
    }

    @Override
    public DeviceFunctionality getDeviceGroup() {
        return DeviceFunctionality.USAGE;
    }


    @Override
    public boolean isSensorDevice() {
        return true;
    }

    @Override
    public long getTimeRequiredForStateError() {
        return OUTDATED_DATA_MS_DEFAULT;
    }
}