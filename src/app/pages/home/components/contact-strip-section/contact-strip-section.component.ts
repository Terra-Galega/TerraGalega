import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContactInfoComponent } from '../../../../components/contact-info/contact-info.component';

@Component({
  selector: 'app-contact-strip-section',
  imports: [ContactInfoComponent, RouterLink],
  templateUrl: './contact-strip-section.component.html',
  styleUrl: './contact-strip-section.component.scss',
})
export class ContactStripSectionComponent {}