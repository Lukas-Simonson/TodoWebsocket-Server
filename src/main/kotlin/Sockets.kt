package edu.dixietech.example.websockets

import edu.dixietech.example.websockets.model.Todo
import edu.dixietech.example.websockets.model.TodoRepository
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureSockets() {

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                classDiscriminator = "message_type"
                // ALL_JSON_OBJECTS as ktor uses reflection for its JSON Serialization
                classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS
            }
        )
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        val serverMessageFlow = MutableSharedFlow<ServerMessage>()
        val sharedFlow = serverMessageFlow.asSharedFlow()

        webSocket("/todos") {
            // Send all Todos to new Connections
            sendSerialized(ServerMessage.AllTodos(TodoRepository.allTodos()))

            // Subscribe to serverMessageFlow To Handle Changes
            val messageSubscription = launch {
                sharedFlow.collect {
                    println("Sending Message")
                    sendSerialized(it)
                }
            }

            incoming.consumeAsFlow()
                .mapNotNull { converter?.deserialize<ClientMessage>(it) }
                .onCompletion { messageSubscription.cancel() } // Handle Closure Of Websocket
                .collect {
                    when (it) {
                        is ClientMessage.AddTodo -> TodoRepository.addTodo(it.todo)
                        is ClientMessage.RemoveTodo -> TodoRepository.removeTodo(it.todo)
                        is ClientMessage.ToggleTodoCompletion -> TodoRepository.toggleTodoCompleted(
                            it.todo
                        )
                    }

                    // Update Every Subscriber with New Information
                    serverMessageFlow.emit(ServerMessage.AllTodos(TodoRepository.allTodos()))
                }
//            }
        }
    }
}

@Serializable
sealed class ClientMessage {
    @Serializable
    @SerialName("add_todo")
    data class AddTodo(val todo: Todo) : ClientMessage()

    @Serializable
    @SerialName("remove_todo")
    data class RemoveTodo(val todo: Todo) : ClientMessage()

    @Serializable
    @SerialName("toggle_todo_completion")
    data class ToggleTodoCompletion(val todo: Todo) : ClientMessage()
}

@Serializable
sealed class ServerMessage {
    @Serializable
    @SerialName("all_todos")
    data class AllTodos(val todos: List<Todo>) : ServerMessage()
}
