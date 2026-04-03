package net.lazz.core.annotation

/**
 * Marca uma classe como um serviço gerenciado pelo sistema de módulos.
 *
 * Classes anotadas com @Service serão automaticamente detectadas,
 * instanciadas e registradas no container de serviços.
 *
 * Funcionamento:
 * - O scanner identifica classes anotadas com @Service.
 * - Uma instância é criada automaticamente.
 * - A classe passa pelo lifecycle completo:
 *   1. Instanciação
 *   2. @OnLoad
 *   3. Injeção de dependências (@Inject)
 *   4. @OnEnable
 *
 * Integração:
 * - Pode receber dependências via @Inject
 * - Pode declarar dependências via @DependsOn
 * - Pode possuir métodos @OnLoad, @OnEnable e @OnDisable
 *
 * Uso comum:
 * - Serviços de lógica (ex: UserService, EconomyService)
 * - Integrações (database, APIs, cache)
 * - Gerenciadores internos (managers)
 *
 * Exemplo:
 * ```
 * @Service
 * class UserService {
 *
 *     @Inject
 *     lateinit var database: DatabaseService
 *
 *     @OnEnable
 *     fun start() {
 *         println("UserService iniciado!")
 *     }
 * }
 * ```
 *
 * Nesse caso, o UserService será automaticamente registrado,
 * terá suas dependências injetadas e será iniciado pelo sistema.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Service