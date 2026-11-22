import { Component, ElementRef, HostListener, Input, inject, viewChild } from '@angular/core';
import { ThemeService } from '../../services/theme.service';
import { LanguageService } from '../../services/language.service';
import { Palette, PaletteService } from '../../services/palette.service';
import { chartColorsFor } from '../../utils/dashboard-metrics';

interface PaletteOption {
    id: Palette;
    // Claves de traducción del nombre y de la descripción
    name: string;
    description: string;
}

// Contador para que cada instancia tenga ids únicos (aria-controls, aria-labelledby)
let nextId = 0;

// Botón del ojo + panel de accesibilidad visual: daltonismo (radios) y alto contraste (switch).
// - variant 'menu': botón redondo crema, dentro de la columna de la tuerca del navbar.
//   El panel se abre a la izquierda del ojo (debajo en móvil).
// - variant 'bar': botón transparente para barras superiores (admin). El panel se abre debajo.
@Component({
    selector: 'app-palette-picker',
    templateUrl: './palette-picker.component.html',
    styleUrl: './palette-picker.component.scss',
    host: {
        '[class.palette-picker--bar]': "variant === 'bar'",
    },
})
export class PalettePickerComponent {
    @Input() variant: 'menu' | 'bar' = 'menu';

    readonly theme = inject(ThemeService);
    readonly language = inject(LanguageService);
    readonly palette = inject(PaletteService);
    private readonly element = inject(ElementRef<HTMLElement>);
    private readonly trigger = viewChild<ElementRef<HTMLButtonElement>>('trigger');

    private readonly uid = nextId++;
    readonly panelId = `a11y-panel-${this.uid}`;
    readonly groupLabelId = `a11y-group-${this.uid}`;
    open = false;

    readonly options: PaletteOption[] = [
        { id: 'standard', name: 'paletteStandard', description: 'paletteStandardDesc' },
        { id: 'red-green', name: 'paletteRedGreen', description: 'paletteRedGreenDesc' },
        { id: 'blue-yellow', name: 'paletteBlueYellow', description: 'paletteBlueYellowDesc' },
    ];

    toggle(): void {
        this.open = !this.open;
        if (this.open) {
            // Al abrir, el foco pasa a la opción marcada (como en un grupo de radios)
            setTimeout(() => this.focusOption(this.checkedIndex()));
        }
    }

    // La tuerca lo llama al cerrarse, para que el panel no quede abierto escondido
    close(): void {
        this.open = false;
    }

    choose(id: Palette): void {
        this.palette.set(id);
    }

    // Los cuatro primeros colores de las gráficas con esa paleta: vista previa de la opción
    swatches(id: Palette): string[] {
        return chartColorsFor(id, this.theme.isDark).slice(0, 4);
    }

    // Flechas, Inicio y Fin: se mueven entre opciones y las eligen (como un radiogroup)
    onOptionKeydown(event: KeyboardEvent, index: number): void {
        const last = this.options.length - 1;
        let next: number | null = null;
        if (event.key === 'ArrowDown' || event.key === 'ArrowRight') {
            next = index === last ? 0 : index + 1;
        } else if (event.key === 'ArrowUp' || event.key === 'ArrowLeft') {
            next = index === 0 ? last : index - 1;
        } else if (event.key === 'Home') {
            next = 0;
        } else if (event.key === 'End') {
            next = last;
        }
        if (next !== null) {
            event.preventDefault();
            this.choose(this.options[next].id);
            this.focusOption(next);
        }
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        if (!this.element.nativeElement.contains(event.target as Node)) {
            this.open = false;
        }
    }

    // Esc con el panel abierto: cierra solo el panel y devuelve el foco al ojo.
    // stopPropagation evita que el mismo Esc cierre también la tuerca.
    @HostListener('keydown.escape', ['$event'])
    onEscape(event: Event): void {
        if (!this.open) {
            return;
        }
        event.stopPropagation();
        this.open = false;
        this.trigger()?.nativeElement.focus();
    }

    private checkedIndex(): number {
        return Math.max(0, this.options.findIndex((o) => o.id === this.palette.palette()));
    }

    private focusOption(index: number): void {
        const buttons = this.element.nativeElement.querySelectorAll('.palette__option') as NodeListOf<HTMLButtonElement>;
        buttons[index]?.focus();
    }
}