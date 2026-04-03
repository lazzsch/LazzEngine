package net.lazz.core.annotation

import kotlin.reflect.KClass

/**
 * Define dependências entre classes dentro do sistema de módulos/serviços.
 *
 * Essa annotation é usada para indicar que uma determinada classe
 * depende de outras classes para funcionar corretamente.
 *
 * As classes informadas serão utilizadas, por exemplo, em processos de:
 * - Injeção de dependência
 * - Ordem de carregamento
 * - Inicialização de módulos
 *
 * Funcionamento:
 * A classe anotada só deve ser inicializada após todas as dependências
 * declaradas estarem disponíveis/registradas.
 *
 * Exemplo de uso:
 * ```
 * @DependsOn(DatabaseService::class, CacheService::class)
 * class UserService
 * ```
 *
 * Nesse caso, o UserService depende de DatabaseService e CacheService,
 * então esses serviços devem ser carregados primeiro.
 *
 * @param value Lista de classes das quais esta classe depende.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOn(
    vararg val value: KClass<*>
)