package net.lazz.core.annotation

/**
 * Marca um campo para injeção automática de dependência.
 *
 * Essa annotation é utilizada pelo sistema de injeção (ex: ModuleDependencyInjector)
 * para identificar quais propriedades devem receber instâncias automaticamente.
 *
 * Funcionamento:
 * - O sistema irá procurar uma instância compatível com o tipo do campo.
 * - Se encontrada, ela será atribuída ao campo via reflection.
 *
 * Parâmetro:
 * @param optional Define se a dependência é opcional.
 *
 * Comportamento:
 * - optional = false (padrão):
 *   -> Se a dependência NÃO for encontrada, será lançado erro.
 *
 * - optional = true:
 *   -> Se não encontrar a dependência, o campo permanece null
 *      (ou valor padrão), sem interromper o fluxo.
 *
 * Requisitos:
 * - O campo deve ser `var` para permitir modificação via reflection.
 * - O tipo deve estar registrado no container de serviços/módulos.
 *
 * Exemplo:
 * ```
 * class UserService {
 *
 *     @Inject
 *     lateinit var database: DatabaseService
 *
 *     @Inject(optional = true)
 *     var cache: CacheService? = null
 * }
 * ```
 *
 * Nesse exemplo:
 * - database é obrigatório → erro se não existir
 * - cache é opcional → pode ser null
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject(
    val optional: Boolean = false
)