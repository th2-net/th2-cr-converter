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

package com.exactpro.th2.converter.model.v1.box

import com.exactpro.th2.converter.model.Convertible
import com.exactpro.th2.converter.model.latest.box.GenericBoxSpec
import com.exactpro.th2.converter.model.latest.box.Prometheus
import com.exactpro.th2.converter.model.latest.box.pins.GrpcClient
import com.exactpro.th2.converter.model.latest.box.pins.GrpcSection
import com.exactpro.th2.converter.model.latest.box.pins.GrpcServer
import com.exactpro.th2.converter.model.latest.box.pins.MqPin
import com.exactpro.th2.converter.model.latest.box.pins.PinSpec
import com.exactpro.th2.converter.model.v1.box.extendedsettings.ExtendedSettingsV1
import com.exactpro.th2.converter.model.v1.box.pins.PinSpecV1
import com.fasterxml.jackson.annotation.JsonProperty

data class GenericBoxSpecV1(
    // required fields
    @JsonProperty("image-name") val imageName: String,
    @JsonProperty("image-version") val imageVersion: String,

    // optional fields
    val type: String?,
    @JsonProperty("version-range") val versionRange: String?,
    @JsonProperty("custom-config") val customConfig: Map<String, Any>?,
    @JsonProperty("extended-settings") val extendedSettings: ExtendedSettingsV1?,
    val pins: List<PinSpecV1>?,
    val prometheus: Prometheus?,
    val loggingConfig: String?,
    val mqRouter: Map<String, Any>?,
    val grpcRouter: Map<String, Any>?,
    val cradleManager: Map<String, Any>?,
    val disabled: Boolean?
) : Convertible {
    override fun toNextVersion(): Convertible {
        return GenericBoxSpec(
            imageName,
            imageVersion,
            type,
            versionRange,
            customConfig,
            extendedSettings?.toExtendedSettings(),
            convertPins().takeIf { it.isNotEmpty() },
            prometheus,
            loggingConfig,
            mqRouter,
            grpcRouter,
            cradleManager,
            disabled
        )
    }

    private fun convertPins(): PinSpec {
        val mqPins = ArrayList<MqPin>()
        val grpcClient = ArrayList<GrpcClient>()
        val grpcServer = ArrayList<GrpcServer>()

        for (pin in pins ?: ArrayList()) {
            when (pin.connectionType) {
                PinType.MQ.value -> mqPins.add(pin.toMqPin())
                PinType.GRPC_CLIENT.value -> grpcClient.add(pin.toGrpcClientPin())
                PinType.GRPC_SERVER.value -> grpcServer.add(pin.toGrpcServerPin())
            }
        }
        val grpcSection = GrpcSection(
            grpcClient.ifEmpty { null },
            grpcServer.ifEmpty { null }
        )

        return PinSpec(
            mqPins.ifEmpty { null },
            grpcSection.takeIf { it.isNotEmpty() }
        )
    }
}

enum class PinType(val value: String) {
    MQ("mq"),
    GRPC_CLIENT("grpc-client"),
    GRPC_SERVER("grpc-server"),
}
