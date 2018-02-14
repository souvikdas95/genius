/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.metrics;

import org.opendaylight.infrautils.metrics.Meter;
import org.opendaylight.infrautils.metrics.MetricDescriptor;
import org.opendaylight.infrautils.metrics.MetricProvider;

public class DataStoreMetrics {

    private final MetricProvider metricProvider;
    private final Class<?> clazz;
    private final Meter added;
    private final Meter updated;
    private final Meter deleted;

    public DataStoreMetrics(MetricProvider metricProvider, Class<?> clazz) {
        this.metricProvider = metricProvider;
        this.clazz = clazz;
        this.added = initCounter("_added");
        this.updated = initCounter("_updated");
        this.deleted = initCounter("_deleted");
    }

    public void incrementAdded() {
        added.mark();
    }

    public void incrementUpdated() {
        updated.mark();
    }

    public void incrementDeleted() {
        deleted.mark();
    }

    private Meter initCounter(String type) {
        String className = clazz.getSimpleName();
        // expects the form org.opendaylight.project.module
        String project = clazz.getName().split("\\.")[2];
        String module = clazz.getName().split("\\.")[3];
        return metricProvider.newMeter(new MetricDescriptor() {
            @Override
            public Object anchor() {
                return this;
            }

            @Override
            public String project() {
                return project;
            }

            @Override
            public String module() {
                return module;
            }

            @Override
            public String id() {
                return className + type;
            }
        });
    }
}
