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
import com.exactpro.th2.model.latest.box.extendedsettings.NodePortConfig
import com.exactpro.th2.model.latest.box.extendedsettings.Service
import com.exactpro.th2.model.v1.box.extendedsettings.ExtendedSettingsV1
import com.exactpro.th2.model.v1.box.extendedsettings.ServiceV1

fun ServiceV1.toService(): Service {
    val nodePort: MutableList<NodePortConfig> = ArrayList()
    val clusterIP: MutableList<ClusterIpConfig> = ArrayList()
    when (type) {
        ServiceV1.ServiceType.NodePort -> {
            endpoints?.forEach {
                nodePort.add(NodePortConfig(it.name, it.targetPort, it.nodePort ?: -1))
            }
        }
        ServiceV1.ServiceType.ClusterIP -> {
            endpoints?.forEach {
                clusterIP.add(ClusterIpConfig(it.name, it.targetPort))
            }
        }
        null -> {}
    }
    return Service(
        enabled,
        nodePort.takeIf { it.isNotEmpty() },
        clusterIP.takeIf { it.isNotEmpty() },
        ingress
    )
}

fun ExtendedSettingsV1.toExtendedSettings(): ExtendedSettings {
    return ExtendedSettings(
        chartConfig,
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
