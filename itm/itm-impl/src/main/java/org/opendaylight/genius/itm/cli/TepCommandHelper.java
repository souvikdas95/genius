/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.genius.itm.cache.UnprocessedTunnelsStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("checkstyle:RegexpSingleLineJava")
public class TepCommandHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TepCommandHelper.class);

    private static final AtomicInteger CHECK = new AtomicInteger();

    private final DataBroker dataBroker;
    private final RetryingManagedNewTransactionRunner txRunner;
    private final ItmConfig itmConfig;
    private final UnprocessedTunnelsStateCache unprocessedTunnelsStateCache;

    /*
     * boolean flag add_or_delete --- can be set to true if the last called tep
     * command is Tep-add else set to false when Tep-delete is called
     * tepCommandHelper object is created only once in session initiated
     */
    private final Map<String, Map<SubnetObject, List<Vteps>>> transportZonesHashMap = new HashMap<>();
    private List<Subnets> subnetList = new ArrayList<>();
    private List<TransportZone> transportZoneArrayList = new ArrayList<>();
    private final List<Vteps> vtepDelCommitList = new ArrayList<>();

    @Inject
    public TepCommandHelper(final DataBroker dataBroker, final ItmConfig itmConfig,
                            final UnprocessedTunnelsStateCache unprocessedTunnelsStateCache) {
        this.dataBroker = dataBroker;
        this.txRunner = new RetryingManagedNewTransactionRunner(dataBroker);
        this.itmConfig = itmConfig;
        this.unprocessedTunnelsStateCache = unprocessedTunnelsStateCache;
    }

    @PostConstruct
    public void start() throws ExecutionException, InterruptedException {
        LOG.info("TepCommandHelper Started");
    }

    @PreDestroy
    public void close() {
        LOG.info("TepCommandHelper Closed");
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void createLocalCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress,
                                 String subnetMask, String gatewayIp, String transportZone) throws TepException {

        CHECK.incrementAndGet();
        IpAddress ipAddressObj = null;
        IpAddress gatewayIpObj = null;
        IpPrefix subnetMaskObj = null;
        final VtepsKey vtepkey = new VtepsKey(dpnId, portName);
        try {
            ipAddressObj = IpAddressBuilder.getDefaultInstance(ipAddress);
            gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
            if (gatewayIp != null && !gatewayIp.isEmpty()
                    && !"null".equals(gatewayIp) || "0.0.0.0".equals(gatewayIp)) {
                gatewayIpObj = IpAddressBuilder.getDefaultInstance(gatewayIp);
            } else {
                LOG.debug("gateway is null");
                gatewayIp = null;
            }
        } catch (RuntimeException e) {
            handleError("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255");
            return;
        }
        try {
            subnetMaskObj = IpPrefixBuilder.getDefaultInstance(subnetMask);
        } catch (Exception e) {
            handleError("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32");
            return;
        }

        if (!validateIPs(ipAddress, subnetMask, gatewayIp)) {
            handleError("IpAddress and gateWayIp should belong to the subnet provided");
            return;
        }

        if (checkTepPerTzPerDpn(transportZone, dpnId)) {
            handleError("Only one end point per transport Zone per Dpn is allowed");
            return;
        }

        Vteps vtepCli = new VtepsBuilder().setDpnId(dpnId).setIpAddress(ipAddressObj).withKey(vtepkey)
                .setPortname(portName).build();
        validateForDuplicates(vtepCli, transportZone);

        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        SubnetObject subObCli = new SubnetObject(gatewayIpObj, subnetsKey, subnetMaskObj, vlanId);
        if (transportZonesHashMap.containsKey(transportZone)) {
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = transportZonesHashMap.get(transportZone);
            if (subVtepMapTemp.containsKey(subObCli)) { // if Subnet exists
                List<Vteps> vtepListTemp = subVtepMapTemp.get(subObCli);
                if (vtepListTemp.contains(vtepCli)) {
                    // do nothing
                } else {
                    vtepListTemp.add(vtepCli);
                }
            } else { // subnet doesnt exist
                if (checkExistingSubnet(subVtepMapTemp, subObCli)) {
                    handleError("Subnet with subnet mask " + subObCli.get_key() + " already exists");
                    return;
                }
                List<Vteps> vtepListTemp = new ArrayList<>();
                vtepListTemp.add(vtepCli);
                subVtepMapTemp.put(subObCli, vtepListTemp);
            }
        } else {
            List<Vteps> vtepListTemp = new ArrayList<>();
            vtepListTemp.add(vtepCli);
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = new HashMap<>();
            subVtepMapTemp.put(subObCli, vtepListTemp);
            transportZonesHashMap.put(transportZone, subVtepMapTemp);
        }
    }

    private boolean validateIPs(String ipAddress, String subnetMask, String gatewayIp) {
        SubnetUtils utils = new SubnetUtils(subnetMask);
        if (utils.getInfo().isInRange(ipAddress) && (gatewayIp == null || utils.getInfo().isInRange(gatewayIp))) {
            return true;
        } else {
            LOG.trace("InValid IP");
            return false;
        }
    }

    /**
     * Validate for duplicates.
     *
     * @param inputVtep
     *            the input vtep
     * @param transportZone
     *            the transport zone
     */
    public void validateForDuplicates(Vteps inputVtep, String transportZone) {
        Map<String, TransportZone> allTransportZonesAsMap = getAllTransportZonesAsMap();

        boolean isConfiguredTepGreType = isGreTunnelType(transportZone, allTransportZonesAsMap);
        // Checking for duplicates in local cache
        for (Entry<String, Map<SubnetObject, List<Vteps>>> entry : transportZonesHashMap.entrySet()) {
            String tz = entry.getKey();
            boolean isGreType = isGreTunnelType(tz, allTransportZonesAsMap);
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = entry.getValue();
            for (List<Vteps> vtepList : subVtepMapTemp.values()) {
                validateForDuplicateAndSingleGreTep(inputVtep, isConfiguredTepGreType, isGreType, vtepList);
            }
        }
        // Checking for duplicates in config DS
        for (TransportZone tz : allTransportZonesAsMap.values()) {
            boolean isGreType = false;
            if (TunnelTypeGre.class.equals(tz.getTunnelType())) {
                isGreType = true;
            }
            for (Subnets sub : ItmUtils.emptyIfNull(tz.getSubnets())) {
                List<Vteps> vtepList = sub.getVteps();
                validateForDuplicateAndSingleGreTep(inputVtep, isConfiguredTepGreType, isGreType, vtepList);
            }
        }
    }

    private void validateForDuplicateAndSingleGreTep(Vteps inputVtep, boolean isConfiguredTepGreType, boolean isGreType,
            List<Vteps> vtepList) {
        if (ItmUtils.isEmpty(vtepList)) {
            return;
        }
        Preconditions.checkArgument(!vtepList.contains(inputVtep), "VTEP already exists");
        BigInteger dpnId = inputVtep.getDpnId();
        if (isConfiguredTepGreType && isGreType) {
            for (Vteps vtep : vtepList) {
                Preconditions.checkArgument(!Objects.equals(vtep.getDpnId(), dpnId),
                    "DPN [" + dpnId + "] already configured with GRE TEP."
                        + " Mutiple GRE TEP's on a single DPN are not allowed.");
            }
        }
    }

    /**
     * Gets all transport zones as map.
     *
     * @return all transport zones as map
     */
    private Map<String, TransportZone> getAllTransportZonesAsMap() {
        TransportZones allTransportZones = getAllTransportZones();
        Map<String, TransportZone> transportZoneMap = new HashMap<>();
        if (null != allTransportZones) {
            for (TransportZone tzone : ItmUtils.emptyIfNull(allTransportZones.getTransportZone())) {
                transportZoneMap.put(tzone.getZoneName(), tzone);
            }
        }
        return transportZoneMap;
    }

    /**
     * Checks if is gre tunnel type.
     *
     * @param transportZoneName
     *            the zone name
     * @param trsnsportZoneMap
     *            the zone map
     * @return true, if is gre tunnel type
     */
    private boolean isGreTunnelType(String transportZoneName, Map<String, TransportZone> trsnsportZoneMap) {
        TransportZone tzone = trsnsportZoneMap.get(transportZoneName);
        /*
         * if (tzone != null &&
         * StringUtils.equalsIgnoreCase(ITMConstants.TUNNEL_TYPE_GRE,
         * tzone.getTunnelType())) { return true; }
         */
        return tzone != null && TunnelTypeGre.class.equals(tzone.getTunnelType());
    }

    /**
     * Gets the transport zone.
     *
     * @param transportZoneName
     *            the tzone
     * @return the transport zone
     */
    public TransportZone getTransportZone(String transportZoneName) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(transportZoneName)).build();
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker).orNull();
    }

    /**
     * Gets all transport zones.
     *
     * @return all transport zones
     */
    public TransportZones getAllTransportZones() {
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker).orNull();
    }

    public boolean checkExistingSubnet(Map<SubnetObject, List<Vteps>> subVtepMapTemp, SubnetObject subObCli) {
        for (SubnetObject subOb : subVtepMapTemp.keySet()) {
            if (subOb.get_key().equals(subObCli.get_key())) {
                if (!subOb.get_vlanId().equals(subObCli.get_vlanId())) {
                    return true;
                }
                if (!subOb.get_gatewayIp().equals(subObCli.get_gatewayIp())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkTepPerTzPerDpn(String tzone, BigInteger dpnId) {
        // check in local cache
        if (transportZonesHashMap.containsKey(tzone)) {
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = transportZonesHashMap.get(tzone);
            for (List<Vteps> vtepList : subVtepMapTemp.values()) {
                for (Vteps vtep : vtepList) {
                    if (Objects.equals(vtep.getDpnId(), dpnId)) {
                        return true;
                    }
                }
            }
        }

        // check in DS
        InstanceIdentifier<TransportZone> tzonePath =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> transportZoneOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker);
        if (transportZoneOptional.isPresent()) {
            TransportZone tz = transportZoneOptional.get();
            if (tz.getSubnets() == null || tz.getSubnets().isEmpty()) {
                return false;
            }
            for (Subnets sub : tz.getSubnets()) {
                if (sub.getVteps() == null || sub.getVteps().isEmpty()) {
                    continue;
                }
                for (Vteps vtep : sub.getVteps()) {
                    if (Objects.equals(vtep.getDpnId(), dpnId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void buildTeps() {
        TransportZone transportZone = null;
        try {
            LOG.debug("no of teps added {}", CHECK);
            if (transportZonesHashMap != null && !transportZonesHashMap.isEmpty()) {
                transportZoneArrayList = new ArrayList<>();
                for (Entry<String, Map<SubnetObject, List<Vteps>>> mapEntry : transportZonesHashMap.entrySet()) {
                    String tz = mapEntry.getKey();
                    LOG.debug("transportZonesHashMap {}", tz);
                    subnetList = new ArrayList<>();
                    Map<SubnetObject, List<Vteps>> subVtepMapTemp = mapEntry.getValue();
                    for (Entry<SubnetObject, List<Vteps>> entry : subVtepMapTemp.entrySet()) {
                        SubnetObject subOb = entry.getKey();
                        LOG.debug("subnets {}", subOb.get_prefix());
                        List<Vteps> vtepList = entry.getValue();
                        Subnets subnet = new SubnetsBuilder().setGatewayIp(subOb.get_gatewayIp())
                                .withKey(subOb.get_key()).setPrefix(subOb.get_prefix()).setVlanId(subOb.get_vlanId())
                                .setVteps(vtepList).build();
                        subnetList.add(subnet);
                        LOG.debug("vteps {}", vtepList);
                    }
                    InstanceIdentifier<TransportZone> transportZonePath =
                            InstanceIdentifier.builder(TransportZones.class)
                                    .child(TransportZone.class, new TransportZoneKey(tz)).build();
                    Optional<TransportZone> transportZoneOptional =
                            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, transportZonePath, dataBroker);
                    LOG.debug("read container from DS");
                    if (transportZoneOptional.isPresent()) {
                        TransportZone tzoneFromDs = transportZoneOptional.get();
                        LOG.debug("read tzone container {}", tzoneFromDs);
                        if (tzoneFromDs.getTunnelType() == null
                                || tzoneFromDs.getTunnelType().equals(TunnelTypeVxlan.class)) {
                            transportZone =
                                    new TransportZoneBuilder().withKey(new TransportZoneKey(tz))
                                            .setTunnelType(TunnelTypeVxlan.class).setSubnets(subnetList)
                                            .setZoneName(tz).build();
                        } else if (tzoneFromDs.getTunnelType().equals(TunnelTypeGre.class)) {
                            transportZone =
                                    new TransportZoneBuilder().withKey(new TransportZoneKey(tz))
                                            .setTunnelType(TunnelTypeGre.class).setSubnets(subnetList)
                                            .setZoneName(tz).build();
                        }
                    } else {
                        transportZone =
                                new TransportZoneBuilder().withKey(new TransportZoneKey(tz))
                                        .setTunnelType(TunnelTypeVxlan.class).setSubnets(subnetList).setZoneName(tz)
                                        .build();
                    }
                    LOG.debug("tzone object {}", transportZone);
                    transportZoneArrayList.add(transportZone);
                }
                TransportZones transportZones =
                    new TransportZonesBuilder().setTransportZone(transportZoneArrayList).build();
                InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
                LOG.debug("InstanceIdentifier {}", path);
                Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                    tx -> tx.merge(path, transportZones, true)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                        MoreExecutors.directExecutor());
                LOG.debug("wrote to Config DS {}", transportZones);
                transportZonesHashMap.clear();
                transportZoneArrayList.clear();
                subnetList.clear();
                LOG.debug("Everything cleared");
            } else {
                LOG.debug("NO vteps were configured");
            }
        } catch (RuntimeException e) {
            LOG.error("Error building TEPs", e);
        }
    }

    public List<String> showTeps(boolean monitorEnabled, int monitorInterval) throws TepException {
        boolean flag = false;
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, dataBroker);
        if (transportZonesOptional.isPresent()) {
            List<String> result = new ArrayList<>();
            TransportZones transportZones = transportZonesOptional.get();
            if (transportZones.getTransportZone() == null || transportZones.getTransportZone().isEmpty()) {
                handleError("No teps configured");
                return result;
            }
            result.add(String.format("Tunnel Monitoring (for VXLAN tunnels): %s", monitorEnabled ? "On" : "Off"));
            result.add(String.format("Tunnel Monitoring Interval (for VXLAN tunnels): %d", monitorInterval));
            result.add(System.lineSeparator());
            result.add(String.format("%-16s  %-16s  %-16s  %-12s  %-12s %-12s %-16s %-12s", "TransportZone",
                    "TunnelType", "SubnetMask", "GatewayIP", "VlanID", "DpnID", "IPAddress", "PortName"));
            result.add("---------------------------------------------------------------------------------------------"
                    + "---------------------------------");
            for (TransportZone tz : transportZones.getTransportZone()) {
                if (tz.getSubnets() == null || tz.getSubnets().isEmpty()) {
                    continue;
                }
                for (Subnets sub : tz.getSubnets()) {
                    if (sub.getVteps() == null || sub.getVteps().isEmpty()) {
                        continue;
                    }
                    for (Vteps vtep : sub.getVteps()) {
                        flag = true;
                        String strTunnelType ;
                        if (TunnelTypeGre.class.equals(tz.getTunnelType())) {
                            strTunnelType = ITMConstants.TUNNEL_TYPE_GRE;
                        } else {
                            strTunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                        }
                        result.add(String.format("%-16s  %-16s  %-16s  %-12s  %-12s %-12s %-16s %-12s",
                                tz.getZoneName(), strTunnelType, sub.getPrefix().stringValue(),
                                sub.getGatewayIp().stringValue(), sub.getVlanId().toString(),
                                vtep.getDpnId().toString(), vtep.getIpAddress().stringValue(),
                                vtep.getPortname()));
                    }
                }
            }
            if (flag) {
                return result;
            } else {
                return Collections.singletonList("No teps to display");
            }
        } else {
            return Collections.singletonList("No teps configured");
        }
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    public void showCache(String cacheName) {
        boolean dataStoreCache = DataStoreCache.isCacheValid(cacheName);
        boolean inMemoryCache = isInMemoryCacheNameValid(cacheName);
        if (!dataStoreCache && !inMemoryCache) {
            System.out.println(" " + cacheName + " is not a valid Cache Name ");
            return ;
        }
        if (dataStoreCache) {
            List<Object> keys = null;
            keys = DataStoreCache.getKeys(cacheName);
            if (keys != null && !keys.isEmpty()) {
                System.out.println("Dumping the data in cache for " + cacheName);
                for (Object key : keys) {
                    System.out.println(" KEY:  " + key + " Value: " + DataStoreCache.get(cacheName, key));
                }
            } else {
                System.out.println("No data in cache for " + cacheName);
            }
        } else if (inMemoryCache) {
            System.out.println("Dumping the data in cache for " + cacheName);
            Collection<String> cacheContent;
            switch (cacheName) {
                case ITMConstants.INTERNAL_TUNNEL_CACHE_NAME:
                    cacheContent = ItmUtils.ITM_CACHE.getAllInternalInterfaces();
                    break;
                case ITMConstants.EXTERNAL_TUNNEL_CACHE_NAME:
                    cacheContent = ItmUtils.ITM_CACHE.getAllExternalInterfaces();
                    break;
                case ITMConstants.UNPROCESSED_TUNNELS_CACHE_NAME:
                    cacheContent = unprocessedTunnelsStateCache.getAllUnprocessedTunnels();
                    break;
                default:
                    cacheContent = Collections.emptyList();
            }
            System.out.println("Number of data in cache " + cacheContent.size());
            if (!cacheContent.isEmpty()) {
                for (String key : cacheContent) {
                    System.out.println(key + " ");
                }
            } else {
                System.out.println("No data in cache for " + cacheName);
            }
        }
    }

    public boolean isInMemoryCacheNameValid(String name) {
        boolean valid = false;
        valid = name.equals(ITMConstants.INTERNAL_TUNNEL_CACHE_NAME)
                || name.equals(ITMConstants.EXTERNAL_TUNNEL_CACHE_NAME)
                || name.equals(ITMConstants.UNPROCESSED_TUNNELS_CACHE_NAME);
        return valid;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void deleteVtep(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone) throws TepException {

        IpAddress ipAddressObj = null;
        IpAddress gatewayIpObj = null;
        IpPrefix subnetMaskObj = null;
        final VtepsKey vtepkey = new VtepsKey(dpnId, portName);
        try {
            ipAddressObj = IpAddressBuilder.getDefaultInstance(ipAddress);
            gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
            if (!"null".equals(gatewayIp) || "0.0.0.0".equals(gatewayIp) && gatewayIp != null) {
                gatewayIpObj = IpAddressBuilder.getDefaultInstance(gatewayIp);
            } else {
                LOG.debug("gateway is null");
                gatewayIp = null;
            }
        } catch (RuntimeException e) {
            handleError("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255");
            return;
        }
        try {
            subnetMaskObj = IpPrefixBuilder.getDefaultInstance(subnetMask);
        } catch (Exception e) {
            handleError("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32");
            return;
        }

        if (!validateIPs(ipAddress, subnetMask, gatewayIp)) {
            handleError("IpAddress and gateWayIp should belong to the subnet provided");
            return;
        }
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        Vteps vtepCli = null;
        Subnets subCli = null;

        InstanceIdentifier<Vteps> vpath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(transportZone)).child(Subnets.class, subnetsKey)
                .child(Vteps.class, vtepkey).build();

    // check if present in tzones and delete from cache
        boolean existsInCache = isInCache(dpnId, portName, vlanId, ipAddress, subnetMask, gatewayIp,
                transportZone);
        if (!existsInCache) {
            Optional<Vteps> vtepOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, vpath, dataBroker);
            if (vtepOptional.isPresent()) {
                vtepCli = vtepOptional.get();
                if (Objects.equals(vtepCli.getIpAddress(), ipAddressObj)) {
                    InstanceIdentifier<Subnets> spath =
                            InstanceIdentifier
                                    .builder(TransportZones.class)
                                    .child(TransportZone.class, new TransportZoneKey(transportZone))
                                    .child(Subnets.class, subnetsKey).build();
                    Optional<Subnets> subOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, spath,
                            dataBroker);
                    if (subOptional.isPresent()) {
                        subCli = subOptional.get();
                        if (Objects.equals(subCli.getGatewayIp(), gatewayIpObj) && Objects.equals(subCli.getVlanId(),
                                vlanId)) {
                            vtepDelCommitList.add(vtepCli);
                        } else {
                            handleError("vtep with this vlan or gateway doesnt exist");
                        }
                    }
                } else {
                    handleError("Vtep with this ipaddress doesnt exist");
                }
            } else {
                handleError("Vtep Doesnt exist");
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public <T extends DataObject> void deleteOnCommit() {
        List<InstanceIdentifier<T>> vtepPaths = new ArrayList<>();
        List<InstanceIdentifier<T>> subnetPaths = new ArrayList<>();
        List<Subnets> subDelList = new ArrayList<>();
        List<Vteps> vtepDelList = new ArrayList<>();
        List<InstanceIdentifier<T>> allPaths = new ArrayList<>();
        try {
            if (vtepDelCommitList != null && !vtepDelCommitList.isEmpty()) {
                InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
                Optional<TransportZones> transportZonesOptional =
                        ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
                if (transportZonesOptional.isPresent()) {
                    List<TransportZone> transportZones = transportZonesOptional.get().nonnullTransportZone();
                    for (TransportZone tz : transportZones) {
                        if (tz.getSubnets() == null || tz.getSubnets().isEmpty()) {
                            continue;
                        }
                        for (Subnets sub : tz.getSubnets()) {
                            vtepDelList.addAll(vtepDelCommitList);
                            for (Vteps vtep : vtepDelList) {
                                InstanceIdentifier<T> vpath =
                                        (InstanceIdentifier<T>) InstanceIdentifier
                                                .builder(TransportZones.class)
                                                .child(TransportZone.class, tz.key())
                                                .child(Subnets.class, sub.key())
                                                .child(Vteps.class, vtep.key()).build();
                                if (sub.getVteps() != null && sub.getVteps().remove(vtep)) {
                                    vtepPaths.add(vpath);
                                    if (sub.getVteps().size() == 0 || sub.getVteps() == null) {
                                        subDelList.add(sub);
                                    }

                                }
                            }
                        }
                    }

                    for (TransportZone tz : transportZones) {
                        if (tz.getSubnets() == null || tz.getSubnets().isEmpty()) {
                            continue;
                        }
                        for (Subnets sub : subDelList) {
                            if (tz.getSubnets().remove(sub)) {
                                InstanceIdentifier<T> spath =
                                        (InstanceIdentifier<T>) InstanceIdentifier
                                                .builder(TransportZones.class)
                                                .child(TransportZone.class, tz.key())
                                                .child(Subnets.class, sub.key()).build();
                                subnetPaths.add(spath);
                            }
                        }
                    }

                    allPaths.addAll(vtepPaths);
                    allPaths.addAll(subnetPaths);
                    Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                        tx -> allPaths.forEach(tx::delete)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                            MoreExecutors.directExecutor());
                }
                vtepPaths.clear();
                subnetPaths.clear();
                allPaths.clear();
                vtepDelCommitList.clear();
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    public void showState(Collection<StateTunnelList> tunnelLists, boolean tunnelMonitorEnabled) throws TepException {
        if (tunnelLists == null || tunnelLists.isEmpty()) {
            handleError("No Internal Tunnels Exist");
            return;
        }
        if (!tunnelMonitorEnabled) {
            System.out.println("Tunnel Monitoring is Off");
        }
        String displayFormat = "%-16s  %-16s  %-16s  %-16s  %-16s  %-10s  %-10s";
        System.out.println(String.format(displayFormat, "Tunnel Name", "Source-DPN",
                        "Destination-DPN", "Source-IP", "Destination-IP", "Trunk-State", "Transport Type"));
        System.out.println("-----------------------------------------------------------------------------------------"
                + "--------------------------------------------");

        for (StateTunnelList tunnelInst : tunnelLists) {
           // Display only the internal tunnels
            if (TepTypeInternal.class.equals(tunnelInst.getDstInfo().getTepDeviceType())) {
                String tunnelInterfaceName = tunnelInst.getTunnelInterfaceName();
                LOG.trace("tunnelInterfaceName::: {}", tunnelInterfaceName);
                String tunnelState = ITMConstants.TUNNEL_STATE_UNKNOWN;
                if (tunnelInst.getOperState() == TunnelOperStatus.Up) {
                    tunnelState = ITMConstants.TUNNEL_STATE_UP;
                } else if (tunnelInst.getOperState() == TunnelOperStatus.Down) {
                    tunnelState = ITMConstants.TUNNEL_STATE_DOWN;
                }
                Class<? extends TunnelTypeBase> tunType = tunnelInst.getTransportType();
                String tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                if (TunnelTypeVxlan.class.equals(tunType)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                } else if (tunType.equals(TunnelTypeGre.class)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_GRE;
                } else if (tunType.equals(TunnelTypeMplsOverGre.class)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_MPLSoGRE;
                } else if (tunType.equals(TunnelTypeLogicalGroup.class)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_LOGICAL_GROUP_VXLAN;
                }
                System.out.println(String.format(displayFormat, tunnelInst.getTunnelInterfaceName(),
                        tunnelInst.getSrcInfo().getTepDeviceId(), tunnelInst.getDstInfo().getTepDeviceId(),
                        tunnelInst.getSrcInfo().getTepIp().stringValue(),
                        tunnelInst.getDstInfo().getTepIp().stringValue(), tunnelState, tunnelType));
            }
        }
    }

    // Show DPN-ID and Bridge mapping
    public void showBridges(Map<BigInteger, OvsdbBridgeRef> dpnIdBridgeRefMap) {
        System.out.println(String.format("%-16s  %-16s  %-36s%n", "DPN-ID", "Bridge-Name", "Bridge-UUID")
                           + "------------------------------------------------------------------------");
        dpnIdBridgeRefMap.forEach((dpnId, ovsdbBridgeRef) -> {
            String szBridgeId = ovsdbBridgeRef.getValue().firstKeyOf(Node.class).getNodeId().getValue();
            String bridgeUUID = szBridgeId.substring(13, 49);
            String bridgeName = szBridgeId.substring(57);
            System.out.println(String.format("%-16s  %-16s  %-36s", dpnId, bridgeName, bridgeUUID));
        });
    }

    // deletes from ADD-cache if it exists.
    public boolean isInCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone) throws TepException {
        boolean exists = false;
        final VtepsKey vtepkey = new VtepsKey(dpnId, portName);
        IpAddress ipAddressObj = IpAddressBuilder.getDefaultInstance(ipAddress);
        IpPrefix subnetMaskObj = IpPrefixBuilder.getDefaultInstance(subnetMask);
        IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
        if (gatewayIp != null) {
            gatewayIpObj = IpAddressBuilder.getDefaultInstance(gatewayIp);
        } else {
            LOG.debug("gateway is null");
        }
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        Vteps vtepCli = new VtepsBuilder().setDpnId(dpnId).setIpAddress(ipAddressObj).withKey(vtepkey)
                .setPortname(portName).build();
        SubnetObject subObCli = new SubnetObject(gatewayIpObj, subnetsKey, subnetMaskObj, vlanId);

        if (transportZonesHashMap.containsKey(transportZone)) {
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = transportZonesHashMap.get(transportZone);
            if (subVtepMapTemp.containsKey(subObCli)) { // if Subnet exists
                List<Vteps> vtepListTemp = subVtepMapTemp.get(subObCli);
                if (vtepListTemp.contains(vtepCli)) {
                    exists = true; // return true if tzones has vtep
                    vtepListTemp.remove(vtepCli);
                    if (vtepListTemp.size() == 0) {
                        subVtepMapTemp.remove(subObCli);
                        if (subVtepMapTemp.size() == 0) {
                            transportZonesHashMap.remove(transportZone);
                        }
                    }
                } else {
                    handleError("Vtep has not been configured");
                }
            }
        }
        return exists;
    }

    public void configureTunnelType(String transportZoneName, String tunnelType) throws ExecutionException,
            InterruptedException {
        LOG.debug("configureTunnelType {} for transportZone {}", tunnelType, transportZoneName);

        TransportZone transportZoneFromConfigDS = ItmUtils.getTransportZoneFromConfigDS(transportZoneName, dataBroker);
        Class<? extends TunnelTypeBase> tunType;

        validateTunnelType(transportZoneName, tunnelType, transportZoneFromConfigDS);
        if (transportZoneFromConfigDS != null) {
            if (!transportZoneName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                LOG.debug("Transport zone {} with tunnel type {} already exists. No action required.",
                    transportZoneName, tunnelType);
                return;
            } else {
                tunnelType = StringUtils.upperCase(tunnelType);
                tunType = ItmUtils.TUNNEL_TYPE_MAP.get(tunnelType);
                if (Objects.equals(transportZoneFromConfigDS.getTunnelType(), tunType)) {
                    // default-TZ already exists and tunnel-type is not changed during
                    // controller restart, then nothing to do now. Just return.
                    return;
                }
            }
        }

        // get tunnel-type
        tunnelType = StringUtils.upperCase(tunnelType);
        tunType = ItmUtils.TUNNEL_TYPE_MAP.get(tunnelType);

        List<TransportZone> tzList = null;
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tzones = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);

        TransportZone tzone = new TransportZoneBuilder().withKey(new TransportZoneKey(transportZoneName))
                .setTunnelType(tunType).build();
        if (tzones.isPresent()) {
            tzList = tzones.get().getTransportZone();
            if (tzList == null || tzList.isEmpty()) {
                tzList = new ArrayList<>();
            }
        } else {
            tzList = new ArrayList<>();
        }
        tzList.add(tzone);
        TransportZones transportZones = new TransportZonesBuilder().setTransportZone(tzList).build();
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.put(LogicalDatastoreType.CONFIGURATION,
                path, transportZones, WriteTransaction.CREATE_MISSING_PARENTS)).get();

    }

    /**
     * Validate tunnel type.
     *
     * @param transportZoneName
     *            the t zone name
     * @param tunnelType
     *            the tunnel type
     */
    private void validateTunnelType(String transportZoneName, String tunnelType,TransportZone tzoneFromConfigDs) {
        String strTunnelType = ItmUtils.validateTunnelType(tunnelType);
        Class<? extends TunnelTypeBase> tunType;
        if (strTunnelType.equals(ITMConstants.TUNNEL_TYPE_VXLAN)) {
            tunType = TunnelTypeVxlan.class;
        } else {
            tunType = TunnelTypeGre.class;
        }

        if (tzoneFromConfigDs != null) {
            if (!Objects.equals(tzoneFromConfigDs.getTunnelType(), tunType) && ItmUtils.isNotEmpty(tzoneFromConfigDs
                    .getSubnets())) {
                // for default-TZ, such error message is not needed to be thrown.
                // it needs to be handled in different way, by deleting default-TZ
                // with old tunnel-type and then add default-TZ with new tunnel-type
                if (!transportZoneName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                    throw new IllegalArgumentException(
                        "Changing the tunnel type from " + tzoneFromConfigDs.getTunnelType()
                            + " to " + strTunnelType
                            + " is not allowed for already configured transport zone [" + transportZoneName
                            + "].");
                } else {
                    // delete already existing default TZ
                    ItmUtils.deleteTransportZoneFromConfigDS(ITMConstants.DEFAULT_TRANSPORT_ZONE, dataBroker);
                }
            }
        }
    }

    public void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol) {
        InstanceIdentifier<TunnelMonitorParams> path = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        Optional<TunnelMonitorParams> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, dataBroker);
        Class<? extends TunnelMonitoringTypeBase> monitorType ;
        if (storedTunnelMonitor.isPresent() && storedTunnelMonitor.get().getMonitorProtocol() != null) {
            monitorType = storedTunnelMonitor.get().getMonitorProtocol();
        } else {
            if (monitorProtocol != null && monitorProtocol.equalsIgnoreCase(ITMConstants.MONITOR_TYPE_LLDP)) {
                monitorType = TunnelMonitoringTypeLldp.class;
            } else {
                monitorType = TunnelMonitoringTypeBfd.class;
            }
        }
        if (!storedTunnelMonitor.isPresent() || storedTunnelMonitor.get().isEnabled() != monitorEnabled) {
            TunnelMonitorParams tunnelMonitor = new TunnelMonitorParamsBuilder().setEnabled(monitorEnabled)
                    .setMonitorProtocol(monitorType).build();
            Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> tx.merge(path, tunnelMonitor, true)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                    MoreExecutors.directExecutor());
        }
    }

    public void configureTunnelMonitorInterval(int interval) {
        InstanceIdentifier<TunnelMonitorInterval> path =
                InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        Optional<TunnelMonitorInterval> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path,
                dataBroker);
        if (!storedTunnelMonitor.isPresent() || storedTunnelMonitor.get().getInterval() != interval) {
            TunnelMonitorInterval tunnelMonitor = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
            Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> tx.merge(path, tunnelMonitor, true)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                    MoreExecutors.directExecutor());
        }
    }

    public void handleError(String errorMessage) throws TepException {
        throw new TepException(errorMessage);
    }
}
