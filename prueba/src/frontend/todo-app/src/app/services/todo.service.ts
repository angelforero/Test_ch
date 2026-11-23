import { Injectable, signal, computed } from '@angular/core';
import { Todo } from '../models/todo.model';

@Injectable({
  providedIn: 'root'
})
export class TodoService {
  private _todos = signal<Todo[]>([
    { id: 1, title: 'Aprender Angular Signals', completed: true, createdAt: new Date() },
    { id: 2, title: 'Construir una To-Do App', completed: false, createdAt: new Date() },
    { id: 3, title: 'Explorar Angular 19', completed: false, createdAt: new Date() },
  ]);

  private _filter = signal<'all' | 'active' | 'completed'>('all');

  // Computed signals
  todos = computed(() => {
    const filter = this._filter();
    const todos = this._todos();
    switch (filter) {
      case 'active':    return todos.filter(t => !t.completed);
      case 'completed': return todos.filter(t => t.completed);
      default:          return todos;
    }
  });

  totalCount     = computed(() => this._todos().length);
  pendingCount   = computed(() => this._todos().filter(t => !t.completed).length);
  completedCount = computed(() => this._todos().filter(t => t.completed).length);
  currentFilter  = computed(() => this._filter());

  addTodo(title: string): void {
    const trimmed = title.trim();
    if (!trimmed) return;
    const newTodo: Todo = {
      id: Date.now(),
      title: trimmed,
      completed: false,
      createdAt: new Date()
    };
    this._todos.update(todos => [...todos, newTodo]);
  }

  toggleTodo(id: number): void {
    this._todos.update(todos =>
      todos.map(t => t.id === id ? { ...t, completed: !t.completed } : t)
    );
  }

  deleteTodo(id: number): void {
    this._todos.update(todos => todos.filter(t => t.id !== id));
  }

  clearCompleted(): void {
    this._todos.update(todos => todos.filter(t => !t.completed));
  }

  setFilter(filter: 'all' | 'active' | 'completed'): void {
    this._filter.set(filter);
  }
}
