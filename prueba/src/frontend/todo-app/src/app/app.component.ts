import { Component } from '@angular/core';
import { TodoListComponent } from './components/todo-list/todo-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [TodoListComponent],
  template: `
    <main class="app-container">
      <app-todo-list />
    </main>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      align-items: flex-start;
      justify-content: center;
      padding: 48px 16px;
      background: #f1f5f9;
    }
  `]
})
export class AppComponent {}
