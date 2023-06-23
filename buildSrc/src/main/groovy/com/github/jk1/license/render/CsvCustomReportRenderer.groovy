/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
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
package com.github.jk1.license.render

import com.github.jk1.license.ImportedModuleData
import com.github.jk1.license.License
import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.ModuleData
import com.github.jk1.license.ProjectData
import org.gradle.api.tasks.Input

import java.util.stream.Collectors

class CsvCustomReportRenderer implements ReportRenderer {

    @Input
    String filename
    @Input
    boolean includeHeaderLine = true
    @Input
    String quote = '\"'
    @Input
    String separator = ','
    @Input
    String nl = '\r\n'
    @Input
    boolean onlyOneLicensePerModule = false
    @Input
    String licenceSeparator = " | "

    CsvCustomReportRenderer(String filename = 'licenses.csv', boolean onlyOneLicensePerModule = false) {
        this.filename = filename
        this.onlyOneLicensePerModule = onlyOneLicensePerModule
    }

    @Override
    void render(ProjectData data) {
        LicenseReportExtension config = data.project.licenseReport
        File output = new File(config.outputDir, filename)
        output.write('')

        if (includeHeaderLine) {
            output << "${quote('artifact')}$separator${quote('version')}$separator${quote('moduleUrl')}$separator${quote('moduleLicense')}$separator${quote('moduleLicenseUrl')}$separator$nl"
        }

        data.allDependencies.sort().each {
            renderDependency(output, it)
        }

        data.importedModules.modules.flatten().sort().each {
            renderImportedModuleDependency(output, it)
        }
    }

    void renderDependency(File output, ModuleData data) {
        String moduleUrl = ""
        String moduleLicense = ""
        String moduleLicenseUrl = ""
        if (onlyOneLicensePerModule) {
            (moduleUrl, moduleLicense, moduleLicenseUrl) = LicenseDataCollector.singleModuleLicenseInfo(data)
        } else {
            LicenseDataCollector.MultiLicenseInfo info = LicenseDataCollector.multiModuleLicenseInfo(data)

            moduleUrl = info.moduleUrls.join(licenceSeparator)
            Map<String, License> licenses = info.licenses.stream()
                    .filter { it != null }
                    .collect(Collectors.toMap({ it.name + it.url }, { it }))
            moduleLicense = licenses.values().stream()
                    .map { it.name }
                    .collect(Collectors.joining(licenceSeparator))
            moduleLicenseUrl = licenses.values().stream()
                    .map { it.url }
                    .collect(Collectors.joining(licenceSeparator))
        }
        String artifact = "${data.group}:${data.name}"
        output << "${quote(artifact)}$separator${quote(data.version)}$separator${quote(moduleUrl)}$separator${quote(moduleLicense)}$separator${quote(moduleLicenseUrl)}$separator$nl"
    }

    private void renderImportedModuleDependency(File output, ImportedModuleData module) {

        String artifact = "${module.name}:${module.version}"
        output << "${quote(artifact)}$separator${quote(module.projectUrl)}$separator${quote(module.license)}${separator}${quote(module.licenseUrl)}$separator$nl"
    }

    private String quote(String content) {
        if (content == null || content.isEmpty()) {
            return ''
        }
        content = content.trim()
        content = content.replaceAll(quote, "\\\\$quote")
        "${quote}${content}${quote}"
    }
}
