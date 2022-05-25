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

import com.exactpro.th2.converter.model.latest.box.pins.FilterSpecGrpc
import com.exactpro.th2.converter.model.latest.box.pins.FilterSpecMq

data class FilterSpecV1(
    val properties: List<FilterFieldSpecV1>?,
    val message: List<FilterFieldSpecV1>?,
    val metadata: List<FilterFieldSpecV1>?
) {
    fun toMqFilter(): FilterSpecMq {
        return FilterSpecMq(
            properties?.map { it.toFilterFieldSpec() },
            message?.map { it.toFilterFieldSpec() },
            metadata?.map { it.toFilterFieldSpec() }
        )
    }

    fun toGrpcFilter(): FilterSpecGrpc {
        return FilterSpecGrpc(
            properties?.map { it.toFilterFieldSpec() }
        )
    }
}
