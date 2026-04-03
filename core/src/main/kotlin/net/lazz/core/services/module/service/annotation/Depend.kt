package net.lazz.core.services.module.service.annotation

/**
 * Declara que uma classe ou módulo depende de outros módulos para funcionar corretamente.
 *
 * Essa annotation é usada pelo sistema para garantir que os módulos informados sejam
 * carregados e ativados antes do módulo atual.
 *
 * Ela ajuda principalmente em:
 *
 * - ordem de carregamento
 * - validação de dependências
 * - prevenção de falhas por API ausente
 * - comunicação entre módulos
 *
 * ------------------------------------------------------------------------------
 * EXEMPLO 1 — MÓDULO QUE OFERECE UMA API
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @ModuleAPI("money")
 * class MoneyService : ModuleCallable {
 *
 *     private val data = ConcurrentHashMap<UUID, Int>()
 *
 *     fun get(uuid: UUID): Int {
 *         return data.getOrDefault(uuid, 0)
 *     }
 *
 *     fun add(uuid: UUID, value: Int) {
 *         data[uuid] = get(uuid) + value
 *     }
 *
 *     override fun call(method: String, vararg args: Any?): Any? {
 *         return when (method) {
 *             "get" -> get(args[0] as UUID)
 *             "add" -> {
 *                 add(args[0] as UUID, args[1] as Int)
 *                 null
 *             }
 *             else -> null
 *         }
 *     }
 * }
 * ```
 *
 * Esse módulo expõe a API `money`, que poderá ser consumida por outros módulos.
 *
 * ------------------------------------------------------------------------------
 * EXEMPLO 2 — MÓDULO QUE DEPENDE DA API
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @Depend("money")
 * class ShopModule(
 *     plugin: JavaPlugin
 * ) : AbstractModule(
 *     plugin = plugin,
 *     id = "shop"
 * ) {
 *
 *     override fun onEnable() {
 *         plugin.logger.info("Shop ativado")
 *     }
 * }
 * ```
 *
 * Nesse caso, o módulo `shop` só deve ser ativado depois que `money` estiver carregado.
 *
 * ------------------------------------------------------------------------------
 * EXEMPLO 3 — USO DA DEPENDÊNCIA NA PRÁTICA
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @Depend("money")
 * class ShopCommand : CommandExecutor {
 *
 *     override fun onCommand(
 *         sender: CommandSender,
 *         command: Command,
 *         label: String,
 *         args: Array<String>
 *     ): Boolean {
 *
 *         val player = sender as? Player ?: return true
 *         val uuid = player.uniqueId
 *
 *         val money = ModuleAPI.callAs<Int>("money", "get", uuid) ?: 0
 *
 *         if (money >= 100) {
 *             ModuleAPI.call("money", "add", uuid, -100)
 *             player.sendMessage("Compra realizada com sucesso!")
 *         } else {
 *             player.sendMessage("Você não tem dinheiro suficiente.")
 *         }
 *
 *         return true
 *     }
 * }
 * ```
 *
 * Aqui o módulo consumidor depende do módulo `money` para acessar sua API.
 *
 * ------------------------------------------------------------------------------
 * COMPORTAMENTO
 * ------------------------------------------------------------------------------
 *
 * Quando o sistema carrega os módulos, ele usa essa annotation para:
 *
 * - resolver a ordem correta de load
 * - impedir ativação de módulos sem dependência disponível
 * - evitar chamadas para APIs que ainda não existem
 *
 * ------------------------------------------------------------------------------
 * BOAS PRÁTICAS
 * ------------------------------------------------------------------------------
 *
 * - Use nomes de módulos exatamente como eles são registrados
 * - Declare todas as dependências reais do módulo
 * - Mantenha dependências diretas e claras
 * - Evite depender de módulos desnecessários
 *
 * ------------------------------------------------------------------------------
 * RESUMO
 * ------------------------------------------------------------------------------
 *
 * `@Depend` informa ao sistema que um módulo precisa de outros módulos para funcionar.
 * Isso garante que as dependências sejam carregadas antes e que a integração entre
 * módulos aconteça de forma segura.
 *
 * @param value Lista de IDs dos módulos dos quais este módulo depende
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Depend(
    vararg val value: String
)