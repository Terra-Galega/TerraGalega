import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-login-header',
  imports: [],
  templateUrl: './login-header.component.html',
  styleUrl: './login-header.component.scss',
})
export class LoginHeaderComponent {
  @Input() activeTab: 'login' | 'signup' = 'login';

  @Output() tabChange = new EventEmitter<'login' | 'signup'>();

  selectTab(tab: 'login' | 'signup') {
    this.tabChange.emit(tab);
  }
}
