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

package li.klass.fhem.activities.graph;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

import li.klass.fhem.R;

import static li.klass.fhem.util.DateFormatUtil.ANDFHEM_DATE_TIME_FORMAT;

@SuppressLint("ViewConstructor")
public class ChartMarkerView extends MarkerView {

    public ChartMarkerView(Context context) {
        super(context, R.layout.chart_marker);
        setOffset(getXOffset(), getYOffset());
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        ((TextView) findViewById(R.id.time)).setText(ANDFHEM_DATE_TIME_FORMAT.print((long) e.getX()));
        ((TextView) findViewById(R.id.value)).setText(String.valueOf(e.getY()));
    }

    private int getXOffset() {
        return -(getWidth() / 2);
    }

    private int getYOffset() {
        return -getHeight();
    }
}
