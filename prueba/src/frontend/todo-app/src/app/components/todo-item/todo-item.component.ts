import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Todo } from '../../models/todo.model';

@Component({
  selector: 'app-todo-item',
  standalone: true,
  imports: [CommonModule],
  template: `
    <li class="todo-item" [class.completed]="todo.completed">
      <label class="todo-label">
        <input
          type="checkbox"
          [checked]="todo.completed"
          (change)="toggle.emit(todo.id)"
          class="todo-checkbox"
        />
        <span class="todo-title">{{ todo.title }}</span>
      </label>
      <button class="delete-btn" (click)="delete.emit(todo.id)" title="Eliminar tarea">
        ✕
      </button>
    </li>
  `,
  styles: [`
    .todo-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 16px;
      border-bottom: 1px solid #eee;
      transition: background 0.15s;
    }
    .todo-item:hover { background: #fafafa; }
    .todo-item:last-child { border-bottom: none; }

    .todo-label {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      flex: 1;
    }

    .todo-checkbox {
      width: 20px;
      height: 20px;
      cursor: pointer;
      accent-color: #4f46e5;
    }

    .todo-title {
      font-size: 1rem;
      color: #374151;
      transition: all 0.2s;
    }

    .completed .todo-title {
      text-decoration: line-through;
      color: #9ca3af;
    }

    .delete-btn {
      background: none;
      border: none;
      color: #d1d5db;
      font-size: 1rem;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: color 0.15s, background 0.15s;
      opacity: 0;
    }

    .todo-item:hover .delete-btn {
      opacity: 1;
    }

    .delete-btn:hover {
      color: #ef4444;
      background: #fee2e2;
    }
  `]
})
export class TodoItemComponent {
  @Input() todo!: Todo;
  @Output() toggle = new EventEmitter<number>();
  @Output() delete = new EventEmitter<number>();
}
