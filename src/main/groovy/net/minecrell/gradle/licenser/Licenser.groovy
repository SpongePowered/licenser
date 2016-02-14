package net.minecrell.gradle.licenser

import groovy.text.SimpleTemplateEngine
import net.minecrell.gradle.licenser.header.Header
import net.minecrell.gradle.licenser.tasks.LicenseCheck
import net.minecrell.gradle.licenser.tasks.LicenseTask
import net.minecrell.gradle.licenser.tasks.LicenseUpdate
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet

class Licenser implements Plugin<Project> {

    private static final String CHECK_TASK = 'licenseCheck'
    private static final String FORMAT_TASK = 'licenseFormat'

    protected Project project
    protected LicenseExtension extension

    protected Task globalCheck
    protected Task globalFormat

    @Override
    void apply(Project project) {
        this.project = project

        project.with {
            this.extension = extensions.create('license', LicenseExtension)
            extension.with {
                header = project.file('LICENSE')
                sourceSets = project.sourceSets
            }

            this.globalCheck = task(CHECK_TASK)
            this.globalFormat = task(FORMAT_TASK)

            // Wait a bit until creating the tasks
            afterEvaluate {
                def header = new Header({
                    File header = extension.header
                    if (header != null && header.exists()) {
                        def text = header.getText(extension.charset)

                        Map<String, String> properties = extension.ext.properties
                        if (properties != null && !properties.isEmpty()) {
                            def engine = new SimpleTemplateEngine()
                            def template = engine.createTemplate(text).make(properties)
                            text = template.toString()
                        }

                        return text
                    }

                    return ""
                })

                extension.sourceSets.each {
                    def check = createTask(it.getTaskName(CHECK_TASK, null), LicenseCheck, header, it)
                    check.ignoreFailures = extension.ignoreFailures
                    globalCheck.dependsOn check
                    globalFormat.dependsOn createTask(it.getTaskName(FORMAT_TASK, null), LicenseUpdate, header, it)
                }
            }
        }
    }

    Project getProject() {
        project
    }

    LicenseExtension getExtension() {
        extension
    }

    private <T extends LicenseTask> T createTask(String name, Class<T> type, Header expectedHeader, SourceSet sourceSet) {
        return (T) project.task(name, type: type) {
            header = expectedHeader
            files = sourceSet.allSource
            filter = extension
            charset = extension.charset
        }
    }

}