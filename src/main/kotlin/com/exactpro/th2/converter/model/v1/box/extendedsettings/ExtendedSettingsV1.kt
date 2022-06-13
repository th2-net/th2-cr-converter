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

package com.exactpro.th2.converter.model.v1.box.extendedsettings

import com.exactpro.th2.converter.model.latest.box.ChartConfig
import com.exactpro.th2.converter.model.latest.box.extendedsettings.ExtendedSettings
import com.exactpro.th2.converter.model.latest.box.extendedsettings.ExternalBox
import com.exactpro.th2.converter.model.latest.box.extendedsettings.Mounting
import com.exactpro.th2.converter.model.latest.box.extendedsettings.Resources
import com.exactpro.th2.converter.model.latest.box.extendedsettings.SharedMemory
import com.fasterxml.jackson.annotation.JsonProperty

data class ExtendedSettingsV1(
    @JsonProperty("chart-cfg") val chartConfig: ChartConfig?,
    val envVariables: Map<String, String>?,
    val sharedMemory: SharedMemory?,
    val replicas: Int?,
    val k8sProbes: Boolean?,
    val externalBox: ExternalBox?,
    val hostNetwork: Boolean?,
    val nodeSelector: Map<String, String>?,
    val mounting: List<Mounting>?,
    val resources: Resources?,
    val service: ServiceV1?
) {
    fun toExtendedSettings(): ExtendedSettings {
        return ExtendedSettings(
            chartConfig,
            envVariables,
            sharedMemory,
            replicas,
            k8sProbes,
            externalBox,
            hostAliases = null,
            hostNetwork,
            nodeSelector,
            mounting,
            resources,
            service?.toService()
        )
    }
}
