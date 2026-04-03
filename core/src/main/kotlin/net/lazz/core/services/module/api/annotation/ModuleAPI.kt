package net.lazz.core.services.module.api.annotation

/**
 * Marca uma classe como uma API pública de módulo.
 *
 * Essa annotation expõe uma instância dentro do sistema de módulos para que outros módulos
 * possam acessar seus métodos de forma dinâmica, sem depender diretamente da implementação.
 *
 * A API registrada pode ser consumida de três formas principais:
 *
 * 1) por nome, usando `ModuleAPI.call(...)`
 * 2) por retorno tipado, usando `ModuleAPI.callAs<T>(...)`
 * 3) via `ModuleCallable`, evitando reflection
 *
 * ------------------------------------------------------------------------------
 * EXEMPLO 1 — MÓDULO QUE FORNECE A API
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
 * Nesse exemplo, o módulo "money" expõe uma API que pode ser chamada por outros módulos.
 *
 * ------------------------------------------------------------------------------
 * EXEMPLO 2 — MÓDULO QUE CONSOME A API
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @Depend("money")
 * class ShopService {
 *
 *     fun buy(player: Player) {
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
 *     }
 * }
 * ```
 *
 * O módulo consumidor depende do módulo "money" e utiliza a API sem conhecer a implementação.
 *
 * ------------------------------------------------------------------------------
 * EXEMPLO 3 — API DE CHAT
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @ModuleAPI("chat")
 * class ChatService {
 *
 *     fun send(player: Player, message: String) {
 *         player.sendMessage(message)
 *     }
 *
 *     fun broadcast(message: String) {
 *         Bukkit.broadcastMessage(message)
 *     }
 * }
 * ```
 *
 * Uso:
 *
 * ```kotlin
 * ModuleAPI.call("chat", "send", player, "Olá!")
 * ModuleAPI.call("chat", "broadcast", "Servidor iniciado!")
 * ```
 *
 * ------------------------------------------------------------------------------
 * EXEMPLO 4 — RETORNOS DIFERENTES
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @ModuleAPI("profile")
 * class ProfileService {
 *
 *     fun getName(uuid: UUID): String {
 *         return "Player_" + uuid.toString().take(5)
 *     }
 *
 *     fun isVip(uuid: UUID): Boolean {
 *         return true
 *     }
 * }
 * ```
 *
 * Uso:
 *
 * ```kotlin
 * val name = ModuleAPI.callAs<String>("profile", "getName", uuid)
 * val vip = ModuleAPI.callAs<Boolean>("profile", "isVip", uuid)
 * ```
 *
 * ------------------------------------------------------------------------------
 * FORMAS DE ACESSO
 * ------------------------------------------------------------------------------
 *
 * - `ModuleAPI.call("money", "get", uuid)`
 *   Retorna `Any?`
 *
 * - `ModuleAPI.callAs<Int>("money", "get", uuid)`
 *   Retorna tipado (recomendado quando possível)
 *
 * ------------------------------------------------------------------------------
 * COMO FUNCIONA INTERNAMENTE
 * ------------------------------------------------------------------------------
 *
 * Quando o módulo é carregado, o service anotado com `@ModuleAPI` é registrado
 * no contexto do módulo (`namedServices`).
 *
 * Ao chamar `ModuleAPI.call(...)`:
 *
 * 1) o sistema busca a instância pelo nome
 * 2) se for `ModuleCallable`, usa `call(...)`
 * 3) senão, usa reflection automaticamente
 *
 * ------------------------------------------------------------------------------
 * BOAS PRÁTICAS
 * ------------------------------------------------------------------------------
 *
 * - Use nomes curtos e claros:
 *   "money", "chat", "profile", "party", "warp"
 *
 * - Declare `@Depend("modulo")` quando consumir APIs externas
 *
 * - Use `ModuleCallable` para melhor performance
 *
 * - Use `callAs<T>` quando souber o retorno
 *
 * - Evite lógica complexa baseada em string
 *
 * ------------------------------------------------------------------------------
 * RESUMO
 * ------------------------------------------------------------------------------
 *
 * `@ModuleAPI` transforma uma classe em uma API acessível por outros módulos,
 * permitindo comunicação desacoplada, simples e flexível.
 *
 * Exemplo final:
 *
 * ```kotlin
 * ModuleAPI.call("money", "add", uuid, 50)
 * ```
 *
 * @param value Nome da API exposta dentro do módulo
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModuleAPI(
    val value: String
)