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

import com.exactpro.th2.model.latest.box.pins.GrpcClient
import com.exactpro.th2.model.latest.box.pins.GrpcSection
import com.exactpro.th2.model.latest.box.pins.GrpcServer
import com.exactpro.th2.model.latest.box.pins.MqPublisher
import com.exactpro.th2.model.latest.box.pins.MqSection
import com.exactpro.th2.model.latest.box.pins.MqSubscriber
import com.exactpro.th2.model.latest.box.pins.PinSpec
import com.exactpro.th2.model.v1.box.SpecV1
import com.exactpro.th2.model.v1.box.pins.PinType
import com.exactpro.th2.model.v2.SpecV2

class ConvertibleBoxSpecV1(val spec: SpecV1) : Convertible {
    override fun toNextVersion(): Convertible {
        return ConvertibleBoxSpecV2(
            SpecV2(
                spec.imageName,
                spec.imageVersion,
                spec.type,
                spec.versionRange,
                spec.customConfig,
                spec.extendedSettings?.toExtendedSettingsV2(),
                convertPins().takeIf { it.isNotEmpty() },
                spec.prometheus,
                spec.loggingConfig,
                spec.mqRouter,
                spec.grpcRouter,
                spec.cradleManager,
                spec.disabled,
                imagePullSecrets = spec.imagePullSecrets,
            )
        )
    }

    override fun getSpecObject(): Any {
        return spec
    }

    private fun convertPins(): PinSpec {
        val mqSubscriber = ArrayList<MqSubscriber>()
        val mqPublishers = ArrayList<MqPublisher>()
        val grpcClient = ArrayList<GrpcClient>()
        val grpcServer = ArrayList<GrpcServer>()

        for (pin in spec.pins ?: ArrayList()) {
            when (pin.connectionType) {
                PinType.MQ.value -> {
                    if (pin.attributes?.contains("publish") == true) {
                        mqPublishers.add(pin.toPublisherPin())
                    } else {
                        mqSubscriber.add(pin.toSubscriberPin())
                    }
                }
                PinType.GRPC_CLIENT.value -> grpcClient.add(pin.toGrpcClientPin())
                PinType.GRPC_SERVER.value -> grpcServer.add(pin.toGrpcServerPin())
            }
        }
        val grpcSection = GrpcSection(
            grpcClient.ifEmpty { null },
            grpcServer.ifEmpty { null }
        )

        val mqSection = MqSection(
            mqSubscriber.ifEmpty { null },
            mqPublishers.ifEmpty { null }
        )
        return PinSpec(
            mqSection.takeIf { it.isNotEmpty() },
            grpcSection.takeIf { it.isNotEmpty() }
        )
    }
}
