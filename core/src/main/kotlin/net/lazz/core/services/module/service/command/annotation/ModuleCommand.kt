package net.lazz.core.services.module.service.command.annotation

/**
 * Marca uma classe como um comando de módulo.
 *
 * Essa annotation é usada pelo sistema para detectar automaticamente comandos
 * dentro dos módulos durante o processo de scan, registrando-os no Bukkit
 * sem necessidade de configuração manual no plugin.yml.
 *
 * ------------------------------------------------------------------------------
 * 🧠 COMO FUNCIONA
 * ------------------------------------------------------------------------------
 *
 * - O scanner (`ModuleCommandScanner`) identifica classes com essa annotation
 * - A classe deve implementar `CommandExecutor`
 * - O comando é registrado dinamicamente no servidor
 * - Permissões e cooldown são tratados automaticamente pelo sistema
 *
 *
 * ------------------------------------------------------------------------------
 * 📦 EXEMPLO BÁSICO
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @ModuleCommand(name = "ping")
 * class PingCommand : CommandExecutor {
 *
 *     override fun onCommand(
 *         sender: CommandSender,
 *         command: Command,
 *         label: String,
 *         args: Array<String>
 *     ): Boolean {
 *
 *         sender.sendMessage("§aPong!")
 *         return true
 *     }
 * }
 * ```
 *
 *
 * ------------------------------------------------------------------------------
 * 📦 EXEMPLO COMPLETO (COM PERMISSÃO E COOLDOWN)
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @ModuleCommand(
 *     name = "money",
 *     aliases = ["bal", "saldo"],
 *     permission = "module.money",
 *     permissionMessage = "§cVocê não pode usar esse comando.",
 *     cooldown = 3000
 * )
 * class MoneyCommand : CommandExecutor {
 *
 *     override fun onCommand(
 *         sender: CommandSender,
 *         command: Command,
 *         label: String,
 *         args: Array<String>
 *     ): Boolean {
 *
 *         if (sender !is Player) return true
 *
 *         val uuid = sender.uniqueId
 *
 *         val money = ModuleAPI.callAs<Int>("money", "get", uuid) ?: 0
 *
 *         sender.sendMessage("§aSeu saldo: $money")
 *         return true
 *     }
 * }
 * ```
 *
 *
 * ------------------------------------------------------------------------------
 * ⚙️ COMPORTAMENTO
 * ------------------------------------------------------------------------------
 *
 * ✔ Registro automático no Bukkit
 * ✔ Suporte a aliases
 * ✔ Verificação de permissão automática
 * ✔ Mensagem customizável de permissão
 * ✔ Cooldown por jogador
 *
 *
 * ------------------------------------------------------------------------------
 * ⏱️ COOLDOWN
 * ------------------------------------------------------------------------------
 *
 * - Definido em milissegundos
 *
 * ```kotlin
 * cooldown = 3000 // 3 segundos
 * ```
 *
 * O sistema impedirá que o jogador execute o comando novamente
 * até o tempo acabar.
 *
 *
 * ------------------------------------------------------------------------------
 * 🔐 PERMISSÕES
 * ------------------------------------------------------------------------------
 *
 * Se `permission` for definido:
 *
 * - O sistema verifica automaticamente antes de executar
 * - Caso não tenha permissão, envia `permissionMessage`
 *
 * ```kotlin
 * permission = "module.admin"
 * ```
 *
 *
 * ------------------------------------------------------------------------------
 * 💡 BOAS PRÁTICAS
 * ------------------------------------------------------------------------------
 *
 * - Use nomes curtos e claros para comandos
 * - Sempre valide se o sender é Player quando necessário
 * - Use aliases para melhorar UX
 * - Combine com ModuleAPI para integrar módulos
 *
 *
 * ------------------------------------------------------------------------------
 * 🔗 INTEGRAÇÃO COM MÓDULOS
 * ------------------------------------------------------------------------------
 *
 * Comandos podem acessar APIs de outros módulos:
 *
 * ```kotlin
 * val result = ModuleAPI.call("economy", "get", uuid)
 * ```
 *
 * Isso permite criar comandos altamente dinâmicos e desacoplados.
 *
 *
 * ------------------------------------------------------------------------------
 * 🧠 RESUMO
 * ------------------------------------------------------------------------------
 *
 * `@ModuleCommand` define comandos que são automaticamente registrados e gerenciados
 * pelo sistema de módulos, incluindo permissões e cooldown, sem necessidade de
 * configuração manual.
 *
 * @param name Nome principal do comando
 * @param aliases Aliases do comando (opcional)
 * @param permission Permissão necessária (opcional)
 * @param permissionMessage Mensagem ao não ter permissão
 * @param cooldown Tempo de cooldown em ms (0 = sem cooldown)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModuleCommand(
    val name: String,
    val aliases: Array<String> = [],
    val permission: String = "",
    val permissionMessage: String = "§cSem permissão.",
    val cooldown: Long = 0
)