// ================= CORE build.gradle.kts =================

import org.gradle.api.file.DuplicatesStrategy

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    }) {
        exclude("META-INF/**")
    }

    from("src/main/resources") {
        include("modules/**")
    }
}