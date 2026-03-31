import org.gradle.api.Project
import java.io.File

object Helper {

    fun ask(q: String): String {
        val console = System.console()
        return if (console != null) {
            console.readLine(q).trim()
        } else {
            print(q)
            System.out.flush()
            readLine()?.trim().orEmpty()
        }
    }

    fun choose(title: String, options: List<String>): Int {
        println("\n$title\n")

        options.forEachIndexed { i, opt ->
            println("${i + 1}. $opt")
        }

        print("\nEscolha: ")
        System.out.flush()

        return readLine()?.toIntOrNull() ?: -1
    }

    fun run(project: Project) {

        println("\n[LazzEngine Helper]\n")

        val choice = choose("Selecione uma opcao:", listOf(
            "Commit (Git)",
            "Version (Bump)",
            "Changelog",
            "Release",
            "Criar modulo",
            "Build completo"
        ))

        val nextTask = when (choice) {
            1 -> "commitTask"
            2 -> "versionTask"
            3 -> "changelogTask"
            4 -> "releaseTask"
            5 -> "createModuleTask"
            6 -> "buildTask"
            else -> null
        }

        if (nextTask == null) {
            println("Opcao invalida")
            return
        }

        val task = project.tasks.findByName(nextTask)

        if (task == null) {
            println("Task nao encontrada: $nextTask")
            return
        }

        println("\n-> Executando: $nextTask\n")

        task.actions.forEach { it.execute(task) }
    }

    // ================= COMMIT =================

    fun commit() {

        val options = listOf(
            "feat     -> nova funcionalidade (ex: adiciona sistema de login)",
            "fix      -> correcao de bug (ex: corrige erro ao carregar modulos)",
            "refactor -> melhoria interna (ex: refatora ModuleManager)",
            "perf     -> melhoria de performance (ex: otimiza carregamento)",
            "docs     -> documentacao (ex: atualiza README)",
            "style    -> formatacao (ex: identacao, lint)",
            "test     -> testes (ex: adiciona testes)",
            "chore    -> tarefas internas (ex: update deps)"
        )

        val index = choose("Tipo de commit:", options)

        val type = options
            .getOrNull(index - 1)
            ?.split(" ")
            ?.first()
            ?: return

        println("\nEscopo = onde voce mexeu (ex: core, module, command)")
        val scope = ask("Escopo (opcional): ")

        val desc = ask("Descricao: ")

        if (desc.isBlank()) error("Descricao obrigatoria")

        println("""
        
Breaking change (quebra compatibilidade)?
Exemplos:
- mudou API -> SIM
- removeu metodo -> SIM
- so corrigiu bug -> NAO
        
        """.trimIndent())

        val breaking = ask("Breaking change? (s/n): ").lowercase() == "s"

        val msg = buildString {
            append(type)
            if (scope.isNotBlank()) append("($scope)")
            if (breaking) append("!")
            append(": $desc")
        }

        println("\nCommit gerado:\n$msg")
        println("\nConfirmar? (s/n)")

        if (readLine()?.lowercase() != "s") {
            println("Commit cancelado")
            return
        }

        ProcessBuilder("git", "add", ".").inheritIO().start().waitFor()
        ProcessBuilder("git", "commit", "-m", msg).inheritIO().start().waitFor()
        ProcessBuilder("git", "push").inheritIO().start().waitFor()

        println("Commit enviado com sucesso")
    }

    // ================= VERSION =================

    fun version(project: Project) {

        val file = File(project.rootDir, "version.txt")

        if (!file.exists()) file.writeText("0.0.0")

        val (maj, min, pat) = file.readText().split(".").map { it.toInt() }

        println("\nVersao atual: $maj.$min.$pat")

        val type = ask("Tipo (major/minor/patch): ")

        val newVersion = when (type) {
            "major" -> "${maj + 1}.0.0"
            "minor" -> "$maj.${min + 1}.0"
            "patch" -> "$maj.$min.${pat + 1}"
            else -> error("Tipo invalido")
        }

        file.writeText(newVersion)

        println("Nova versao: $newVersion")
    }

    // ================= CHANGELOG =================

    fun changelog(project: Project) {

        val version = File(project.rootDir, "version.txt")
            .takeIf { it.exists() }
            ?.readText()?.trim() ?: "0.0.0"

        val logs = ProcessBuilder("git", "log", "--pretty=format:%s")
            .start()
            .inputStream
            .bufferedReader()
            .readLines()

        val file = File(project.rootDir, "CHANGELOG.md")

        file.appendText(buildString {
            append("## v$version\n")
            logs.take(10).forEach { append("- $it\n") }
            append("\n")
        })

        println("Changelog atualizado")
    }

    // ================= RELEASE =================

    fun release(project: Project) {

        val version = File(project.rootDir, "version.txt").readText().trim()

        println("\nCriando release v$version...\n")

        ProcessBuilder("git", "add", ".").inheritIO().start().waitFor()
        ProcessBuilder("git", "commit", "-m", "chore(release): v$version").inheritIO().start().waitFor()
        ProcessBuilder("git", "push").inheritIO().start().waitFor()

        ProcessBuilder("git", "tag", "v$version").inheritIO().start().waitFor()
        ProcessBuilder("git", "push", "origin", "v$version").inheritIO().start().waitFor()

        println("Release concluido com sucesso")
    }

    // ================= CREATE MODULE =================

    fun createModule(project: Project) {

        val name = ask("Nome do modulo: ")

        if (name.isBlank()) {
            println("Nome invalido")
            return
        }

        println("\nCriando modulo...\n")

        project.extensions.extraProperties.set("module", name)

        val task = project.tasks.getByName("createModule")
        task.actions.forEach { it.execute(task) }

        println("\nModulo criado com sucesso")

        println("""
        
Para o Gradle reconhecer o novo modulo:
- Execute: gradlew build
- Ou clique em Reload Gradle no IntelliJ
        
        """.trimIndent())
    }
}