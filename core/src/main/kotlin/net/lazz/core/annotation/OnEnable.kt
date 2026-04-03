package net.lazz.core.annotation

/**
 * Marca um método que será executado automaticamente
 * quando o módulo/serviço for ativado.
 *
 * Essa annotation é utilizada pelo sistema de gerenciamento
 * de módulos (ex: ModuleManager) para identificar métodos
 * de inicialização.
 *
 * Funcionamento:
 * - Após a criação da instância e injeção de dependências,
 *   todos os métodos anotados com @OnEnable serão invocados.
 *
 * Ordem típica:
 * 1. Instanciação da classe
 * 2. Injeção de dependências (@Inject)
 * 3. Execução dos métodos @OnEnable
 *
 * Uso comum:
 * - Inicializar dados
 * - Registrar listeners/comandos
 * - Iniciar tasks
 * - Validar dependências em runtime
 *
 * Requisitos:
 * - O método não deve possuir parâmetros.
 * - Pode ser private, protected ou public (via reflection).
 *
 * Exemplo:
 * ```
 * class UserService {
 *
 *     @OnEnable
 *     fun init() {
 *         println("Serviço iniciado!")
 *     }
 * }
 * ```
 *
 * Nesse caso, o método será chamado automaticamente
 * após o serviço ser carregado.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnEnable