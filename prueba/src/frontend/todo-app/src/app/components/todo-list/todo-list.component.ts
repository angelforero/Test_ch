import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TodoService } from '../../services/todo.service';
import { TodoItemComponent } from '../todo-item/todo-item.component';

@Component({
  selector: 'app-todo-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TodoItemComponent],
  template: `
    <div class="todo-wrapper">
      <header class="todo-header">
        <h1>📝 Mis Tareas</h1>
        <p class="subtitle">{{ todoService.pendingCount() }} tarea(s) pendiente(s)</p>
      </header>

      <!-- Input para nueva tarea -->
      <div class="add-todo">
        <input
          type="text"
          [(ngModel)]="newTitle"
          (keyup.enter)="addTodo()"
          placeholder="¿Qué necesitas hacer hoy?"
          class="todo-input"
          maxlength="100"
        />
        <button (click)="addTodo()" class="add-btn" [disabled]="!newTitle.trim()">
          Agregar
        </button>
      </div>

      <!-- Filtros -->
      <div class="filters">
        @for (f of filters; track f.value) {
          <button
            class="filter-btn"
            [class.active]="todoService.currentFilter() === f.value"
            (click)="setFilter(f.value)"
          >
            {{ f.label }}
          </button>
        }
      </div>

      <!-- Lista de tareas -->
      <ul class="todo-list">
        @if (todoService.todos().length === 0) {
          <li class="empty-state">
            @if (todoService.currentFilter() === 'completed') {
              <span>🎯 Aún no has completado ninguna tarea</span>
            } @else if (todoService.currentFilter() === 'active') {
              <span>🎉 ¡No hay tareas pendientes!</span>
            } @else {
              <span>✨ Agrega tu primera tarea arriba</span>
            }
          </li>
        }
        @for (todo of todoService.todos(); track todo.id) {
          <app-todo-item
            [todo]="todo"
            (toggle)="todoService.toggleTodo($event)"
            (delete)="todoService.deleteTodo($event)"
          />
        }
      </ul>

      <!-- Footer -->
      @if (todoService.totalCount() > 0) {
        <footer class="todo-footer">
          <span>{{ todoService.completedCount() }}/{{ todoService.totalCount() }} completadas</span>
          @if (todoService.completedCount() > 0) {
            <button class="clear-btn" (click)="todoService.clearCompleted()">
              Limpiar completadas
            </button>
          }
        </footer>
      }
    </div>
  `,
  styles: [`
    .todo-wrapper {
      background: #fff;
      border-radius: 16px;
      box-shadow: 0 10px 40px rgba(0,0,0,0.12);
      overflow: hidden;
      max-width: 560px;
      width: 100%;
    }

    .todo-header {
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      color: white;
      padding: 28px 24px 20px;
    }

    .todo-header h1 {
      margin: 0 0 4px 0;
      font-size: 1.75rem;
      font-weight: 700;
    }

    .subtitle {
      margin: 0;
      opacity: 0.85;
      font-size: 0.95rem;
    }

    .add-todo {
      display: flex;
      gap: 10px;
      padding: 20px 20px 0;
    }

    .todo-input {
      flex: 1;
      padding: 10px 14px;
      border: 2px solid #e5e7eb;
      border-radius: 8px;
      font-size: 0.95rem;
      outline: none;
      transition: border-color 0.2s;
    }

    .todo-input:focus {
      border-color: #4f46e5;
    }

    .add-btn {
      padding: 10px 20px;
      background: #4f46e5;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s, transform 0.1s;
      white-space: nowrap;
    }

    .add-btn:hover:not(:disabled) { background: #4338ca; }
    .add-btn:active:not(:disabled) { transform: scale(0.97); }
    .add-btn:disabled { background: #d1d5db; cursor: not-allowed; }

    .filters {
      display: flex;
      gap: 6px;
      padding: 16px 20px;
    }

    .filter-btn {
      padding: 6px 14px;
      border: 2px solid #e5e7eb;
      border-radius: 20px;
      background: none;
      font-size: 0.85rem;
      cursor: pointer;
      color: #6b7280;
      transition: all 0.15s;
    }

    .filter-btn:hover { border-color: #4f46e5; color: #4f46e5; }

    .filter-btn.active {
      background: #4f46e5;
      border-color: #4f46e5;
      color: white;
      font-weight: 600;
    }

    .todo-list {
      list-style: none;
      margin: 0;
      padding: 0;
      border-top: 1px solid #f3f4f6;
      max-height: 400px;
      overflow-y: auto;
    }

    .empty-state {
      padding: 40px 20px;
      text-align: center;
      color: #9ca3af;
      font-size: 0.95rem;
    }

    .todo-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 20px;
      border-top: 1px solid #f3f4f6;
      background: #f9fafb;
      font-size: 0.85rem;
      color: #6b7280;
    }

    .clear-btn {
      background: none;
      border: none;
      color: #ef4444;
      font-size: 0.85rem;
      cursor: pointer;
      text-decoration: underline;
    }

    .clear-btn:hover { color: #dc2626; }
  `]
})
export class TodoListComponent {
  todoService = inject(TodoService);
  newTitle = '';

  filters = [
    { label: 'Todas', value: 'all' as const },
    { label: 'Pendientes', value: 'active' as const },
    { label: 'Completadas', value: 'completed' as const },
  ];

  addTodo(): void {
    if (!this.newTitle.trim()) return;
    this.todoService.addTodo(this.newTitle);
    this.newTitle = '';
  }

  setFilter(value: 'all' | 'active' | 'completed'): void {
    this.todoService.setFilter(value);
  }
}
