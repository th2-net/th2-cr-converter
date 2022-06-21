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

package com.exactpro.th2.converter.model.latest.box.pins

import com.exactpro.th2.converter.model.ComparableTo
import com.exactpro.th2.converter.model.v1.box.pins.PinSpecV1
import com.fasterxml.jackson.annotation.JsonIgnore

data class PinSpec(
    val mq: List<MqPin>?,
    val grpc: GrpcSection?
) {
    @JsonIgnore
    fun isNotEmpty(): Boolean {
        return mq != null || grpc != null
    }
}

data class MqPin(
    val name: String,

    val attributes: List<String>?,
    val filters: List<FilterSpecMq>?,
    val settings: PinSettings?
) : ComparableTo<PinSpecV1> {
    override fun contentEquals(that: PinSpecV1): Boolean {
        return this == that.toMqPin()
    }
}

data class GrpcClient(
    val name: String,
    val serviceClass: String,

    val attributes: List<String>?,
    val filters: List<FilterSpecGrpc>?,
    val strategy: String?,
) : ComparableTo<PinSpecV1> {
    override fun contentEquals(that: PinSpecV1): Boolean {
        return this == that.toGrpcClientPin()
    }
}

data class GrpcServer(
    val name: String,
    val serviceClasses: List<String>,
) : ComparableTo<PinSpecV1> {
    override fun contentEquals(that: PinSpecV1): Boolean {
        return this == that.toGrpcServerPin()
    }
}

data class GrpcSection(
    val client: List<GrpcClient>?,
    val server: List<GrpcServer>?
) {
    @JsonIgnore
    fun isNotEmpty(): Boolean {
        return client != null || server != null
    }
}
