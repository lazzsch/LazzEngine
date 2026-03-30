package net.lazz.core.service.task

import net.lazz.core.Core
import net.lazz.core.service.task.enum.TaskState
import net.lazz.core.service.task.manager.AsyncTaskManager
import net.lazz.core.service.task.manager.SyncTaskManager
import net.lazz.core.service.task.monitor.TaskProfiler
import org.bukkit.Bukkit
import kotlin.time.Duration

class TaskManager(plugin: Core) {

    /**
     * ⚠ REGRA DE OURO:
     *
     * SYNC  → Bukkit API (player, mundo)
     * ASYNC → lógica pesada (DB, cálculo, IO)
     *
     * Nunca misture os dois diretamente.
     */

    private val profiler = TaskProfiler()

    private val sync = SyncTaskManager(plugin, profiler) { logs }
    private val async = AsyncTaskManager(profiler, { logs })

    // ================= DEBUG / STATE =================

    @Volatile
    var logs: Boolean = false

    fun getAllTasks(): List<String> {
        val asyncTasks = async.getTaskNames()
        val syncTasks = sync.getTaskNames()

        return (asyncTasks + syncTasks).distinct()
    }

    /**
     * Retorna o TPS atual do servidor.
     *
     * ✔ Usa reflexão para compatibilidade com Paper/Spigot
     * ✔ Retorna o TPS dos últimos 1 minuto (tps[0])
     *
     * ⚠ Em servidores que não suportam getTPS(), retorna 20.0 como fallback
     *
     * @return TPS atual (Double)
     */
    fun getTPS(): Double {
        return try {
            val server = Bukkit.getServer()
            val method = server.javaClass.getMethod("getTPS")
            val tps = method.invoke(server) as DoubleArray
            tps[0]
        } catch (_: Exception) {
            20.0
        }
    }

    fun getColoredTPS(): String {
        val tps = getTPS()
        val color = when {
            tps >= 19 -> "§a"
            tps >= 17 -> "§e"
            tps >= 15 -> "§6"
            else -> "§c"
        }
        return "$color${"%.2f".format(tps)}"
    }

    /**
     * Retorna a quantidade de tasks async registradas no sistema.
     *
     * ✔ Representa tasks ativas/pendentes no AsyncTaskManager
     * ✔ Útil para debug e monitoramento
     *
     * @return quantidade de tasks
     */
    fun getQueueSize(): Int {
        return async.getQueueSize()
    }

    /**
     * Retorna a quantidade de threads atualmente em uso no executor async.
     *
     * ✔ Baseado no ThreadPoolExecutor
     * ✔ Mostra threads que estão executando tasks no momento
     *
     * @return número de threads ativas
     */
    fun getActiveThreads(): Int {
        return async.getActiveThreads()
    }

    /**
     * Retorna o estado geral do servidor baseado no TPS.
     *
     * ✔ Classificação simples para UI/Admin:
     * - ESTÁVEL   (>= 19 TPS)
     * - MODERADO  (>= 17 TPS)
     * - SOB CARGA (>= 15 TPS)
     * - CRÍTICO   (< 15 TPS)
     *
     * ✔ Usado em menus/admin/debug
     *
     * @return string formatada com cor do Minecraft
     */
    fun getState(): TaskState {
        val tps = getTPS()

        return when {
            tps >= 19 -> TaskState.STABLE
            tps >= 17 -> TaskState.MODERATE
            tps >= 15 -> TaskState.UNDER_LOAD
            else -> TaskState.CRITICAL
        }
    }

    /**
     * Retorna as tasks mais pesadas (top lag) registradas pelo profiler.
     *
     * ✔ Ordena por maior tempo de execução (max)
     * ✔ Retorna as principais tasks que causam impacto
     *
     * ✔ Formato:
     * "#1 task-name | avg=5ms | max=40ms"
     *
     * ✔ Usado em:
     * - Menu admin
     * - Debug
     * - Diagnóstico de lag
     *
     * @return lista formatada das top tasks
     */
    fun getTopTasks(): List<String> {
        return dumpTop()
    }

    // ========================= SYNC =========================

    /**
     * Executa uma task na thread principal do servidor (Bukkit).
     *
     * ✔ Usar para:
     * - Player (mensagens, teleport, inventário)
     * - Mundo (blocos, entidades)
     *
     * ⚠ Nunca colocar operações pesadas aqui (DB, HTTP, loops grandes)
     *
     * @param name Nome único da task (usado para controle/cancelamento)
     */
    fun runSync(name: String, task: () -> Unit) =
        sync.run(name, task)

    /**
     * Executa uma task na thread principal após um delay (em ticks).
     *
     * ✔ 20 ticks = 1 segundo
     *
     * ✔ Usar para:
     * - Ações atrasadas (ex: abrir menu depois de X ticks)
     *
     * @param delayTicks Tempo de espera em ticks
     */
    fun runSyncLater(name: String, delayTicks: Long, task: () -> Unit) =
        sync.runLater(name, delayTicks, task)

    /**
     * Executa uma task repetidamente na thread principal.
     *
     * ✔ Usar para:
     * - Atualizações visuais
     * - Scoreboard
     * - Animações
     *
     * ⚠ Cuidado: roda na main thread → pode causar lag
     *
     * @param delayTicks Delay inicial
     * @param periodTicks Intervalo entre execuções
     */
    fun runTimerSync(name: String, delayTicks: Long, periodTicks: Long, task: () -> Unit) =
        sync.runTimer(name, delayTicks, periodTicks, task)


    // ========================= ASYNC =========================

    /**
     * Executa uma task em thread separada (assíncrona).
     *
     * ✔ Usar para:
     * - Banco de dados
     * - HTTP/API
     * - Cálculos pesados
     *
     * ⚠ NÃO usar Bukkit API aqui
     *
     * @param name Nome único da task
     */
    fun runAsync(name: String, task: () -> Unit) =
        async.run(name, task)

    /**
     * Executa uma task async após um delay.
     *
     * ✔ Ideal para:
     * - Retry de conexão
     * - Espera não bloqueante
     *
     * @param delay Tempo de espera (Duration)
     */
    fun runAsyncLater(name: String, delay: Duration, task: () -> Unit) =
        async.runLater(name, delay.inWholeMilliseconds, task)

    /**
     * Executa uma task async repetidamente.
     *
     * ✔ Usar para:
     * - Monitoramento (DB, conexão)
     * - Sistemas de background
     *
     * ⚠ Evitar intervalos muito curtos (<50ms)
     *
     * @param interval Intervalo entre execuções
     */
    fun runTimerAsync(name: String, interval: Duration, task: () -> Unit) =
        async.runTimer(name, interval.inWholeMilliseconds, interval.inWholeMilliseconds, task)


    // ========================= CONTROL =========================

    /**
     * Cancela uma task pelo nome.
     *
     * ✔ Cancela tanto sync quanto async
     * ✔ Seguro mesmo se não existir
     *
     * @param name Nome da task
     */
    fun cancel(name: String) {
        sync.cancel(name)
        async.cancel(name)
    }

    /**
     * Cancela TODAS as tasks registradas.
     *
     * ✔ Usar em:
     * - shutdown do plugin
     * - reload
     */
    fun cancelAll() {
        sync.cancelAll()
        async.cancelAll()
    }

    fun shutdown() {
        cancelAll()
        async.shutdown()
    }

    // ================= DEBUG =================

    fun dumpTop(): List<String> {
        return profiler.snapshot()
            .map { (name, stat) ->
                val avg = stat.totalTime.get() / (stat.executions.get().coerceAtLeast(1))
                val max = stat.maxTime.get()

                Triple(name, avg / 1_000_000, max / 1_000_000)
            }
            .sortedByDescending { it.third }
            .take(10)
            .mapIndexed { i, (name, avg, max) ->
                "#${i + 1} $name | avg=${avg}ms | max=${max}ms"
            }
    }
}