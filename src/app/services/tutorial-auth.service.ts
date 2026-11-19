import { Injectable, inject } from '@angular/core';
import { TourName, TourStep, TutorialService } from './Tutorial.service';
import { isValidEmail } from '../utils/email';

// Mismo formato de teléfono que valida la página de completar perfil
const PHONE_REGEX = /^\+?[\d\s-]{7,}$/;

// Textos que cambian según el tutorial que trajo al cliente al login
interface FlowTexts {
  // Para qué necesita la cuenta: "confirmar tu pedido" / "reservar tu mesa"
  goal: string;
  // A dónde vuelve después: "seguir con tu pedido" / "seguir con tu reserva"
  back: string;
}

const TEXTS: Record<TourName, FlowTexts> = {
  order: { goal: 'confirmar tu pedido', back: 'seguir con tu pedido' },
  reservation: { goal: 'reservar tu mesa', back: 'seguir con tu reserva' },
};

// Servicio auxiliar: pasos de entrada a la cuenta. Lo comparten los tutoriales
// "hacer un pedido" y "hacer una reserva".
// No corre solo: las páginas lo llaman al abrirse, y solo arranca si el cliente viene
// de un tutorial (quedó un flujo armado). Un cliente que ya tiene sesión nunca
// pasa por /login, así que nunca ve estos pasos.
//
//   /login            -> resumeLogin():   elegir iniciar sesión o registrarse
//   /complete-profile -> resumeProfile(): completar datos tras entrar con Google
@Injectable({ providedIn: 'root' })
export class TutorialAuthService {
  private tutorial = inject(TutorialService);

  // La llama LoginComponent al abrirse. Si no hay tutorial pendiente, no hace nada.
  resumeLogin(): void {
    const flow = this.tutorial.currentFlow();
    if (!flow) return;
    void this.tutorial.run(this.chooseSteps(TEXTS[flow]));
  }

  // La llama CompleteProfileComponent al abrirse (cuenta creada con Google).
  resumeProfile(): void {
    const flow = this.tutorial.currentFlow();
    if (!flow) return;
    void this.tutorial.run(this.profileSteps(TEXTS[flow]));
  }

  // ---- Elegir cómo entrar ----

  // Un solo paso que resalta SOLO las pestañas "Iniciar sesión" y "Registrarse".
  // Lo demás de la tarjeta (formulario, botón de Google) queda bajo la zona oscura
  // y no se puede tocar: así el cliente no se salta el flujo.
  // Según la pestaña que toque, arranca el tramo que corresponde.
  private chooseSteps(t: FlowTexts): TourStep[] {
    return [
      {
        element: '#auth-tabs',
        title: '¿Cómo quieres continuar?',
        description: `Para ${t.goal} necesitas una cuenta. Toca "Iniciar sesión" si ya tienes una, o "Registrarse" para crear una.`,
        side: 'right',
        align: 'center',
        waitClick: '#auth-tabs button',
        afterClick: (target) => {
          const branch = target.closest('#tab-signup-btn')
            ? this.signupSteps(t)
            : this.loginSteps(t);
          // Se arranca después de que el motor cierre este tramo
          setTimeout(() => void this.tutorial.run(branch));
        },
      },
    ];
  }

  // ---- Iniciar sesión ----

