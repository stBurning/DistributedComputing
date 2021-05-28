import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskDispatcher(timeDelay: Long = 100) {
    private val pool = Channel<Task>()
    init {
        CoroutineScope(Dispatchers.IO).launch {
        while (true){
            delay(timeDelay)
            pool.receive().run()
        }


        }
    }
    suspend fun addTask(task: Task){
        pool.send(task)
    }

}


interface Task{
    fun run()
}