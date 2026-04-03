package net.lazz.core.annotation

/**
 * Marca um método que será executado automaticamente
 * quando o módulo/serviço for desativado.
 *
 * Essa annotation é utilizada pelo sistema de gerenciamento
 * de módulos (ex: ModuleManager) para identificar métodos
 * de finalização e limpeza.
 *
 * Funcionamento:
 * - Durante o processo de unload/disable do módulo,
 *   todos os métodos anotados com @OnDisable serão invocados.
 *
 * Uso comum:
 * - Fechar conexões (database, sockets, etc.)
 * - Cancelar tasks/threads
 * - Salvar dados pendentes
 * - Liberar recursos
 *
 * Requisitos:
 * - O método não deve possuir parâmetros.
 * - Pode ser private, protected ou public (via reflection).
 *
 * Exemplo:
 * ```
 * class UserService {
 *
 *     @OnDisable
 *     fun shutdown() {
 *         println("Encerrando serviço...")
 *     }
 * }
 * ```
 *
 * Nesse caso, o método será chamado automaticamente
 * quando o módulo for desativado.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnDisable