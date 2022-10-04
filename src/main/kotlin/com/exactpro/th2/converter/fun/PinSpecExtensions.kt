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

import com.exactpro.th2.model.latest.box.pins.FilterFieldSpec
import com.exactpro.th2.model.latest.box.pins.FilterSpecGrpc
import com.exactpro.th2.model.latest.box.pins.FilterSpecMq
import com.exactpro.th2.model.latest.box.pins.GrpcClient
import com.exactpro.th2.model.latest.box.pins.GrpcSection
import com.exactpro.th2.model.latest.box.pins.GrpcServer
import com.exactpro.th2.model.latest.box.pins.MqPublisher
import com.exactpro.th2.model.latest.box.pins.MqSection
import com.exactpro.th2.model.latest.box.pins.MqSubscriber
import com.exactpro.th2.model.latest.box.pins.PinSpec
import com.exactpro.th2.model.v1.box.pins.FilterFieldSpecV1
import com.exactpro.th2.model.v1.box.pins.FilterSpecV1
import com.exactpro.th2.model.v1.box.pins.PinSpecV1

fun FilterFieldSpecV1.toFilterFieldSpec(): FilterFieldSpec {
    return FilterFieldSpec(expectedValue, fieldName, operation)
}

fun FilterSpecV1.toMqFilter(): FilterSpecMq {
    return FilterSpecMq(
        properties?.map { it.toFilterFieldSpec() },
        message?.map { it.toFilterFieldSpec() },
        metadata?.map { it.toFilterFieldSpec() }
    )
}

fun FilterSpecV1.toGrpcFilter(): FilterSpecGrpc {
    return FilterSpecGrpc(
        properties?.map { it.toFilterFieldSpec() }
    )
}

fun PinSpecV1.toSubscriberPin(): MqSubscriber {
    return MqSubscriber(
        name,
        attributes,
        filters?.map { it.toMqFilter() },
        settings
    )
}

fun PinSpecV1.toPublisherPin(): MqPublisher {
    return MqPublisher(
        name,
        attributes,
        filters?.map { it.toMqFilter() },
    )
}

fun PinSpecV1.toGrpcClientPin(): GrpcClient {
    return GrpcClient(
        name,
        serviceClass ?: "PLEASE SPECIFY SERVICE CLASS",
        attributes,
        filters?.map { it.toGrpcFilter() },
        strategy
    )
}

fun PinSpecV1.toGrpcServerPin(): GrpcServer {
    return GrpcServer(
        name,
        serviceClasses ?: listOf("PLEASE SPECIFY SERVICE CLASSES")
    )
}

fun PinSpec.isNotEmpty() = mq != null || grpc != null
fun MqSection.isNotEmpty() = subscribers != null || publishers != null
fun GrpcSection.isNotEmpty() = client != null || server != null
