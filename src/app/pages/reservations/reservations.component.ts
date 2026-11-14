import { Component } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';

// Página de reservas (por ahora vacía: solo navbar y footer).
@Component({
  selector: 'app-reservations',
  imports: [NavbarComponent, FooterComponent],
  templateUrl: './reservations.component.html',
})
export class ReservationsComponent {}
