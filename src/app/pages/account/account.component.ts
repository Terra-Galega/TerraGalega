import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';
import { BtnActionComponent } from './components/btn-action/btn-action.component';

import { Client } from '../../models/client';
import { ClientService } from '../../services/client.service';

@Component({
  selector: 'app-account',
  imports: [
    NavbarComponent,
    FooterComponent,
    ButtonComponent,
    BtnActionComponent
  ],
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss'
})
export class AccountComponent {

  private clientService = inject(ClientService);
  private router = inject(Router);

  client: Client | null = null;

  selectedAction = '';

  onSelectedChange(action: string) {
    this.selectedAction = action;

    switch (action) {
    case 'Editar perfil':
      this.editProfile();
      break;

    case 'Cerrar sesion':
      this.logOut();
      break;
    
    case 'Eliminar cuenta':
      this.deleteAccount();
      break;
    }
  }

  editProfile(){
    
  }

  logOut(){

  }

  deleteAccount(){

  }

  
}