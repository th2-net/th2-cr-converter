/*
 * Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.converter.`fun`

import com.exactpro.th2.model.latest.box.extendedsettings.ClusterIpConfig
import com.exactpro.th2.model.latest.box.extendedsettings.ExtendedSettings
import com.exactpro.th2.model.latest.box.extendedsettings.LoadBalancerConfig
import com.exactpro.th2.model.latest.box.extendedsettings.NodePortConfig
import com.exactpro.th2.model.latest.box.extendedsettings.Service
import com.exactpro.th2.model.v1.box.extendedsettings.ExtendedSettingsV1
import com.exactpro.th2.model.v1.box.extendedsettings.ServiceEndpoint
import com.exactpro.th2.model.v1.box.extendedsettings.ServiceV1
import com.exactpro.th2.model.v1.box.extendedsettings.ServiceV1.ServiceType.ClusterIP
import com.exactpro.th2.model.v1.box.extendedsettings.ServiceV1.ServiceType.LoadBalancer
import com.exactpro.th2.model.v1.box.extendedsettings.ServiceV1.ServiceType.NodePort
import com.exactpro.th2.model.v2.ClusterIpConfigV2
import com.exactpro.th2.model.v2.ExtendedSettingsV2
import com.exactpro.th2.model.v2.LoadBalancerConfigV2
import com.exactpro.th2.model.v2.NodePortConfigV2
import com.exactpro.th2.model.v2.ServiceV2

fun ServiceV1.toServiceV2(): ServiceV2 {
    val nodePort: MutableList<NodePortConfigV2> = ArrayList()
    val clusterIP: MutableList<ClusterIpConfigV2> = ArrayList()
    val loadBalancer: MutableList<LoadBalancerConfigV2> = ArrayList()

    val addToRelevantList: (ServiceEndpoint) -> Unit = when (type) {
        NodePort -> {
            {
                nodePort.add(NodePortConfigV2(it.name, it.targetPort, it.nodePort ?: -1))
            }
        }

        ClusterIP -> {
            {
                clusterIP.add(ClusterIpConfigV2(it.name, it.targetPort))
            }
        }

        LoadBalancer -> {
            {
                loadBalancer.add(LoadBalancerConfigV2(it.name, it.targetPort))
            }
        }

        null -> { _ -> }
    }

    endpoints?.forEach(addToRelevantList)

    return ServiceV2(
        enabled,
        nodePort.takeIf { it.isNotEmpty() },
        clusterIP.takeIf { it.isNotEmpty() },
        loadBalancer.takeIf { it.isNotEmpty() },
        ingress
    )
}

fun ServiceV2.toService(): Service {
    if (ingress == null) {
        return Service(
            enabled,
            this.nodePort?.map { NodePortConfig(it.name, it.containerPort, it.exposedPort) },
            this.clusterIP?.map { ClusterIpConfig(it.name, it.containerPort) },
            this.loadBalancer?.map { LoadBalancerConfig(it.name, it.containerPort) }
        )
    }
    assert(ingress!!.urlPaths!!.size == 1) {
        "Service can't be upgraded to version v2-2. 'ingress.urlPaths' must contain 1 item"
    }

    val multiConfigErrorMessage =
        "Service can't be upgraded to next version. only one out of nodePort, clusterIP or loadBalancer is allowed"
    val urlPath = ingress!!.urlPaths!![0]
    val port8080 = 8080
    val port80 = 80
    if (nodePort != null) {
        assert(clusterIP == null) { multiConfigErrorMessage }
        assert(loadBalancer == null) { multiConfigErrorMessage }
        val nodePortsV2 = nodePort!!.map { NodePortConfig(it.name, it.containerPort, it.exposedPort) }.toMutableList()
        if (nodePort!!.size == 1) {
            val firstPort = nodePortsV2[0]
            nodePortsV2[0] = NodePortConfig(firstPort.name, firstPort.containerPort, firstPort.exposedPort, urlPath)
        } else {
            var contains8080 = false
            var contains80 = false
            nodePortsV2.forEachIndexed breaking@{ index, nodePortConfig ->
                if (nodePortConfig.containerPort == port8080) {
                    val port = nodePortsV2[index]
                    nodePortsV2[index] = NodePortConfig(port.name, port.containerPort, port.exposedPort, urlPath)
                    contains8080 = true
                    return@breaking
                }
            }
            if (!contains8080) {
                nodePortsV2.forEachIndexed breaking@{ index, nodePortConfig ->
                    if (nodePortConfig.containerPort == port80) {
                        val port = nodePortsV2[index]
                        nodePortsV2[index] = NodePortConfig(port.name, port.containerPort, port.exposedPort, urlPath)
                        contains80 = true
                        return@breaking
                    }
                }
            }
            if (!contains80 && !contains8080) {
                val firstPort = nodePortsV2[0]
                nodePortsV2[0] = NodePortConfig(firstPort.name, firstPort.containerPort, firstPort.exposedPort, urlPath)
            }
        }
        return Service(
            enabled,
            nodePort = nodePortsV2
        )
    } else if (clusterIP != null) {
        assert(nodePort == null) { multiConfigErrorMessage }
        assert(loadBalancer == null) { multiConfigErrorMessage }
        val clusterIPV2 = clusterIP!!.map { ClusterIpConfig(it.name, it.containerPort) }.toMutableList()
        if (clusterIP!!.size == 1) {
            val firstPort = clusterIPV2[0]
            clusterIPV2[0] = ClusterIpConfig(firstPort.name, firstPort.containerPort, urlPath)
        } else {
            var contains8080 = false
            var contains80 = false
            clusterIPV2.forEachIndexed breaking@{ index, nodePortConfig ->
                if (nodePortConfig.containerPort == port8080) {
                    val port = clusterIPV2[index]
                    clusterIPV2[index] = ClusterIpConfig(port.name, port.containerPort, urlPath)
                    contains8080 = true
                    return@breaking
                }
            }
            if (!contains8080) {
                clusterIPV2.forEachIndexed breaking@{ index, nodePortConfig ->
                    if (nodePortConfig.containerPort == port80) {
                        val port = clusterIPV2[index]
                        clusterIPV2[index] = ClusterIpConfig(port.name, port.containerPort, urlPath)
                        contains80 = true
                        return@breaking
                    }
                }
            }
            if (!contains80 && !contains8080) {
                val firstPort = clusterIPV2[0]
                clusterIPV2[0] = ClusterIpConfig(firstPort.name, firstPort.containerPort, urlPath)
            }
        }
        return Service(
            enabled,
            clusterIP = clusterIPV2
        )
    } else if (loadBalancer != null) {
        assert(clusterIP == null) { multiConfigErrorMessage }
        assert(nodePort == null) { multiConfigErrorMessage }
        val loadBalancerV2 = loadBalancer!!.map { LoadBalancerConfig(it.name, it.containerPort) }.toMutableList()
        if (loadBalancer!!.size == 1) {
            val firstPort = loadBalancerV2[0]
            loadBalancerV2[0] = LoadBalancerConfig(firstPort.name, firstPort.containerPort, urlPath)
        } else {
            var contains8080 = false
            var contains80 = false
            loadBalancerV2.forEachIndexed breaking@{ index, nodePortConfig ->
                if (nodePortConfig.containerPort == port8080) {
                    val port = loadBalancerV2[index]
                    loadBalancerV2[index] = LoadBalancerConfig(port.name, port.containerPort, urlPath)
                    contains8080 = true
                    return@breaking
                }
            }
            if (!contains8080) {
                loadBalancerV2.forEachIndexed breaking@{ index, nodePortConfig ->
                    if (nodePortConfig.containerPort == port80) {
                        val port = loadBalancerV2[index]
                        loadBalancerV2[index] = LoadBalancerConfig(port.name, port.containerPort, urlPath)
                        contains80 = true
                        return@breaking
                    }
                }
            }
            if (!contains80 && !contains8080) {
                val firstPort = loadBalancerV2[0]
                loadBalancerV2[0] = LoadBalancerConfig(firstPort.name, firstPort.containerPort, urlPath)
            }
        }
        return Service(
            enabled,
            loadBalancer = loadBalancerV2
        )
    }
    throw IllegalArgumentException(
        "When ingress.urlPath is specified, one of nodePort, clusterIP or loadBalancer should also be specified "
    )
}

fun ExtendedSettingsV1.toExtendedSettingsV2(): ExtendedSettingsV2 {
    return ExtendedSettingsV2(
        envVariables,
        sharedMemory,
        replicas,
        k8sProbes,
        externalBox,
        hostAliases,
        hostNetwork,
        nodeSelector,
        mounting,
        resources,
        service?.toServiceV2()
    )
}

fun ExtendedSettingsV2.toExtendedSettings(): ExtendedSettings {
    return ExtendedSettings(
        envVariables,
        sharedMemory,
        replicas,
        k8sProbes,
        externalBox,
        hostAliases,
        hostNetwork,
        nodeSelector,
        mounting,
        resources,
        service?.toService()
    )
}
