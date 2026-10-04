import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-btn-action',
  imports: [],
  templateUrl: './btn-action.component.html',
  styleUrl: './btn-action.component.scss'
})
export class BtnActionComponent {

  @Input() selected = '';
  @Input() action = '';

  @Output() selectedChange = new EventEmitter<string>();

}