  // El último paso espera a que el cliente se vaya de /login: así, si se equivoca de
  // contraseña, el globo sigue ahí. El flujo queda armado para la página siguiente.
  private loginSteps(t: FlowTexts): TourStep[] {
    return [
      // Correo: avanza al escribir uno válido y salir del campo,
      // o al tocar la cuenta de cliente de prueba (el primer botón de prueba)
      {
        element: '#login-email',
        title: 'Tu correo',
        description:
          'Escríbelo y toca fuera del campo (o pulsa Enter). Si solo estás probando, usa la cuenta de cliente de prueba que está más abajo.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#login-email', valid: (v) => isValidEmail(v) },
        waitClick: '.demo-login-btn:first-child',
      },
      // Contraseña: "Siguiente" por si ya venía llena con la cuenta de prueba
      {
        element: '#login-password',
        title: 'Tu contraseña',
        description:
          'Escríbela y toca fuera del campo. Con el ojito puedes verla. Si ya se llenó sola con la cuenta de prueba, pulsa "Siguiente".',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#login-password', valid: (v) => v.length > 0 },
        canSkip: true,
      },
      // Entrar
      {
        element: '#btn-login',
        title: 'Entra a tu cuenta',
        description: `Pulsa "Iniciar sesión". Te llevaremos de vuelta para ${t.back}.`,
        side: 'right',
        align: 'center',
        waitNavigation: true,
      },
    ];
  }

  // ---- Registrarse ----

  // Un paso por campo; cada uno avanza cuando el cliente lo escribe y sale del campo.
  private signupSteps(t: FlowTexts): TourStep[] {
    return [
      {
        element: '#signup-form',
        title: 'Crea tu cuenta',
        description: 'Son pocos datos. Te acompañamos campo por campo.',
        side: 'right',
        align: 'center',
      },
      {
        element: '#signup-name',
        title: 'Tu nombre',
        description: 'Escríbelo y toca fuera del campo para continuar.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#signup-name', valid: (v) => v.trim().length > 0 },
      },
      {
        element: '#signup-lastname',
        title: 'Tu apellido',
        description: 'Ahora tu apellido.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#signup-lastname', valid: (v) => v.trim().length > 0 },
      },
      {
        element: '#signup-email',
        title: 'Tu correo',
        description:
          'Usa un correo al que tengas acceso: te enviaremos un enlace para activar la cuenta.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#signup-email', valid: (v) => isValidEmail(v) },
      },
      {
        element: '#signup-password',
        title: 'Tu contraseña',
        description: 'Elige una que recuerdes. Con el ojito puedes verla mientras la escribes.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#signup-password', valid: (v) => v.trim().length > 0 },
      },
      {
        element: '#signup-phone',
        title: 'Tu teléfono',
        description: 'Lo usamos para contactarte si hay algún cambio con tu pedido o tu reserva.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#signup-phone', valid: (v) => PHONE_REGEX.test(v.trim()) },
      },
      {
        element: '#signup-address',
        title: 'Tu dirección',
        description: 'Será tu primera dirección de entrega. Luego podrás agregar más.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#signup-address', valid: (v) => v.trim().length > 0 },
      },
      // Crear cuenta: avanza cuando aparece la pantalla "Revisa tu correo".
      // Si falta algo, el formulario avisa y el globo se queda esperando.
      {
        element: '#signup-form app-button',
        title: 'Crea tu cuenta',
        description: 'Pulsa "Crear cuenta". Si algo falta, el formulario te lo dirá.',
        side: 'right',
        align: 'center',
        waitElement: '#signup-pending',
      },
      // Cierre de este tramo
      {
        element: '#signup-pending',
        title: '¡Revisa tu correo!',
        description: `Te enviamos un enlace para activar tu cuenta. Ábrelo y, cuando vuelvas, inicia sesión para ${t.back}.`,
        side: 'right',
        align: 'center',
      },
    ];
  }

  // ---- Completar perfil (cuenta creada con Google) ----

  private profileSteps(t: FlowTexts): TourStep[] {
    return [
      {
        element: '#complete-profile-form',
        title: 'Completa tu perfil',
        description:
          'Revisa tu nombre y completa los datos que faltan.',
        side: 'right',
        align: 'center',
      },
      {
        element: '#profile-phone',
        title: 'Tu teléfono',
        description: 'Escríbelo y toca fuera del campo para continuar.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#profile-phone', valid: (v) => PHONE_REGEX.test(v.trim()) },
      },
      // Solo se pide si la cuenta no tenía ninguna dirección
      {
        element: '#profile-address',
        optional: true,
        title: 'Tu dirección de entrega',
        description: 'Escríbela para que sepamos dónde llevar tus pedidos.',
        side: 'right',
        align: 'center',
        waitInput: { selector: '#profile-address', valid: (v) => v.trim().length > 0 },
      },
      {
        element: '#btn-complete-profile',
        title: 'Guarda y continúa',
        description: `Pulsa "Guardar y continuar" para ${t.back}.`,
        side: 'right',
        align: 'center',
        waitNavigation: true,
      },
    ];
  }
}
