import { Component, ElementRef, HostListener, inject, viewChild } from '@angular/core';
import { ThemeService } from '../../services/theme.service';
import { LanguageService } from '../../services/language.service';
import { PalettePickerComponent } from '../palette-picker/palette-picker.component';

@Component({
    selector: 'app-settings-menu',
    standalone: true,
    imports: [PalettePickerComponent],
    templateUrl: './settings-menu.component.html',
    styleUrl: './settings-menu.component.scss',
})
export class SettingsMenuComponent {
    readonly theme = inject(ThemeService);
    readonly language = inject(LanguageService);
    private readonly element = inject(ElementRef<HTMLElement>);
    private readonly picker = viewChild(PalettePickerComponent);

    open = false;

    toggle(): void {
        this.open = !this.open;
        if (!this.open) {
            this.picker()?.close();
        }
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        if (!this.element.nativeElement.contains(event.target as Node)) {
            this.open = false;
            this.picker()?.close();
        }
    }

    // Si el panel de paletas está abierto, su propio Esc lo cierra primero y no llega aquí
    @HostListener('document:keydown.escape')
    onEscape(): void {
        this.open = false;
    }
}