/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.hwvtep;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.utils.hwvtep.HwvtepNodeHACache;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class HwvtepClusteredDataTreeChangeListener<
    T extends DataObject, K extends ClusteredDataTreeChangeListener<T>>
        extends AsyncClusteredDataTreeChangeListenerBase<T, K> {

    private final HwvtepNodeHACache hwvtepNodeHACache;

    public HwvtepClusteredDataTreeChangeListener(Class<T> clazz, Class<K> eventClazz,
            HwvtepNodeHACache hwvtepNodeHACache) {
        super(clazz, eventClazz);
        this.hwvtepNodeHACache = hwvtepNodeHACache;
    }

    @Override
    protected void remove(InstanceIdentifier<T> identifier, T del) {
        if (hwvtepNodeHACache.isHAEnabledDevice(identifier)) {
            return;
        }
        removed(identifier,del);
    }

    @Override
    protected void update(InstanceIdentifier<T> identifier, T original, T update) {
        if (hwvtepNodeHACache.isHAEnabledDevice(identifier)) {
            return;
        }
        updated(identifier,original, update);
    }

    @Override
    protected void add(InstanceIdentifier<T> identifier, T add) {
        if (hwvtepNodeHACache.isHAEnabledDevice(identifier)) {
            return;
        }
        added(identifier, add);
    }

    protected abstract void added(InstanceIdentifier<T> identifier, T add);

    protected abstract void removed(InstanceIdentifier<T> identifier, T del);

    protected abstract void updated(InstanceIdentifier<T> identifier, T original, T update);


}
