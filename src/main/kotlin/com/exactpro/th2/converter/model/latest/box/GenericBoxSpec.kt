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

package com.exactpro.th2.converter.model.latest.box

import com.exactpro.th2.converter.model.Convertible
import com.exactpro.th2.converter.model.latest.box.extendedsettings.ExtendedSettings
import com.exactpro.th2.converter.model.latest.box.pins.PinSpec
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)

data class GenericBoxSpec(
    // required fields
    val imageName: String,
    val imageVersion: String,

    // optional fields
    val type: String?,
    val versionRange: String?,
    val customConfig: Map<String, Any>?,
    val extendedSettings: ExtendedSettings?,
    val pins: PinSpec?,
    val prometheus: Prometheus?,
    val loggingConfig: String?,
    val mqRouter: Map<String, Any>?,
    val grpcRouter: Map<String, Any>?,
    val cradleManager: Map<String, Any>?,
    val disabled: Boolean?,
    val bookName: String? = null
) : Convertible {
    override fun toNextVersion(): Convertible {
        throw AssertionError("THis is the latest version. Further conversions are not supported")
    }
}
