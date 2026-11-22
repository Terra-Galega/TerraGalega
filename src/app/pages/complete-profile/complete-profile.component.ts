import { TranslatePipe } from '../../pipes/translate.pipe';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { ButtonComponent } from '../../components/button/button.component';
import { AuthService } from '../../services/auth.service';
import { TutorialAuthService } from '../../services/tutorial-auth.service';
import { ClientService } from '../../services/client.service';
import { AddressService } from '../../services/address.service';
import { getErrorMessage } from '../../utils/http-error';

// Máximos de la base de datos (los mismos que valida el back)
const NAME_MAX = 70;
const PHONE_MAX = 25;
const ADDRESS_MAX = 120;

// A dónde se vuelve según el parámetro "redirect" (sin él, a la cuenta)
const REDIRECTS: Record<string, string> = {
  checkout: '/checkout',
  reservations: '/reservations',
};

// Después de crear la cuenta con Google: pide lo que Google no da y el
// registro normal sí exige (apellido, teléfono y una dirección de entrega).
@Component({
  selector: 'app-complete-profile',
  imports: [FormsModule, NavbarComponent, FooterComponent, PageHeaderComponent, ButtonComponent, TranslatePipe],
  templateUrl: './complete-profile.component.html',
})
export class CompleteProfileComponent implements OnInit {
  private auth = inject(AuthService);
  private clientService = inject(ClientService);
  private addressService = inject(AddressService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private tutorialAuth = inject(TutorialAuthService);


  readonly nameMax = NAME_MAX;
  readonly phoneMax = PHONE_MAX;
  readonly addressMax = ADDRESS_MAX;

  name = '';
  lastName = '';
  phone = '';
  address = '';
  addressName = '';

  // false si la cuenta ya tenía alguna dirección (entonces no se pide)
  needsAddress = true;

  saving = false;
  error = '';

  ngOnInit(): void {
    // Lo que Google ya dio se precarga para que solo revise
    const session = this.auth.session;
    this.name = session?.name ?? '';
    this.lastName = session?.lastName ?? '';
    this.tutorialAuth.resumeProfile();

    // Si falla alguna carga se piden todos los campos; el back decide qué hace falta
    this.clientService.getMe().subscribe({
      next: (client) => (this.phone = client.phone ?? ''),
      error: () => {},
    });
    this.addressService.getAddresses().subscribe({
      next: (addresses) => (this.needsAddress = addresses.length === 0),
      error: () => {},
    });
  }

  save(): void {
    if (this.saving) return;
    this.error = '';

    const name = this.name.trim();
    const lastName = this.lastName.trim();
    const phone = this.phone.trim();
    const address = this.address.trim();

    if (!name || !lastName || !phone || (this.needsAddress && !address)) {
      this.error = 'Completa todos los campos para continuar.';
      return;
    }
    if (!/^\+?[\d\s-]{7,}$/.test(phone)) {
      this.error = 'Escribe un número de teléfono válido.';
      return;
    }

    this.saving = true;
    this.clientService
      .completeProfile({
        name,
        lastName,
        phone,
        address: this.needsAddress
          ? { label: address, name: this.addressName.trim() || undefined }
          : undefined,
      })
      .subscribe({
        next: (user) => {
          this.auth.setSession(user);
          const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '';
          this.router.navigate([REDIRECTS[redirect] ?? '/account'], {
            replaceUrl: true,
          });
        },
        error: (error) => {
          this.saving = false;
          this.error = getErrorMessage(error, 'No pudimos guardar tus datos. Inténtalo de nuevo.');
        },
      });
  }

  // Salir sin completar: se cierra la sesión (la cuenta queda creada)
  logout(): void {
    this.auth.logout();
    this.router.navigate(['/home'], { replaceUrl: true });
  }
}