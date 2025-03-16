package edu.dixietech.example.websockets.model

import java.util.Collections

object TodoRepository {
    private val todos = Collections.synchronizedList(mutableListOf<Todo>())

    // Read functions that iterate, need to be manually synchronized.
    fun allTodos(): List<Todo> = synchronized(todos) { todos.toList() }

    // Modifying Functions are automatically synchronized by the synchronized list.
    fun addTodo(todo: Todo) = todos.add(todo)
    fun removeTodo(todo: Todo) = todos.remove(todo)

    // We need to iterate (indexOf) so we need to manually synchronize.
    fun toggleTodoCompleted(todo: Todo) {
        synchronized(todos) {
            todos.indexOfOrNull(todo)?.let {
                todos[it] = todo.copy(isCompleted = !todo.isCompleted)
            }
        }
    }
}

fun <T> List<T>.indexOfOrNull(element: T) = indexOf(element).takeIf { it != -1 }