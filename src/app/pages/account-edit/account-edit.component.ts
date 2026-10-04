import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Client } from '../../models/client';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ClientService } from '../../services/client.service';
import { ButtonComponent } from '../../components/button/button.component';

@Component({
  selector: 'app-account-edit',
  imports: [NavbarComponent, FooterComponent, ReactiveFormsModule, ButtonComponent],
  templateUrl: './account-edit.component.html',
  styleUrl: './account-edit.component.scss'
})
export class AccountEditComponent implements OnInit {

  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  client: Client | null = null;

  form = new FormGroup({
    name: new FormControl(''),
    lastName: new FormControl(''),
    email: new FormControl(''),
    phone: new FormControl(''),
    password: new FormControl('')
  });

  ngOnInit(): void {

    const id = Number(
      this.route.snapshot.paramMap.get('id')
    );

    this.client = this.clientService.getClientById(id) ?? null;

    if (!this.client) {
      this.router.navigate(['/account']);
      return;
    }

    this.form.patchValue({
      name: this.client.name,
      lastName: this.client.lastName,
      email: this.client.email,
      phone: this.client.phone
    });
  }

  save(): void {

    if (!this.client) {
      return;
    }

    const updatedClient: Client = {
      ...this.client,

      name: this.form.value.name ?? '',
      lastName: this.form.value.lastName ?? '',
      email: this.form.value.email ?? '',
      phone: this.form.value.phone ?? '',

      // Si deja contraseña vacía,
      // conserva la contraseña anterior.
      password:
        this.form.value.password || this.client.password
    };

    this.clientService.updateClient(updatedClient);

    this.router.navigate([
      '/account',
      this.client.id
    ]);
  }

  cancel(): void {

    if (!this.client) {
      return;
    }

    this.router.navigate([
      '/account',
      this.client.id
    ]);
  }
}