package net.lazz.core.annotation

/**
 * Marca um método que será executado automaticamente
 * durante o carregamento inicial do módulo/serviço.
 *
 * Essa annotation representa uma fase anterior ao @OnEnable,
 * sendo usada para preparação básica antes da ativação completa.
 *
 * Funcionamento:
 * - Executado logo após a instanciação da classe.
 * - Ocorre antes da injeção de dependências (@Inject), dependendo da implementação.
 *
 * Ordem típica do lifecycle:
 * 1. Instanciação da classe
 * 2. Execução dos métodos @OnLoad
 * 3. Injeção de dependências (@Inject)
 * 4. Execução dos métodos @OnEnable
 *
 * Uso comum:
 * - Inicializar valores internos simples
 * - Preparar configurações básicas
 * - Definir estados iniciais
 * - Logs de carregamento
 *
 * ⚠ Observação:
 * Evite acessar dependências aqui, pois elas podem ainda não estar injetadas.
 *
 * Requisitos:
 * - O método não deve possuir parâmetros.
 * - Pode ser private, protected ou public (via reflection).
 *
 * Exemplo:
 * ```
 * class UserService {
 *
 *     @OnLoad
 *     fun load() {
 *         println("Carregando serviço...")
 *     }
 * }
 * ```
 *
 * Nesse caso, o método será chamado automaticamente
 * durante o processo de carregamento do módulo.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnLoad