/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.infrautils.testutils.Partials;

public abstract class TestItmProvider implements IITMProvider {

    public static IITMProvider newInstance() {
        return Partials.newPartial(IITMProvider.class);
    }
}
