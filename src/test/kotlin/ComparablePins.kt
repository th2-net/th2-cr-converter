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

import com.exactpro.th2.converter.`fun`.toGrpcClientPin
import com.exactpro.th2.converter.`fun`.toGrpcServerPin
import com.exactpro.th2.converter.`fun`.toPublisherPin
import com.exactpro.th2.converter.`fun`.toSubscriberPin
import com.exactpro.th2.model.latest.box.pins.GrpcClient
import com.exactpro.th2.model.latest.box.pins.GrpcServer
import com.exactpro.th2.model.latest.box.pins.MqPublisher
import com.exactpro.th2.model.latest.box.pins.MqSubscriber
import com.exactpro.th2.model.v1.box.pins.PinSpecV1

data class ComparableMqSubscriberV2(
    val subscriber: MqSubscriber
) : ComparableTo<PinSpecV1> {
    override fun contentEquals(that: PinSpecV1): Boolean =
        this.subscriber == that.toSubscriberPin()
}

data class ComparableMqPublisherV2(
    val publisher: MqPublisher
) : ComparableTo<PinSpecV1> {
    override fun contentEquals(that: PinSpecV1) =
        this.publisher == that.toPublisherPin()
}

data class ComparableGrpcClientV2(
    val grpcClient: GrpcClient
) : ComparableTo<PinSpecV1> {
    override fun contentEquals(that: PinSpecV1) =
        this.grpcClient == that.toGrpcClientPin()
}

data class ComparableGrpcServerV2(
    val grpcServer: GrpcServer
) : ComparableTo<PinSpecV1> {
    override fun contentEquals(that: PinSpecV1) =
        this.grpcServer == that.toGrpcServerPin()
}

sealed interface ComparableTo<T> {
    fun contentEquals(that: T): Boolean
}
