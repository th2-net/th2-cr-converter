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

package com.exactpro.th2.converter.model

import com.exactpro.th2.converter.model.latest.Th2Metadata
import com.exactpro.th2.converter.util.ProjectConstants.API_VERSION_V2

data class Th2Resource(
    val apiVersion: String,
    val kind: String,
    val metadata: Th2Metadata,
    val spec: Convertible,
) {
    fun toNextVersion(): Th2Resource {
        return Th2Resource(
            API_VERSION_V2,
            kind,
            metadata,
            spec.toNextVersion()
        )
    }
}
