/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.networkutils;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;

public interface VniUtils {

    BigInteger getVNI(String vniKey) throws ExecutionException, InterruptedException;

    void releaseVNI(String vniKey) throws ExecutionException, InterruptedException;

    Optional<IdPool> getVxlanVniPool() throws ReadFailedException;
}