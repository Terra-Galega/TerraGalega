import { Component, ElementRef, HostListener, inject } from '@angular/core';
import { ThemeService } from '../../services/theme.service';
import { LanguageService } from '../../services/language.service';

@Component({
    selector: 'app-settings-menu',
    standalone: true,
    templateUrl: './settings-menu.component.html',
    styleUrl: './settings-menu.component.scss',
})
export class SettingsMenuComponent {
    readonly theme = inject(ThemeService);
    readonly language = inject(LanguageService);
    private readonly element = inject(ElementRef<HTMLElement>);

    open = false;

    toggle(): void {
        this.open = !this.open;
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        if (!this.element.nativeElement.contains(event.target as Node)) {
            this.open = false;
        }
    }

    @HostListener('document:keydown.escape')
    onEscape(): void {
        this.open = false;
    }
}