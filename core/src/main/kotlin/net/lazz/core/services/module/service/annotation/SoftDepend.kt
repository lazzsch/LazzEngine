package net.lazz.core.services.module.service.annotation

/**
 * Declara uma dependência opcional entre módulos.
 *
 * Diferente de `@Depend`, essa annotation NÃO impede o módulo de ser carregado
 * caso a dependência não exista ou não esteja ativada.
 *
 * Ela é usada quando um módulo pode se integrar com outro, mas não depende dele
 * obrigatoriamente para funcionar.
 *
 * ------------------------------------------------------------------------------
 * 🧠 QUANDO USAR
 * ------------------------------------------------------------------------------
 *
 * Use `@SoftDepend` quando:
 *
 * - a integração é opcional
 * - o módulo pode funcionar sozinho
 * - você quer adicionar suporte a outro módulo se ele estiver presente
 *
 *
 * ------------------------------------------------------------------------------
 * 📦 EXEMPLO 1 — INTEGRAÇÃO OPCIONAL
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @SoftDepend("money")
 * class ShopModule(
 *     plugin: JavaPlugin
 * ) : AbstractModule(
 *     plugin = plugin,
 *     id = "shop"
 * ) {
 *
 *     override fun onEnable() {
 *         plugin.logger.info("Shop carregado")
 *     }
 * }
 * ```
 *
 * Aqui o módulo "shop" funciona normalmente mesmo sem o módulo "money".
 *
 *
 * ------------------------------------------------------------------------------
 * 📦 EXEMPLO 2 — USO CONDICIONAL DA API
 * ------------------------------------------------------------------------------
 *
 * ```kotlin
 * @SoftDepend("money")
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
 *         val money = ModuleAPI.callAs<Int>("money", "get", uuid)
 *
 *         if (money == null) {
 *             player.sendMessage("Sistema de dinheiro não disponível.")
 *             return true
 *         }
 *
 *         player.sendMessage("Você tem: $money")
 *         return true
 *     }
 * }
 * ```
 *
 * Aqui a chamada só acontece se o módulo estiver presente.
 *
 *
 * ------------------------------------------------------------------------------
 * ⚠️ COMPORTAMENTO
 * ------------------------------------------------------------------------------
 *
 * - NÃO bloqueia o carregamento do módulo
 * - NÃO garante que a dependência estará disponível
 * - NÃO garante ordem de carregamento
 *
 * Ou seja:
 *
 * ```kotlin
 * ModuleAPI.call("money", ...)
 * ```
 *
 * pode retornar `null` se o módulo não estiver presente.
 *
 *
 * ------------------------------------------------------------------------------
 * 💡 BOAS PRÁTICAS
 * ------------------------------------------------------------------------------
 *
 * - Sempre verifique se a API existe antes de usar
 *
 * ```kotlin
 * val result = ModuleAPI.call("money", "get", uuid)
 * if (result != null) {
 *     // usar
 * }
 * ```
 *
 * - Nunca assuma que a dependência está disponível
 * - Use fallback quando necessário
 *
 *
 * ------------------------------------------------------------------------------
 * 🧠 RESUMO
 * ------------------------------------------------------------------------------
 *
 * `@SoftDepend` indica que um módulo pode integrar com outro, mas não depende dele.
 * O sistema não força a existência da dependência, deixando a responsabilidade
 * de verificação para o desenvolvedor.
 *
 * @param value Lista de módulos opcionais
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SoftDepend(
    vararg val value: String
)