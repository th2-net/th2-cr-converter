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

package com.exactpro.th2.converter.model.v1.box.pins

import com.exactpro.th2.converter.model.latest.box.pins.GrpcClient
import com.exactpro.th2.converter.model.latest.box.pins.GrpcServer
import com.exactpro.th2.converter.model.latest.box.pins.MqPin
import com.exactpro.th2.converter.model.latest.box.pins.PinSettings
import com.fasterxml.jackson.annotation.JsonProperty

data class PinSpecV1(
    // required
    val name: String,
    @JsonProperty("connection-type") val connectionType: String,

    // optional
    @JsonProperty("service-class") val serviceClass: String?,
    @JsonProperty("service-classes") val serviceClasses: List<String>?,
    val attributes: List<String>?,
    val strategy: String?,
    val filters: List<FilterSpecV1>?,
    val settings: PinSettings?
) {

    fun toMqPin(): MqPin {
        return MqPin(
            name,
            attributes,
            filters?.map { it.toMqFilter() },
            settings
        )
    }

    fun toGrpcClientPin(): GrpcClient {
        return GrpcClient(
            name,
            serviceClass ?: SERVICE_CLASS_PLACE_HOLDER,
            attributes,
            filters?.map { it.toGrpcFilter() },
            strategy
        )
    }

    fun toGrpcServerPin(): GrpcServer {
        return GrpcServer(
            name,
            serviceClasses ?: SERVICE_CLASSES_PLACE_HOLDER
        )
    }

    companion object {
        const val SERVICE_CLASS_PLACE_HOLDER = "PLEASE SPECIFY SERVICE CLASS"
        val SERVICE_CLASSES_PLACE_HOLDER = listOf("PLEASE SPECIFY SERVICE CLASSES")
    }
}
