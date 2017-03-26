/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.interfaces;

import java.math.BigInteger;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.exceptions.InterfaceAlreadyExistsException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

public interface IInterfaceManager {

    Long getPortForInterface(String ifName);

    Long getPortForInterface(Interface intf);

    BigInteger getDpnForInterface(String ifName);

    BigInteger getDpnForInterface(Interface intrf);

    String getEndpointIpForDpn(BigInteger dpnId);

    List<ActionInfo> getInterfaceEgressActions(String ifName);

    InterfaceInfo getInterfaceInfo(String intInfo);

    InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName,
            InterfaceInfo.InterfaceType interfaceType);

    InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName);

    /**
     * This API is currently used only for CLI usage. Please be careful that this API
     * can return stale entries since it is just a cache read.
     */
    InterfaceInfo getInterfaceInfoFromOperationalDSCache(String interfaceName);

    Interface getInterfaceInfoFromConfigDataStore(String interfaceName);

    void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId, String description,
            IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException;

    void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId, String description,
            IfL2vlan.L2vlanMode l2vlanMode, boolean isExternal) throws InterfaceAlreadyExistsException;

    boolean isServiceBoundOnInterfaceForIngress(short servicePriority, String interfaceName);

    boolean isServiceBoundOnInterfaceForEgress(short servicePriority, String interfaceName);

    void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo);

    void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo,
                     WriteTransaction tx);

    void unbindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo);

    List<Interface> getVlanInterfaces();

    List<Interface> getVxlanInterfaces();

    List<Interface> getChildInterfaces(String parentInterface);

    boolean isExternalInterface(String interfaceName);

    String getPortNameForInterface(NodeConnectorId nodeConnectorId, String interfaceName);

    String getPortNameForInterface(String dpnId, String interfaceName);

    String getParentRefNameForInterface(String interfaceName);

    void updateInterfaceParentRef(String interfaceName, String parentInterface);

    void updateInterfaceParentRef(String interfaceName, String parentInterface, boolean readInterfaceBeforeWrite);

}
