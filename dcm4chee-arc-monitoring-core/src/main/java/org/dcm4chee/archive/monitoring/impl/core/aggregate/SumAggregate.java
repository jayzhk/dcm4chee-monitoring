//
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.monitoring.impl.core.aggregate;

import java.util.List;
import java.util.Map;

import org.dcm4chee.archive.monitoring.impl.core.AbstractMetric;
import org.dcm4chee.archive.monitoring.impl.core.Util;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoir;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;
import org.dcm4chee.archive.monitoring.impl.util.LongAdder;



/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class SumAggregate extends AbstractMetric implements Aggregate {
	private final MonitoringContext context;
	private final Aggregate aggregate;

	private final LongAdder sum = new LongAdder();
	private final AggregatedReservoir reservoir;
	
	public SumAggregate(MonitoringContext context, Aggregate aggregate, AggregatedReservoir reservoir) {
		this.context = context;
		this.aggregate = aggregate;
		this.reservoir = reservoir;
	}
	
	@Override
	public void update(MonitoringContext context, long now, long value) {
		sum.add(value);
		long sum = this.sum.sum();
		reservoir.update(context, now, sum);
		if (aggregate != null) {
			//TODO propagate original context or context of this aggregate?
			aggregate.update(context, now, value);
		}
	}
	
	@Override
	public AggregateSnapshot getSnapshot() {
		AggregateSnapshotImpl snapshot = new AggregateSnapshotImpl();
		
		AggregatedReservoirSnapshot primarySnapshot = reservoir.getCurrentSnapshot();
		snapshot.setValues(primarySnapshot.getValues(false));
		snapshot.setSize(primarySnapshot.size());
		snapshot.setMean(primarySnapshot.getMean());
		snapshot.setStdDev(primarySnapshot.getStdDev());
		snapshot.setMin(primarySnapshot.getMin());
		snapshot.setMax(primarySnapshot.getMax());
		snapshot.setFirstUsageTimestamp(primarySnapshot.getFirstUsageTimestamp());
		snapshot.setLastUsageTimestamp(primarySnapshot.getLastUsageTimestamp());
		snapshot.setMinTimestamp(primarySnapshot.getMinTimestamp());
		snapshot.setMaxTimestamp(primarySnapshot.getMaxTimestamp());
		
		snapshot.setPath(Util.createPath(context.getPath()));
	    snapshot.setAttributes(getAttributes(true));
	    
		return snapshot;
	}
	    
	@Override
	public List<AggregatedReservoirSnapshot> getSnapshots(long start, long end, long resolution) {
		List<AggregatedReservoirSnapshot> reservoirSnapshots = reservoir.getSnapshots(start, end, resolution);

		// augment snapshots with path & attributes
        if (!reservoirSnapshots.isEmpty()) {
            String path = Util.createPath(context.getPath());
            Map<String,Object> attrs = getAttributes(true);
            for (AggregatedReservoirSnapshot reservoirSnapshot : reservoirSnapshots) {
                reservoirSnapshot.setPath(path);
                reservoirSnapshot.setAttributes(attrs);
            } 
        }
        
		return reservoirSnapshots;
	}

}
